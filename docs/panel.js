(function () {
  const state = {
    sha: "",
    devices: [],
    pending: [],
  };

  const tokenInput = document.getElementById("tokenInput");
  const ownerInput = document.getElementById("ownerInput");
  const repoInput = document.getElementById("repoInput");
  const branchInput = document.getElementById("branchInput");
  const pathInput = document.getElementById("pathInput");
  const statusText = document.getElementById("statusText");
  const pendingWrap = document.getElementById("pendingWrap");
  const devicesWrap = document.getElementById("devicesWrap");

  document.getElementById("loadBtn").addEventListener("click", loadAll);
  document.getElementById("saveBtn").addEventListener("click", saveAll);

  function setStatus(text, isError) {
    statusText.textContent = text;
    statusText.style.color = isError ? "#ff5252" : "#9e9e9e";
  }

  function ghHeaders() {
    const token = tokenInput.value.trim();
    return {
      "Accept": "application/vnd.github+json",
      "Authorization": `Bearer ${token}`,
      "Content-Type": "application/json",
    };
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

  async function loadAll() {
    try {
      setStatus("Loading...", false);
      const { owner, repo, branch, path } = getRepoParts();
      if (!tokenInput.value.trim()) throw new Error("Enter GitHub token first.");

      const contentUrl = `https://api.github.com/repos/${owner}/${repo}/contents/${path}?ref=${encodeURIComponent(branch)}`;
      const contentResp = await fetch(contentUrl, { headers: ghHeaders() });
      if (!contentResp.ok) throw new Error(`Failed to load devices JSON (${contentResp.status}).`);
      const contentJson = await contentResp.json();
      state.sha = contentJson.sha || "";
      const parsed = JSON.parse(decodeBase64Utf8(contentJson.content || ""));
      state.devices = Array.isArray(parsed.devices) ? parsed.devices : [];

      const issuesUrl = `https://api.github.com/repos/${owner}/${repo}/issues?state=open&labels=device-registration&per_page=100`;
      const issuesResp = await fetch(issuesUrl, { headers: ghHeaders() });
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
      setStatus(error.message || String(error), true);
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

  async function saveAll() {
    try {
      setStatus("Saving...", false);
      const token = tokenInput.value.trim();
      if (!token) throw new Error("Enter GitHub token first.");
      if (!state.sha) throw new Error("Load data before saving.");

      const { owner, repo, branch, path } = getRepoParts();
      const payloadObject = {
        updatedAtUtc: new Date().toISOString(),
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
        sha: state.sha,
        branch,
      };

      const url = `https://api.github.com/repos/${owner}/${repo}/contents/${path}`;
      const resp = await fetch(url, {
        method: "PUT",
        headers: ghHeaders(),
        body: JSON.stringify(body),
      });
      if (!resp.ok) {
        const text = await resp.text();
        throw new Error(`Save failed (${resp.status}): ${text}`);
      }
      setStatus("Saved successfully.", false);
      await loadAll();
    } catch (error) {
      setStatus(error.message || String(error), true);
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

