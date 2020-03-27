package com.sandeepdev.instrumentfx;

class AudioEngine {

    AudioEngine(int buffersize, int samplerate) {
        System.loadLibrary("AudioEngine");
        InitEngine(samplerate, buffersize);
    }

    void startMasterAudioOutput() {
        if (!isPlaying())
            TogglePlayback();
    }

    void stopMasterAudioOutput() {
        if (isPlaying())
            TogglePlayback();
    }

    // Functions implemented in the native library.
    native void InitEngine(int samplerate, int buffersize);

    native void onForeground();

    native void onBackground();

    native void TogglePlayback();

    native void Cleanup();

    native void SetDualToneMode(int mode);

    native void TransposeToCents(int value);

    native boolean isPlaying();
}
