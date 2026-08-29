# Life Group Texts

Send one message to your church life group from your own number, straight from a Light
Phone III. No accounts, no third-party service, nothing leaves the phone.

A native Kotlin/Compose rewrite of the earlier React + Capacitor app, styled to match
LightOS. Not built with the Light SDK — `SEND_SMS` and `READ_CONTACTS` are not on the
SDK's permission allowlist — so this is a plain Android app that follows Light's design
language, the same approach as the Channels app.

## Screens

| Tab | What it does |
|-----|--------------|
| **Send** | Pick groups and individuals in any combination, review the message, send |
| **People** | Add contacts by hand or import from the phone; create groups and set membership |
| **Message** | Write one message of any length, with an honest segment counter |
| **Log** | Every send with its real outcome, including why a failure failed |

## Messages of any length

The previous version could only send 160 characters, and messages with an emoji or a
curly apostrophe failed silently. Both had the same two causes:

1. **Single-segment sending.** It called `SmsManager.sendTextMessage`, which carries one
   segment. Anything longer was rejected by the radio. This version uses
   `divideMessage` + `sendMultipartTextMessage`, so a long message is split by the phone
   and reassembled on the recipient's end as one message.

2. **Silent encoding promotion.** SMS uses the GSM 03.38 alphabet, where a segment holds
   160 characters. A single character outside that alphabet — an emoji, a smart quote
   pasted from a phone keyboard — switches the whole message to UCS-2, where a segment
   holds only 70. Combined with single-segment sending, a short message could fail for
   no visible reason.

`SmsText` handles the encoding rules and reports what a message actually costs. The
Message screen shows the segment count as you type, names the characters forcing UCS-2,
and offers to rewrite the punctuation or drop them. See `SmsTextTest` for the rules.

Failures are also visible now: every segment reports back through a `PendingIntent`, so
the Log shows what the radio actually said rather than assuming success.

**Carriers bill per segment.** A 400-character message to 20 people is 60 texts, not 20.
The Send screen states the total before you commit.

## Build

Requires Android Studio (JDK 17–21) and the Android SDK. `local.properties` is
machine-specific and git-ignored; point `sdk.dir` at your SDK.

```bash
./gradlew assembleDebug
```

Install it:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Run the tests:

```bash
./gradlew testDebugUnitTest
```

## Design

Palette and typography ported from the MIT-licensed Light SDK (`:sdk:ui`): background
`#000000`, content `#FFFFFF`, secondary `#BBBBBB`. Akkurat is picked up from the LP3's
system fonts when present and falls back to the platform default elsewhere — it is a
commercial typeface and is deliberately not bundled.

## Permissions

| Permission | Reason |
|------------|--------|
| `SEND_SMS` | Send texts from your own number |
| `READ_CONTACTS` | Import people from the phone's contact list |

## Privacy

Contacts, groups, the draft message and the send log live in a local Room database on
the device. There is no network code in this app.
