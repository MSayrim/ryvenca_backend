# Server secrets (not committed)

Put the Firebase **service account** file here:

```
config/firebase-service-account.json
```

Firebase console → ⚙ Project settings → **Service accounts** → *Generate new private key*.

The server uses it to verify Apple / Google / e-mail sign-ins and to delete Firebase users when an account
is deleted. Another location can be set with `RYVENCA_FIREBASE_CREDENTIALS=/path/to/file.json`.
Without the file the server still runs, with local e-mail/password accounts only.

Everything in this folder except this README is git-ignored.
