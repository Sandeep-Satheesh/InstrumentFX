# InstrumentFX

This is an Android app that aims to manipulate audio in realtime and add effects to it like transpose (pitch shift), dual tone etc. The app deals with analog audio from the device mic, and outputs the processed audio through the speakers. A TRRS jack can be connected to route the audio through a speaker (or also input audio through line-in).

The goal is to enable players to bring in advanced audio effects like portamento, legato and pitch bend found in expensive synthesizers (keyboards).
Despite this bias towards synthesizers and musical instruments, this app is applicable to any source of audio (voice input etc.) that has a low noise profile, so that the effects are clearly audible in the output.

This app uses the Superpowered Audio SDK to achieve fast processing speed and low latency in the output audio, at almost the same time after the sound is received.
