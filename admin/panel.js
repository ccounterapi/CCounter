(function () {
  const ADMIN_PASSWORD_SHA256 = "1efe63f6225e2bdfcd621d644e5bd06b12a6329f546085092c7a7c81990544ce";

  const state = {
    sha: "",
    devices: [],
    pending: [],
    encodedApiKey: "",
  };
  let unlocked = false;

  const BASE63_NO_Z_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxy0123456789+/";
  const BASE63_NO_Z_BASE = 63n;

  const lockScreen = document.getElementById("lockScreen");
  const unlockForm = document.getElementById("unlockForm");
  const unlockInput = document.getElementById("unlockInput");
  const unlockError = document.getElementById("unlockError");
  const adminApp = document.getElementById("adminApp");

  const tokenInput = document.getElementById("tokenInput");
  const ownerInput = document.getElementById("ownerInput");
  const repoInput = document.getElementById("repoInput");
  const branchInput = document.getElementById("branchInput");
  const pathInput = document.getElementById("pathInput");
  const apiKeyInput = document.getElementById("apiKeyInput");
  const statusText = document.getElementById("statusText");
  const pendingWrap = document.getElementById("pendingWrap");
  const devicesWrap = document.getElementById("devicesWrap");

  unlockForm.addEventListener("submit", handleUnlock);
  document.getElementById("loadBtn").addEventListener("click", loadAll);
  document.getElementById("saveBtn").addEventListener("click", saveAll);
  setLocked(true);

  function setLocked(locked) {
    unlocked = !locked;
    if (locked) {
      if (adminApp) adminApp.classList.add("hidden");
      if (lockScreen) lockScreen.classList.remove("hidden");
      if (unlockInput) unlockInput.value = "";
      if (unlockError) unlockError.textContent = "";
      if (tokenInput) tokenInput.value = "";
      if (apiKeyInput) apiKeyInput.value = "";
      if (statusText) statusText.textContent = "";
      state.sha = "";
      state.devices = [];
      state.pending = [];
      if (pendingWrap) pendingWrap.innerHTML = "";
      if (devicesWrap) devicesWrap.innerHTML = "";
      setTimeout(() => unlockInput && unlockInput.focus(), 0);
      return;
    }
    if (lockScreen) lockScreen.classList.add("hidden");
    if (adminApp) adminApp.classList.remove("hidden");
    setTimeout(() => tokenInput && tokenInput.focus(), 0);
  }

  async function sha256Hex(value) {
    const data = new TextEncoder().encode(value);
    const hashBuffer = await crypto.subtle.digest("SHA-256", data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map((b) => b.toString(16).padStart(2, "0")).join("");
  }

  async function handleUnlock(event) {
    event.preventDefault();
    const password = String(unlockInput ? unlockInput.value : "");
    if (!password) {
      if (unlockError) unlockError.textContent = "Enter password.";
      return;
    }
    if (!window.crypto || !window.crypto.subtle) {
      if (unlockError) unlockError.textContent = "This browser does not support secure unlock.";
      return;
    }
    try {
      const hash = await sha256Hex(password);
      if (hash !== ADMIN_PASSWORD_SHA256) {
        if (unlockError) unlockError.textContent = "Incorrect password.";
        if (unlockInput) {
          unlockInput.value = "";
          unlockInput.focus();
        }
        return;
      }
      setLocked(false);
    } catch (_error) {
      if (unlockError) unlockError.textContent = "Unlock failed. Try again.";
    }
  }

  function setStatus(text, isError) {
    statusText.textContent = text;
    statusText.style.color = isError ? "#ff5252" : "#9e9e9e";
  }

  function ghHeaders(includeContentType = true) {
    const token = tokenInput.value.trim();
    const headers = {
      "Accept": "application/vnd.github+json",
      "Authorization": `Bearer ${token}`,
    };
    if (includeContentType) {
      headers["Content-Type"] = "application/json";
    }
    return headers;
  }

  function getRepoParts() {
    return {
      owner: ownerInput.value.trim(),
      repo: repoInput.value.trim(),
      branch: branchInput.value.trim() || "main",
      path: pathInput.value.trim() || "admin/devices.json",
    };
  }

  function decodeBase64Utf8(base64) {
    const raw = atob(base64);
    const bytes = Uint8Array.from(raw, (c) => c.charCodeAt(0));
    return new TextDecoder().decode(bytes);
  }

  function encodeBase64Utf8(text) {
    const bytes = new TextEncoder().encode(text);
    let raw = "";
    for (let i = 0; i < bytes.length; i += 1) raw += String.fromCharCode(bytes[i]);
    return btoa(raw);
  }

  function encodeBase63NoZ(text) {
    const source = String(text || "");
    if (!source) return "";
    const bytes = new TextEncoder().encode(source);
    let value = 0n;
    for (let i = 0; i < bytes.length; i += 1) {
      value = (value << 8n) + BigInt(bytes[i]);
    }
    if (value === 0n) return BASE63_NO_Z_ALPHABET[0];
    let encoded = "";
    while (value > 0n) {
      const remainder = Number(value % BASE63_NO_Z_BASE);
      encoded = BASE63_NO_Z_ALPHABET[remainder] + encoded;
      value /= BASE63_NO_Z_BASE;
    }
    return encoded;
  }

  function decodeBase63NoZ(encoded) {
    const source = String(encoded || "").trim();
    if (!source) return "";
    let value = 0n;
    for (let i = 0; i < source.length; i += 1) {
      const idx = BASE63_NO_Z_ALPHABET.indexOf(source[i]);
      if (idx < 0) throw new Error("Encoded API key contains unsupported symbols.");
      value = value * BASE63_NO_Z_BASE + BigInt(idx);
    }
    if (value === 0n) return "";
    const bytes = [];
    while (value > 0n) {
      bytes.push(Number(value & 255n));
      value >>= 8n;
    }
    bytes.reverse();
    return new TextDecoder().decode(Uint8Array.from(bytes));
  }

  function parseIssueBody(body) {
    const text = String(body || "");
    const find = (regex) => {
      const m = text.match(regex);
      return m ? m[1].trim() : "";
    };
    return {
      deviceId: find(/Device ID:\s*(.+)/i),
      profileName: find(/Profile Name:\s*(.+)/i) || "User",
      language: find(/Language:\s*(.+)/i) || "English",
    };
  }

  function toDateTimeLocalValue(isoUtc) {
    if (!isoUtc) return "";
    const date = new Date(isoUtc);
    if (Number.isNaN(date.getTime())) return "";
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 16);
  }

  function toUtcIsoFromDateTimeLocal(localValue) {
    if (!localValue) return null;
    const date = new Date(localValue);
    if (Number.isNaN(date.getTime())) return null;
    return date.toISOString();
  }

  function nowPlus30DaysIso() {
    return new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();
  }

  function sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  async function fetchContentMetadata(owner, repo, branch, path) {
    const contentUrl = `https://api.github.com/repos/${owner}/${repo}/contents/${path}?ref=${encodeURIComponent(branch)}&_ts=${Date.now()}-${Math.random()}`;
    const contentResp = await fetch(contentUrl, { headers: ghHeaders(false) });
    if (!contentResp.ok) throw new Error(`Failed to load devices JSON (${contentResp.status}).`);
    return contentResp.json();
  }

  async function loadAll() {
    if (!unlocked) {
      setStatus("Panel is locked.", true);
      return;
    }
    try {
      setStatus("Loading...", false);
      const { owner, repo, branch, path } = getRepoParts();
      if (!tokenInput.value.trim()) throw new Error("Enter GitHub token first.");

      const contentJson = await fetchContentMetadata(owner, repo, branch, path);
      state.sha = contentJson.sha || "";
      const parsed = JSON.parse(decodeBase64Utf8(contentJson.content || ""));
      state.devices = Array.isArray(parsed.devices) ? parsed.devices : [];
      state.encodedApiKey = String(parsed.openAiApiKeyBase63NoZ || "").trim();
      if (apiKeyInput) {
        if (state.encodedApiKey) {
          try {
            apiKeyInput.value = decodeBase63NoZ(state.encodedApiKey);
          } catch (_error) {
            apiKeyInput.value = "";
            setStatus("Loaded devices, but encoded API key could not be decoded. Update it and save again.", true);
          }
        } else {
          apiKeyInput.value = "";
        }
      }

      const issuesUrl = `https://api.github.com/repos/${owner}/${repo}/issues?state=open&labels=device-registration&per_page=100`;
      const issuesResp = await fetch(issuesUrl, { headers: ghHeaders(false) });
      if (!issuesResp.ok) throw new Error(`Failed to load registration issues (${issuesResp.status}).`);
      const issues = await issuesResp.json();

      const knownDeviceIds = new Set(state.devices.map((d) => String(d.deviceId || "").toLowerCase()));
      state.pending = issues
        .map((issue) => {
          const parsedBody = parseIssueBody(issue.body);
          return {
            issueNumber: issue.number,
            issueTitle: issue.title,
            ...parsedBody,
          };
        })
        .filter((item) => item.deviceId && !knownDeviceIds.has(item.deviceId.toLowerCase()));

      renderPending();
      renderDevices();
      setStatus(`Loaded ${state.devices.length} devices and ${state.pending.length} pending requests.`, false);
    } catch (error) {
      const message = (error && error.message) ? String(error.message) : String(error);
      if (/failed to fetch/i.test(message)) {
        setStatus("Failed to fetch. Check internet/VPN/adblock and ensure token has repository access.", true);
      } else {
        setStatus(message, true);
      }
    }
  }

  function renderPending() {
    if (!state.pending.length) {
      pendingWrap.innerHTML = `<p class="muted">No pending registrations.</p>`;
      return;
    }
    const rows = state.pending.map((p, idx) => `
      <tr>
        <td>${escapeHtml(p.deviceId)}</td>
        <td>${escapeHtml(p.profileName)}</td>
        <td>${escapeHtml(p.language)}</td>
        <td>#${p.issueNumber}</td>
        <td>
          <div class="row-actions">
            <button class="btn btn-primary" data-approve="${idx}">Approve (30 days)</button>
            <button class="btn btn-danger" data-delete-pending="${idx}">Delete</button>
          </div>
        </td>
      </tr>
    `).join("");

    pendingWrap.innerHTML = `
      <table>
        <thead>
          <tr>
            <th>Device ID</th>
            <th>Profile</th>
            <th>Language</th>
            <th>Issue</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    `;

    Array.from(pendingWrap.querySelectorAll("[data-approve]")).forEach((btn) => {
      btn.addEventListener("click", () => {
        const index = Number(btn.getAttribute("data-approve"));
        approvePending(index);
      });
    });
    Array.from(pendingWrap.querySelectorAll("[data-delete-pending]")).forEach((btn) => {
      btn.addEventListener("click", async () => {
        const index = Number(btn.getAttribute("data-delete-pending"));
        await deletePending(index);
      });
    });
  }

  function renderDevices() {
    if (!state.devices.length) {
      devicesWrap.innerHTML = `<p class="muted">No devices yet.</p>`;
      return;
    }

    const rows = state.devices.map((d, idx) => {
      const status = !d.accessEnabled ? "Pending" : d.paused ? "Paused" : "Active";
      const statusClass = !d.accessEnabled ? "" : d.paused ? "red" : "green";
      return `
        <tr>
          <td><code>${escapeHtml(d.deviceId || "")}</code></td>
          <td>
            <input data-name="${idx}" type="text" value="${escapeAttr(d.profileName || "")}" />
          </td>
          <td>
            <span class="pill ${statusClass}">${status}</span>
          </td>
          <td>
            <label><input data-enabled="${idx}" type="checkbox" ${d.accessEnabled ? "checked" : ""} /> enabled</label><br/>
            <label><input data-paused="${idx}" type="checkbox" ${d.paused ? "checked" : ""} /> paused</label>
          </td>
          <td>
            <input data-expiry="${idx}" type="datetime-local" value="${escapeAttr(toDateTimeLocalValue(d.expiresAtUtc))}" />
          </td>
          <td>
            <div class="row-actions">
              <button class="btn btn-outline" data-extend="${idx}">+30d</button>
              <button class="btn btn-danger" data-remove="${idx}">Remove</button>
            </div>
          </td>
        </tr>
      `;
    }).join("");

    devicesWrap.innerHTML = `
      <table>
        <thead>
          <tr>
            <th>Device ID</th>
            <th>Profile</th>
            <th>Status</th>
            <th>Control</th>
            <th>Expires (local time)</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    `;

    bindDeviceInputs();
  }

  function bindDeviceInputs() {
    Array.from(devicesWrap.querySelectorAll("[data-enabled]")).forEach((el) => {
      el.addEventListener("change", () => {
        const index = Number(el.getAttribute("data-enabled"));
        state.devices[index].accessEnabled = !!el.checked;
      });
    });
    Array.from(devicesWrap.querySelectorAll("[data-paused]")).forEach((el) => {
      el.addEventListener("change", () => {
        const index = Number(el.getAttribute("data-paused"));
        state.devices[index].paused = !!el.checked;
      });
    });
    Array.from(devicesWrap.querySelectorAll("[data-expiry]")).forEach((el) => {
      el.addEventListener("change", () => {
        const index = Number(el.getAttribute("data-expiry"));
        state.devices[index].expiresAtUtc = toUtcIsoFromDateTimeLocal(el.value);
      });
    });
    Array.from(devicesWrap.querySelectorAll("[data-name]")).forEach((el) => {
      el.addEventListener("input", () => {
        const index = Number(el.getAttribute("data-name"));
        state.devices[index].profileName = el.value;
      });
    });
    Array.from(devicesWrap.querySelectorAll("[data-remove]")).forEach((btn) => {
      btn.addEventListener("click", () => {
        const index = Number(btn.getAttribute("data-remove"));
        state.devices.splice(index, 1);
        renderDevices();
      });
    });
    Array.from(devicesWrap.querySelectorAll("[data-extend]")).forEach((btn) => {
      btn.addEventListener("click", () => {
        const index = Number(btn.getAttribute("data-extend"));
        const current = state.devices[index].expiresAtUtc ? new Date(state.devices[index].expiresAtUtc) : new Date();
        current.setDate(current.getDate() + 30);
        state.devices[index].expiresAtUtc = current.toISOString();
        renderDevices();
      });
    });
  }

  function approvePending(index) {
    const item = state.pending[index];
    if (!item) return;
    const nowIso = new Date().toISOString();
    state.devices.push({
      deviceId: item.deviceId,
      profileName: item.profileName || "User",
      language: item.language || "English",
      accessEnabled: true,
      paused: false,
      createdAtUtc: nowIso,
      updatedAtUtc: nowIso,
      expiresAtUtc: nowPlus30DaysIso(),
    });
    state.pending.splice(index, 1);
    renderPending();
    renderDevices();
  }

  async function closeRegistrationIssue(issueNumber) {
    const { owner, repo } = getRepoParts();
    const url = `https://api.github.com/repos/${owner}/${repo}/issues/${issueNumber}`;
    const response = await fetch(url, {
      method: "PATCH",
      headers: ghHeaders(true),
      body: JSON.stringify({ state: "closed" }),
    });
    if (!response.ok) {
      const text = await response.text();
      throw new Error(`Failed to close issue #${issueNumber} (${response.status}): ${text}`);
    }
  }

  async function deletePending(index) {
    const item = state.pending[index];
    if (!item) return;
    const label = item.profileName || item.deviceId || `#${item.issueNumber}`;
    const confirmed = window.confirm(`Delete pending registration for ${label}?`);
    if (!confirmed) return;
    try {
      setStatus(`Deleting pending request #${item.issueNumber}...`, false);
      await closeRegistrationIssue(item.issueNumber);
      state.pending.splice(index, 1);
      renderPending();
      setStatus(`Pending request #${item.issueNumber} deleted.`, false);
    } catch (error) {
      const message = (error && error.message) ? String(error.message) : String(error);
      if (/failed to fetch/i.test(message)) {
        setStatus("Failed to fetch. Check internet/VPN/adblock and ensure token has repository access.", true);
      } else {
        setStatus(message, true);
      }
    }
  }

  async function saveAll() {
    if (!unlocked) {
      setStatus("Panel is locked.", true);
      return;
    }
    try {
      setStatus("Saving...", false);
      const token = tokenInput.value.trim();
      if (!token) throw new Error("Enter GitHub token first.");

      const { owner, repo, branch, path } = getRepoParts();
      const payloadObject = {
        updatedAtUtc: new Date().toISOString(),
        openAiApiKeyBase63NoZ: encodeBase63NoZ(apiKeyInput ? apiKeyInput.value.trim() : ""),
        devices: state.devices.map((item) => ({
          deviceId: String(item.deviceId || "").trim(),
          profileName: String(item.profileName || "User").trim(),
          language: String(item.language || "English").trim(),
          accessEnabled: !!item.accessEnabled,
          paused: !!item.paused,
          expiresAtUtc: item.expiresAtUtc || null,
          createdAtUtc: item.createdAtUtc || new Date().toISOString(),
          updatedAtUtc: new Date().toISOString(),
        })).filter((item) => item.deviceId),
      };
      const encoded = encodeBase64Utf8(JSON.stringify(payloadObject, null, 2) + "\n");

      const body = {
        message: `admin: update device access (${new Date().toISOString()})`,
        content: encoded,
        sha: "",
        branch,
      };

      const saveRequest = async () => {
        const url = `https://api.github.com/repos/${owner}/${repo}/contents/${path}`;
        const resp = await fetch(url, {
          method: "PUT",
          headers: ghHeaders(true),
          body: JSON.stringify(body),
        });
        if (resp.ok) return;
        if (!resp.ok) {
          const text = await resp.text();
          const error = new Error(`Save failed (${resp.status}): ${text}`);
          error.statusCode = resp.status;
          error.responseText = text;
          throw error;
        }
      };

      const maxAttempts = 5;
      let lastError = null;
      for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
        try {
          const latest = await fetchContentMetadata(owner, repo, branch, path);
          state.sha = latest.sha || "";
          if (!state.sha) {
            throw new Error("Unable to get latest file SHA.");
          }
          body.sha = state.sha;
          await saveRequest();
          if (attempt > 1) {
            setStatus("Saved successfully. Conflict resolved automatically.", false);
          } else {
            setStatus("Saved successfully.", false);
          }
          await loadAll();
          return;
        } catch (error) {
          lastError = error;
          const isConflict = error && error.statusCode === 409;
          if (!isConflict) {
            throw error;
          }

          // GitHub returns expected SHA inside 409 message:
          // "... does not match <sha>"
          const conflictShaMatch = String(error.responseText || "")
            .match(/does not match\s+([0-9a-f]{40})/i);
          const conflictSha = conflictShaMatch ? conflictShaMatch[1] : "";
          if (conflictSha) {
            body.sha = conflictSha;
            try {
              await saveRequest();
              setStatus("Saved successfully. Conflict resolved automatically.", false);
              await loadAll();
              return;
            } catch (retryError) {
              lastError = retryError;
            }
          }

          if (attempt < maxAttempts) {
            await sleep(120 * attempt);
            continue;
          }
        }
      }
      throw lastError || new Error("Save failed due to repeated SHA conflicts. Please try again.");
    } catch (error) {
      const message = (error && error.message) ? String(error.message) : String(error);
      if (/failed to fetch/i.test(message)) {
        setStatus("Failed to fetch. Check internet/VPN/adblock and ensure token has repository access.", true);
      } else {
        setStatus(message, true);
      }
    }
  }

  function escapeHtml(text) {
    return String(text)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll("\"", "&quot;")
      .replaceAll("'", "&#39;");
  }

  function escapeAttr(text) {
    return escapeHtml(text).replaceAll("`", "&#96;");
  }
})();
