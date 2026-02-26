# Security Policy

## Supported versions

Only the latest release receives security fixes.

| Version | Supported |
|---|---|
| 6.5.x (latest) | ✅ |
| < 6.5 | ❌ |

## Reporting a vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Report security issues privately via GitHub's built-in security advisory:  
**Security → Report a vulnerability** in the repository page.

Include:

- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix if you have one

You will receive a response within 7 days. If the vulnerability is confirmed, a patched release will be published and you will be credited in the changelog unless you prefer to remain anonymous.

## Scope

This app processes local JSON files and communicates with:

- `orbispatches.com` — patch metadata scraping
- `api.github.com` — release update check (read-only, no credentials)
- User-configured FTP server (PS4 local network)
- User-configured PKG download URLs

No credentials, tokens or personal data are stored or transmitted to any external service controlled by this project.
