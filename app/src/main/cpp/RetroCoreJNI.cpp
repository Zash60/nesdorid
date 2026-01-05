#include <jni.h>
#include <string>
#include <cstring>
#include <android/log.h>
#include "mesen/Libretro/libretro.h"

#define LOG_TAG "RetroCoreJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static retro_environment_t environ_cb = nullptr;
static retro_video_refresh_t video_cb = nullptr;
static retro_audio_sample_t audio_sample_cb = nullptr;
static retro_audio_sample_batch_t audio_batch_cb = nullptr;
static retro_input_poll_t input_poll_cb = nullptr;
static retro_input_state_t input_state_cb = nullptr;

static int16_t input_states[16][4][2][128] = {0}; // port, device, index, id

// Callbacks
bool retro_environment(unsigned cmd, void *data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT: {
            enum retro_pixel_format *fmt = (enum retro_pixel_format *)data;
            LOGI("Setting pixel format: %d", *fmt);
            return true;
        }
        case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
        case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
            // Return null for now
            *(const char **)data = nullptr;
            return true;
        default:
            return false;
    }
}

void retro_video_refresh(const void *data, unsigned width, unsigned height, size_t pitch) {
    // No video output in this integration
    LOGI("Video refresh: %dx%d", width, height);
}

void retro_audio_sample(int16_t left, int16_t right) {
    // No audio output
}

size_t retro_audio_sample_batch(const int16_t *data, size_t frames) {
    return frames;
}

void retro_input_poll() {
    // No polling needed
}

int16_t retro_input_state(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (port < 16 && device < 4 && index < 2 && id < 128) {
        return input_states[port][device][index][id];
    }
    return 0;
}

extern "C" {

JNIEXPORT void JNICALL Java_com_example_nesdorid_RetroCore_retroInit(JNIEnv *env, jobject obj) {
    retro_set_environment(retro_environment);
    retro_set_video_refresh(retro_video_refresh);
    retro_set_audio_sample(retro_audio_sample);
    retro_set_audio_sample_batch(retro_audio_sample_batch);
    retro_set_input_poll(retro_input_poll);
    retro_set_input_state(retro_input_state);
    retro_init();
    LOGI("Retro core initialized");
}

JNIEXPORT void JNICALL Java_com_example_nesdorid_RetroCore_retroDeinit(JNIEnv *env, jobject obj) {
    retro_deinit();
    LOGI("Retro core deinitialized");
}

JNIEXPORT jboolean JNICALL Java_com_example_nesdorid_RetroCore_retroLoadGame(JNIEnv *env, jobject obj, jobject gameInfo) {
    jclass cls = env->GetObjectClass(gameInfo);
    jfieldID fidPath = env->GetFieldID(cls, "path", "Ljava/lang/String;");
    jfieldID fidData = env->GetFieldID(cls, "data", "[B");
    jfieldID fidSize = env->GetFieldID(cls, "size", "J");
    jfieldID fidMeta = env->GetFieldID(cls, "meta", "Ljava/lang/String;");

    jstring path = (jstring)env->GetObjectField(gameInfo, fidPath);
    jbyteArray data = (jbyteArray)env->GetObjectField(gameInfo, fidData);
    jlong size = env->GetLongField(gameInfo, fidSize);
    jstring meta = (jstring)env->GetObjectField(gameInfo, fidMeta);

    struct retro_game_info game;
    game.path = path ? env->GetStringUTFChars(path, nullptr) : nullptr;
    game.data = data ? env->GetByteArrayElements(data, nullptr) : nullptr;
    game.size = size;
    game.meta = meta ? env->GetStringUTFChars(meta, nullptr) : nullptr;

    bool result = retro_load_game(&game);

    if (game.path) env->ReleaseStringUTFChars(path, game.path);
    if (game.data) env->ReleaseByteArrayElements(data, (jbyte*)game.data, JNI_ABORT);
    if (game.meta) env->ReleaseStringUTFChars(meta, game.meta);

    LOGI("Game loaded: %s", result ? "success" : "failed");
    return result;
}

JNIEXPORT void JNICALL Java_com_example_nesdorid_RetroCore_retroUnloadGame(JNIEnv *env, jobject obj) {
    retro_unload_game();
    LOGI("Game unloaded");
}

JNIEXPORT void JNICALL Java_com_example_nesdorid_RetroCore_retroRun(JNIEnv *env, jobject obj) {
    retro_run();
}

JNIEXPORT void JNICALL Java_com_example_nesdorid_RetroCore_retroReset(JNIEnv *env, jobject obj) {
    retro_reset();
    LOGI("Core reset");
}

JNIEXPORT jint JNICALL Java_com_example_nesdorid_RetroCore_getInputState(JNIEnv *env, jobject obj, jint port, jint device, jint index, jint id) {
    if (port < 16 && device < 4 && index < 2 && id < 128) {
        return input_states[port][device][index][id];
    }
    return 0;
}

// Note: To set input, we need a separate method
JNIEXPORT void JNICALL Java_com_example_nesdorid_RetroCore_setInputState(JNIEnv *env, jobject obj, jint port, jint device, jint index, jint id, jint state) {
    if (port < 16 && device < 4 && index < 2 && id < 128) {
        input_states[port][device][index][id] = state;
    }
}

JNIEXPORT jobject JNICALL Java_com_example_nesdorid_RetroCore_retroGetSystemAvInfo(JNIEnv *env, jobject obj) {
    struct retro_system_av_info av_info;
    retro_get_system_av_info(&av_info);

    jclass geometryCls = env->FindClass("com/example/nesdorid/RetroGameGeometry");
    jmethodID geometryCtor = env->GetMethodID(geometryCls, "<init>", "(IIIIF)V");
    jobject geometry = env->NewObject(geometryCls, geometryCtor,
        av_info.geometry.base_width, av_info.geometry.base_height,
        av_info.geometry.max_width, av_info.geometry.max_height,
        av_info.geometry.aspect_ratio);

    jclass timingCls = env->FindClass("com/example/nesdorid/RetroSystemTiming");
    jmethodID timingCtor = env->GetMethodID(timingCls, "<init>", "(DD)V");
    jobject timing = env->NewObject(timingCls, timingCtor,
        av_info.timing.fps, av_info.timing.sample_rate);

    jclass avCls = env->FindClass("com/example/nesdorid/RetroSystemAvInfo");
    jmethodID avCtor = env->GetMethodID(avCls, "<init>", "(Lcom/example/nesdorid/RetroGameGeometry;Lcom/example/nesdorid/RetroSystemTiming;)V");
    return env->NewObject(avCls, avCtor, geometry, timing);
}

JNIEXPORT jshort JNICALL Java_com_example_nesdorid_RetroCore_retroGetAudioSample(JNIEnv *env, jobject obj) {
    // Not implemented, return 0
    return 0;
}

JNIEXPORT jint JNICALL Java_com_example_nesdorid_RetroCore_retroGetAudioSampleBatch(JNIEnv *env, jobject obj, jshortArray buffer, jint frames) {
    // Not implemented
    return 0;
}

JNIEXPORT jlong JNICALL Java_com_example_nesdorid_RetroCore_retroSerializeSize(JNIEnv *env, jobject obj) {
    return retro_serialize_size();
}

JNIEXPORT jboolean JNICALL Java_com_example_nesdorid_RetroCore_retroSerialize(JNIEnv *env, jobject obj, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte* buf = env->GetByteArrayElements(data, nullptr);
    bool result = retro_serialize(buf, len);
    env->ReleaseByteArrayElements(data, buf, 0);
    return result;
}

JNIEXPORT jboolean JNICALL Java_com_example_nesdorid_RetroCore_retroUnserialize(JNIEnv *env, jobject obj, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte* buf = env->GetByteArrayElements(data, nullptr);
    bool result = retro_unserialize(buf, len);
    env->ReleaseByteArrayElements(data, buf, JNI_ABORT);
    return result;
}

JNIEXPORT void JNICALL Java_com_example_nesdorid_RetroCore_retroCheatSet(JNIEnv *env, jobject obj, jint index, jboolean enabled, jstring code) {
    const char* code_str = code ? env->GetStringUTFChars(code, nullptr) : nullptr;
    retro_cheat_set(index, enabled, code_str);
    if (code_str) env->ReleaseStringUTFChars(code, code_str);
}

JNIEXPORT void JNICALL Java_com_example_nesdorid_RetroCore_retroCheatSetBatch(JNIEnv *env, jobject obj, jobjectArray cheats) {
    jsize len = env->GetArrayLength(cheats);
    jclass cheatCls = env->FindClass("com/example/nesdorid/Cheat");
    jfieldID codeField = env->GetFieldID(cheatCls, "code", "Ljava/lang/String;");
    jfieldID enabledField = env->GetFieldID(cheatCls, "enabled", "Z");

    for (jsize i = 0; i < len; i++) {
        jobject cheat = env->GetObjectArrayElement(cheats, i);
        jstring code = (jstring)env->GetObjectField(cheat, codeField);
        jboolean enabled = env->GetBooleanField(cheat, enabledField);
        const char* code_str = code ? env->GetStringUTFChars(code, nullptr) : nullptr;
        retro_cheat_set(i, enabled, code_str);
        if (code_str) env->ReleaseStringUTFChars(code, code_str);
        env->DeleteLocalRef(cheat);
    }
}

JNIEXPORT void JNICALL Java_com_example_nesdorid_RetroCore_retroSetPixelFormat(JNIEnv *env, jobject obj, jint format) {
    // This is handled in environment callback
}

JNIEXPORT jint JNICALL Java_com_example_nesdorid_RetroCore_retroGetRegion(JNIEnv *env, jobject obj) {
    return retro_get_region();
}

JNIEXPORT void JNICALL Java_com_example_nesdorid_RetroCore_retroSetControllerPortDevice(JNIEnv *env, jobject obj, jint port, jint device) {
    retro_set_controller_port_device(port, device);
}

JNIEXPORT jlong JNICALL Java_com_example_nesdorid_RetroCore_retroGetMemorySize(JNIEnv *env, jobject obj, jint id) {
    return retro_get_memory_size(id);
}

JNIEXPORT jbyteArray JNICALL Java_com_example_nesdorid_RetroCore_retroGetMemoryData(JNIEnv *env, jobject obj, jint id) {
    size_t size = retro_get_memory_size(id);
    if (size == 0) return nullptr;
    void* data = retro_get_memory_data(id);
    if (!data) return nullptr;
    jbyteArray arr = env->NewByteArray(size);
    env->SetByteArrayRegion(arr, 0, size, (jbyte*)data);
    return arr;
}

}