package com.sandeepdev.instrumentfx;

class AudioEngineInterface {
    private int bufsize, sr;
    private volatile short dualToneMode = 0, transposeLevel = 0;

    AudioEngineInterface(int buffersize, int samplerate) {
        System.loadLibrary("AudioEngine");
        InitEngine(samplerate, buffersize);
        bufsize = buffersize;
        sr = samplerate;
    }

    void startAudioOutput() {
        if (!isPlaying())
            TogglePlayback();
    }

    void stopAudioOutput() {
        if (isPlaying())
            TogglePlayback();
    }

    void runInBackground() {
        onBackground();
    }

    void runInForeground() {
        onForeground();
    }

    void cleanup() {
        Cleanup();
    }

    void setDualToneMode(int mode) {
        dualToneMode = (short) mode;
        SetDualToneMode(mode);
    }

    void transpose(int i) {
        TransposeToCents(i);
        transposeLevel = (short) i;
    }

    native void InitEngine(int samplerate, int buffersize);

    native void onForeground();

    native void onBackground();

    native void TogglePlayback();

    native void Cleanup();

    native void SetDualToneMode(int mode);

    native void TransposeToCents(int value);

    native boolean isPlaying();
}
