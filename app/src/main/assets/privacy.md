# Privacy policy

**Ganjoor for Android** — an unofficial, independent reader for [ganjoor.net](https://ganjoor.net),
built and maintained by Muhammad Anas Rashid. Last updated 12 October 2026.

## The short version

The app has no account, no analytics, no advertising and no crash reporting. Nothing you do in
it is recorded or sent anywhere for the author's benefit — there is no server belonging to this
project for anything to be sent to.

What follows is the long version, because "we respect your privacy" is not a privacy policy.

## What is kept on your phone, and only on your phone

Your bookmarks and saved passages, your pinned poets, your reading settings (theme, font,
weight, size, language, offline mode), and any poets you download for offline reading. These
live in the app's own storage, go nowhere, and are deleted with the app.

If you set up the optional AI assistant, its API key is stored on the device **unencrypted**.
The app says so where you type it in. Anyone with access to your unlocked phone and the
patience to read app storage could read that key.

## What leaves your phone

The app has one Android permission: `INTERNET`. Every request below is made directly by your
phone to the service named — none of it passes through any server of this project's.

Any service you connect to can see your IP address and roughly when you asked. That is how the
internet works, and it is worth saying plainly rather than leaving implied.

| When | Where it goes | What is sent |
|---|---|---|
| Opening the app, and reading any poem not already downloaded | **cdn.jsdelivr.net** — the poems are static files in a public GitHub repository, served over jsDelivr's CDN | which file you are reading |
| Opening-line previews, poet portraits, searching poems, listing recitations | **api.ganjoor.net** — Ganjoor's own public API | the poem or poet you are looking at, or your search text |
| Playing a recitation | wherever Ganjoor's API points the audio file | the recording you chose |
| Looking a word up | **nowhere** — the dictionary is bundled in the app and works with no connection |

Turning on **offline mode** stops all of it: the app then refuses the network entirely and reads
only what is already on the device.

## Translation of Ganjoor's descriptions

Ganjoor writes AI descriptions for many poems. With the app in Urdu or English, they can be
translated — and the two builds do this differently, which matters:

- The **Google Play build** translates **on your device** with Google's ML Kit. The text of the
  description does not leave your phone. Each language pack is downloaded once from Google
  (about 30 MB), and after that it works with no connection.
- The **F-Droid and direct-download build** contains no Google library. It calls Google's Cloud
  Translation API over HTTPS **with an API key you provide yourself**, which means the text of
  the description is sent to Google under your own Google Cloud account and their terms.

Either way, translation only happens if you leave it switched on, and only the description is
sent — never a whole poem, never anything about you.

## The optional AI assistant

Off by default. No account, no key and no endpoint ship with the app; nothing reaches any model
until you configure one yourself.

When you ask for a translation or an explanation, **only the verse or passage you selected** is
sent to the endpoint **you** configured — your own server, or a hosted API with your own key
(Gemini, OpenAI, Claude, DeepSeek and OpenRouter have presets). What that service does with the
text is governed by your agreement with them, not by this policy. This project neither sees nor
relays it.

## What is never collected

No account or sign-in. No name, email, phone number or contacts. No location. No advertising
identifier. No analytics, usage statistics or crash reports. No cookies or tracking of any kind.
Nothing is sold or shared, because nothing is gathered in the first place.

## What Google Play is told

Google Play asks every app to declare what it collects, on a fixed list of categories. This is
what is declared for the Play build, and why:

| Google's category | Declared | Why |
|---|---|---|
| **App activity — in-app search history** | **Yes.** Collected, not shared. For app functionality. Not linked to your identity, never used to track you. | Searching poems sends what you typed to `api.ganjoor.net`, because the search runs there and not on your phone. Google counts anything that leaves the device as collected, whether or not the author ever sees it — and the author never does. |
| Location, personal information, financial information, health, messages, photos or video, audio files, files and documents, calendar, contacts, web browsing | No | None of it is touched, and the app holds no permission that would allow it. |
| Device or other identifiers | No | No advertising ID, no device ID, nothing of the sort. |
| App information and performance — crash logs, diagnostics | No | There is no crash reporter and no analytics in the app. Google Play collects crashes from every app by itself; that is Google's, not this project's. |

The AI assistant is not declared, because nothing is sent until you have configured an endpoint
yourself and then asked a question — the transfer is yours, to a service you chose. It is
described above all the same, since a form Google does not ask is still something you should
know.

The translation difference matters here too: the Play build translates on your device, and work
done on the device is not collection. The F-Droid and direct-download build, which is not on
Google Play, is the one that sends text to Google's Cloud Translation API under your own key.

## Children

The app shows classical Persian poetry and a dictionary. It is not directed at children, and
collects nothing from anyone regardless of age.

## Changes

This file lives with the source and changes through the same public history as the code, so any
edit can be seen and dated. Material changes will be noted in a release's changelog.

## Contact

Questions, or anything here that looks wrong:
<https://github.com/anas-rashid/ganjoorandroid/issues>
