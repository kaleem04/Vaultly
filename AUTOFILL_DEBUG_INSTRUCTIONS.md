# Autofill Data Injection Debug Instructions

## What Was Changed

Added comprehensive logging with the `AUTOFILL_CLICK` and `AUTOFILL_INJECT` tags to track:
1. When a credential item is clicked
2. When the callback is triggered
3. When the Dataset is being built
4. When the result is being sent back

## How to Test

1. Build and install the app
2. Open another app with login fields (e.g., your test app)
3. Tap on the username/email field
4. Select "🔐 Vaultly passwords" from autofill suggestions
5. Complete biometric authentication
6. Tap on a credential from the list
7. Check logcat for these specific tags:

```bash
adb logcat -s AUTOFILL_CLICK:* AUTOFILL_INJECT:* AUTOFILL_UI:* VaultlyAutofill:*
```

## Expected Log Sequence

If everything works correctly, you should see:

```
AUTOFILL_UI: >>> SHOWING DIALOG with X credentials
AUTOFILL_UI: >>> fieldMap has X fields: username, password
AUTOFILL_UI: >>> Rendering LazyColumn with X items
AUTOFILL_UI: >>> Creating list item for: <username>
AUTOFILL_CLICK: >>> SURFACE CLICKED FOR: <username>
AUTOFILL_CLICK: >>> Calling onClick lambda...
AUTOFILL_CLICK: >>> LAZY COLUMN ITEM onClick for: <username>
AUTOFILL_CLICK: >>> CREDENTIAL SELECTED CALLBACK TRIGGERED
AUTOFILL_CLICK: >>> Selected: <username>
AUTOFILL_CLICK: >>> About to call fillCredentialsAndFinish
AUTOFILL_INJECT: === FILLING CREDENTIALS START ===
AUTOFILL_INJECT: Username: <username>
AUTOFILL_INJECT: Fields in map: username, password
AUTOFILL_INJECT: >>> Adding USERNAME field to dataset
AUTOFILL_INJECT: >>> Adding PASSWORD field to dataset
AUTOFILL_INJECT: >>> Total fields set in dataset: 2
AUTOFILL_INJECT: >>> Building dataset...
AUTOFILL_INJECT: >>> Dataset built successfully
AUTOFILL_INJECT: === SENDING AUTOFILL RESPONSE WITH 2 FIELDS ===
```

## If Logs Are Missing

### If you don't see `AUTOFILL_CLICK` logs:
- The click handler is not being triggered
- Check if the Surface onClick is working
- Try tapping harder or longer

### If you see `AUTOFILL_CLICK` but no `AUTOFILL_INJECT` logs:
- The fillCredentialsAndFinish method is not being called
- There might be a crash or exception

### If you see `AUTOFILL_INJECT` logs but data still doesn't inject:
- The Dataset is being built and sent correctly
- The problem is with the autofill framework accepting the result
- Check if the AutofillId values match what was passed from the service

## Next Steps Based on Logs

Please run the test and **paste the complete logcat output** filtered by the tags above. This will help identify exactly where the process is failing.
