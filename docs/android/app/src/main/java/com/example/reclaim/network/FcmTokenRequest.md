# FcmTokenRequest.java

## Overview
This file is a **DTO (Data Transfer Object)** that carries this device's push token to
the backend. A DTO holds one request body and fixes its JSON shape; see DTO, Gson, and
FCM in `docs/GLOSSARY.md`. `FcmTokenRequest` is the body for
`PUT api/users/me/fcm-token`. The token identifies this specific device to Firebase
Cloud Messaging, so the backend can send it push notifications.

## Concepts used here
- **DTO.** A carrier object for one request body; see `docs/GLOSSARY.md`.
- **FCM (Firebase Cloud Messaging).** Google's push-notification service; each device
  has a token; see `docs/GLOSSARY.md`.
- **Gson `@SerializedName`.** Maps the `token` field to the JSON key `token`; see
  `docs/GLOSSARY.md`.

## Libraries and imports
- **Gson (`com.google.gson.annotations.SerializedName`).** `@SerializedName("token")`
  sets the JSON key.

## Fields
- `@SerializedName("token") private final String token` — the FCM registration token
  for this device; sent as JSON key `token`.

Set by the constructor `FcmTokenRequest(String token)`. There are no getters; the
object is only serialized and sent.

## Where it's used
Built in `FcmTokenRegistrar.sendToken` and passed to
`ReClaimApiService.updateFcmToken`, so the backend knows where to deliver pushes.
