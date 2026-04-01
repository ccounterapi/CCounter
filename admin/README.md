# CCounter Admin Panel

## What it does
- Shows pending device registrations from GitHub issues (`device-registration` label).
- Lets admin approve a device for 30 days.
- Lets admin pause/resume access.
- Lets admin edit expiration date and time.
- Saves access list to `admin/devices.json` in this repository.

## Hosting on GitHub Pages
1. Push repository to GitHub.
2. In repository settings, enable Pages from branch `main` (root).
3. Open:
   - `https://<owner>.github.io/<repo>/admin/`

## How app registration appears here
The Android app submits a GitHub issue with label `device-registration`.

To enable this, set in `local.properties` (not committed):

```properties
ccounter.githubRegistrationToken=YOUR_GITHUB_FINE_GRAINED_TOKEN
```

Token permissions needed on the repo:
- Issues: Read and Write
- Contents: Read

Then rebuild APK.

