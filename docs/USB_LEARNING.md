# Offline USB IR learning

This is the Android-side workflow for the ESP32 receive-only learner. It is part of the current development work and is separate from the v0.3.0 release UI.

## Goal

Use a small ESP32 + demodulating IR receiver as a **learning probe**. The ESP32 captures what a physical remote transmits; the Android app can then ingest those captures over USB OTG, label/store them locally, export them for review, and replay a valid frame through the phone's own IR blaster.

The ESP32 itself remains receive-only.

## Capture flow

1. Flash the ESP32 with [`firmware/ir_learner/ir_learner.ino`](../firmware/ir_learner/ir_learner.ino).
2. Connect the receiver to the configured input GPIO (GPIO 27 on the tested classic ESP32 build).
3. Connect the ESP32 to the Android phone using USB OTG and a data-capable cable.
4. Open **Settings → Advanced options → IR learning (Beta)** in the development build.
5. Grant Android USB access when requested.
6. Press buttons on the remote. Each frame is kept separately, including repeats and unknown protocols.
7. Stop capture, then name individual signals such as Power, Volume Up, Volume Down or Input.
8. Add optional brand/model/source metadata.
9. Export the session for inspection or replay a valid capture through the phone's consumer-IR hardware.

A phone-app capture and an original-remote capture are different evidence sources and should not be silently merged. Capturing a signal proves what the transmitter emitted; it does not by itself prove that a particular appliance accepts it.

## Stored evidence

The learning workflow preserves:

- raw serial bytes;
- immutable parsed capture records;
- decoded protocol/address/command/state fields when available;
- original mark/space timing envelope;
- repeat and overflow flags;
- sequence gaps and parser errors;
- user-added function/source labels;
- replay-test results separately from the original capture.

Unknown decoder hashes are never treated as transmittable protocol commands. When decoding fails, the raw timing envelope is the important evidence.

## Replay

Replay requires a phone with a real IR blaster. Root access does not create consumer-IR hardware.

Only complete, non-overflow frames should be replayed. A valid unknown protocol may be replayed from raw timings without converting its decoder hash into a command.

The demodulating receiver **cannot measure carrier frequency**. A 38 kHz receiver center frequency is not a carrier measurement. Use a documented protocol/manufacturer value where possible; otherwise any replay carrier is explicitly a trial value.

After replay, appliance response should be recorded as a separate user observation rather than modifying the original capture evidence.

## Philips SPA5250B/94 status

The prototype receive path has been tested with five test frames, but the original Philips SPA5250B/94 remote is not currently available. Therefore no Philips SPA5250B/94 IR command is marked as verified from this learner yet.

Once the original remote is available, capture every physical button several times. Multiple presses are particularly useful for protocols such as RC5/RC6 where toggle behavior can change between presses.

## Computer-side alternative

The same ESP32 can be recorded from a computer using [`tools/ir_capture/record.py`](../tools/ir_capture/record.py):

```sh
.tools/esp32/python/bin/python tools/ir_capture/record.py listen --port auto --source original_remote --brand Philips --model SPA5250B/94
```

Use [`tools/ir_capture/compare.py`](../tools/ir_capture/compare.py) to compare labeled sessions while keeping verification status separate.
