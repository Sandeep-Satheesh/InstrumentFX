#include <jni.h>
#include <string>
#include <android/log.h>
#include <OpenSource/SuperpoweredAndroidAudioIO.h>
#include <Superpowered.h>
#include <SuperpoweredSimple.h>
#include <SuperpoweredTimeStretching.h>
#include <unistd.h>
#include <SuperpoweredAdvancedAudioPlayer.h>
#include <SuperpoweredCPU.h>

static SuperpoweredAndroidAudioIO *masterAudioIO;
static Superpowered::AdvancedAudioPlayer *player;
static Superpowered::TimeStretching *timeStretchingMain, *timeStretchingDualTone;

bool playflag = false;
unsigned int sr = 0;
volatile int transpose = 0;
volatile int dualToneMode = 0;

static inline bool addPitchShiftInput(float *floatBuffer, int &frames) {
    timeStretchingMain->pitchShiftCents = transpose;
    timeStretchingMain->addInput(floatBuffer, frames);
    return timeStretchingMain->getOutputLengthFrames() >= frames;
}

static inline bool transposeAudio(float *input, float *transposedOutput, int &numberOfFrames) {
    return (addPitchShiftInput(input, numberOfFrames) &&
            timeStretchingMain->getOutput(transposedOutput, numberOfFrames));
}

inline static bool createDualToneBuffer(float *input, float *output, int &frames) {
    timeStretchingDualTone->pitchShiftCents = dualToneMode * 1200 + transpose;
    timeStretchingDualTone->addInput(input, frames);
    return timeStretchingDualTone->getOutputLengthFrames() >= frames &&
           timeStretchingDualTone->getOutput(output, frames);
}

// This is called periodically by the audio I/O.
static bool audioProcessing(
        void *__unused clientdata, // custom pointer
        short int *audio,           // buffer of interleaved samples
        int numberOfFrames,         // number of frames to process
        int __unused samplerate     // current sample rate in Hz
) {
    if (playflag) {
        numberOfFrames = (unsigned) numberOfFrames;
        float floatBuffer[numberOfFrames * 2];
        Superpowered::ShortIntToFloat(audio, floatBuffer, numberOfFrames);
        if (!transpose && !dualToneMode) {
            player->processStereo(floatBuffer, false, numberOfFrames);
            Superpowered::FloatToShortInt(floatBuffer, audio, numberOfFrames);
            return true;
        }
        float outputBuffer[numberOfFrames * 2];
        Superpowered::ShortIntToFloat(audio, outputBuffer, numberOfFrames);

        if (transpose)
            transposeAudio(floatBuffer, outputBuffer, numberOfFrames);

        if (dualToneMode) {
            float dualToneBuffer[numberOfFrames * 2];
            createDualToneBuffer(floatBuffer, dualToneBuffer, numberOfFrames);
            if (dualToneMode == -1)
                for (int i = 0; i < numberOfFrames * 2; i++)
                    dualToneBuffer[i] *= 2.5f;
            else
                for (int i = 0; i < numberOfFrames * 2; i++)
                    dualToneBuffer[i] *= 1.5f;
            Superpowered::Add1(dualToneBuffer, outputBuffer, numberOfFrames * 2);
        }
        player->processStereo(outputBuffer, false, numberOfFrames);
        Superpowered::FloatToShortInt(outputBuffer, audio, numberOfFrames);
    }
    return playflag;
}

extern "C" JNIEXPORT void
Java_com_sandeepdev_instrumentfx_AudioEngineInterface_InitEngine(
        JNIEnv *env,
        jobject  __unused obj,
        jint samplerate,
        jint buffersize) {
    Superpowered::Initialize(
            "ExampleLicenseKey-WillExpire-OnNextUpdate",
            true, // enableAudioAnalysis (using SuperpoweredAnalyzer, SuperpoweredLiveAnalyzer, SuperpoweredWaveform or SuperpoweredBandpassFilterbank)
            true, // enableFFTAndFrequencyDomain (using SuperpoweredFrequencyDomain, SuperpoweredFFTComplex, SuperpoweredFFTReal or SuperpoweredPolarFFT)
            true, // enableAudioTimeStretching (using SuperpoweredTimeStretching)
            true, // enableAudioEffects (using any SuperpoweredFX class)
            true, // enableAudioPlayerAndDecoder (using SuperpoweredAdvancedAudioPlayer or SuperpoweredDecoder)
            false, // enableCryptographics (using Superpowered::RSAPublicKey, Superpowered::RSAPrivateKey, Superpowered::hasher or Superpowered::AES)
            false  // enableNetworking (using Superpowered::httpRequest)
    );
    sr = (unsigned) samplerate;

    player = new Superpowered::AdvancedAudioPlayer(sr, 0);

    timeStretchingMain = new Superpowered::TimeStretching(sr);
    timeStretchingDualTone = new Superpowered::TimeStretching(sr);

    // Initialize audio engine with audio callback function.
    masterAudioIO = new SuperpoweredAndroidAudioIO(
            samplerate,      // native sample rate
            buffersize,      // native buffer size
            true,            // enableInput
            true,           // enableOutput
            audioProcessing, // process callback function
            NULL             // clientData
    );
    Superpowered::CPU::setSustainedPerformanceMode(true); // prevent dropouts
}

// StopAudio - Stop audio engine and free audio buffer.
extern "C" JNIEXPORT void
Java_com_sandeepdev_instrumentfx_AudioEngineInterface_Cleanup(JNIEnv *__unused env, jobject __unused obj) {
    player->pause();
    delete masterAudioIO;
    delete timeStretchingMain;
    delete timeStretchingDualTone;
    __android_log_print(ANDROID_LOG_DEBUG, "Recorder", "Cleaning up...");
    delete player;
}

// onBackground - Put audio processing to sleep if no audio is playing.
extern "C" JNIEXPORT void
Java_com_sandeepdev_instrumentfx_AudioEngineInterface_onBackground(JNIEnv *__unused env,
                                                          jobject __unused obj) {
    masterAudioIO->onBackground();
}

// onForeground - Resume audio processing.
extern "C" JNIEXPORT void
Java_com_sandeepdev_instrumentfx_AudioEngineInterface_onForeground(JNIEnv *__unused env,
                                                          jobject __unused obj) {
    masterAudioIO->onForeground();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_sandeepdev_instrumentfx_AudioEngineInterface_TogglePlayback(JNIEnv *env, jobject thiz) {
    // TODO: implement TogglePlayback()
    if (playflag)
        player->pause();
    else
        player->play();

    playflag = !playflag;
}extern "C"
JNIEXPORT void JNICALL
Java_com_sandeepdev_instrumentfx_AudioEngineInterface_TransposeToCents(JNIEnv *env, jobject thiz,
                                                              jint value) {
    transpose = value;
}extern "C"
JNIEXPORT jboolean JNICALL
Java_com_sandeepdev_instrumentfx_AudioEngineInterface_isPlaying(JNIEnv *env, jobject thiz) {
    return static_cast<jboolean>(playflag);
}extern "C"
JNIEXPORT void JNICALL
Java_com_sandeepdev_instrumentfx_AudioEngineInterface_SetDualToneMode(JNIEnv *env, jobject thiz, jint mode) {
    dualToneMode = mode;
}