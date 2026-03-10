# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| latest  | ✅        |

## Reporting a Vulnerability

If you find a security vulnerability in Audita, please **do not** open a public issue.

Instead, report it privately by emailing the maintainer or opening a [GitHub Security Advisory](https://github.com/YOUR_USERNAME/audita/security/advisories/new).

Please include:
- A description of the vulnerability
- Steps to reproduce
- Any suggested fix if you have one

You can expect a response within 72 hours. All valid reports will be acknowledged and addressed as a priority.

## Scope

- GitHub token exposure or mishandling
- Server-side request forgery via the `repoUrl` parameter
- Any issue that could expose user data or allow unauthorised access
