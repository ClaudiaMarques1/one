# Security Review

Read on demand. Use Claude Code's built-in `/security-review` for the diff-level pass; this file
adds the project-specific checks it can't know. Decision (#27): a project rules file on top of the
built-in command, not a full project skill - the surface below is small enough not to need one.

## When it applies

Run `/security-review` and this checklist when a change touches any of: `firestore.rules`, `auth/`,
a repository's collection path, `AndroidManifest.xml`, `.github/workflows/`, or logging.

## Checklist

| Area | Check | Current baseline |
|---|---|---|
| Firestore rules | Every path stays under `users/{uid}/...` with `request.auth.uid == uid`; no wildcard widening, no `allow ... if true` | `firestore.rules:9-11` - owner-only, deny by default |
| New collections | Repository writes under `users/{uid}/<collection>`, so the existing rule covers it; a top-level collection needs its own rule | uid lookup in each repository at line 14 |
| Auth | Errors surface via `_authError` without leaking tokens/credentials; sign-out clears both Credential Manager and Firebase | `auth/AuthViewModel.kt` `signOut()` |
| Manifest | New `<activity>`/`<receiver>`/`<service>` is `exported="false"` unless it needs an intent filter; new permissions justified | Only `MainActivity` exported (launcher); `INTERNET`, `POST_NOTIFICATIONS` |
| Logging | No user data (diary text, emails, uids, tokens) in `Log.*` or `println` | No `Log.*` calls in `app/src/main` |
| Secrets/config | `app/google-services.json` never read, echoed, or committed (gitignored); no keys in source or workflows | CI uses a placeholder, no repo secrets |

## Known, not a fresh finding

`android:allowBackup="true"` (`AndroidManifest.xml:9`) - Firestore data is server-side, but local
cache/prefs are included in device backups. Raise it only if a change adds sensitive local storage.
