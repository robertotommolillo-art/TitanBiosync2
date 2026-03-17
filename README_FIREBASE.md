# Firebase Setup Instructions

TitanBiosync2 uses **Firebase Authentication** for user sign-in/sign-out.

## Prerequisites

1. A Firebase project at [https://console.firebase.google.com](https://console.firebase.google.com)
2. Email/Password sign-in provider enabled in Firebase Console → Authentication → Sign-in method

## Steps

1. **Create (or open) a Firebase project** at the Firebase Console.

2. **Register your Android app**:
   - Package name: `com.titanbiosync`
   - Download the `google-services.json` file.

3. **Place `google-services.json`** in the `app/` directory:
   ```
   TitanBiosync2/
   └── app/
       └── google-services.json   ← place here
   ```
   > ⚠️ **Do NOT commit `google-services.json` to version control.**  
   > It is listed in `.gitignore`. The file in this repo is a placeholder only.

4. **Enable Email/Password authentication** in the Firebase Console:
   - Go to Authentication → Sign-in method → Email/Password → Enable

5. Build and run the app.

## Security

- `google-services.json` contains your Firebase project configuration.
- **Never commit this file** to a public repository.
- For CI/CD, inject the file contents via a secret environment variable or a secure file artifact.
