#include "com_grill_opuscodec_OpusCodec.h"

#include <jni.h>
#include <opus.h>
#include <malloc.h>

/*** decoding ***/
JNIEXPORT jint JNICALL Java_com_grill_opuscodec_OpusCodec_nativeInitDecoder (JNIEnv *env, jobject obj, jint samplingRate, jint numberOfChannels)
{
	int size;
	int error;

	size = opus_decoder_get_size(numberOfChannels);
	OpusDecoder* dec = malloc(size);
	if (dec == NULL) {
        return -7; // OPUS_ALLOC_FAIL
    }
	error = opus_decoder_init(dec, samplingRate, numberOfChannels);

    if (error) {
        free(dec);
    } else {
        jclass cls = (*env)->GetObjectClass(env, obj);
        jfieldID fid = (*env)->GetFieldID(env, cls, "addressDecoder", "J");
        (*env)->SetLongField(env, obj, fid, (jlong)dec);
    }

	return error;
}

static jfieldID addressDecoderFieldID = NULL;

JNIEXPORT jint JNICALL Java_com_grill_opuscodec_OpusCodec_nativeDecodeShorts (JNIEnv *env, jobject obj, jbyteArray in, jshortArray out, jint frames)
{
    // Cache the jfieldID on the first call
    if (addressDecoderFieldID == NULL) {
        jclass cls = (*env)->GetObjectClass(env, obj);
        addressDecoderFieldID = (*env)->GetFieldID(env, cls, "addressDecoder", "J");
    }

    OpusDecoder* dec = (OpusDecoder*)((*env)->GetLongField(env, obj, addressDecoderFieldID));
    if (dec == NULL) {
        return OPUS_BAD_ARG; // Handle null decoder pointer
    }

    jint inputArraySize = (*env)->GetArrayLength(env, in);
    jbyte* encodedData = (*env)->GetByteArrayElements(env, in, 0);
    jshort* decodedData = (*env)->GetShortArrayElements(env, out, 0);
    int samples = opus_decode(dec, (const unsigned char *) encodedData, inputArraySize, decodedData, frames, 0);

    (*env)->ReleaseByteArrayElements(env, in, encodedData, JNI_ABORT);
    (*env)->ReleaseShortArrayElements(env, out, decodedData, 0);

    return samples;
}

JNIEXPORT jint JNICALL Java_com_grill_opuscodec_OpusCodec_nativeDecodeBytes (JNIEnv *env, jobject obj, jbyteArray in, jbyteArray out, jint frames)
{
    // Cache the jfieldID on the first call
    if (addressDecoderFieldID == NULL) {
        jclass cls = (*env)->GetObjectClass(env, obj);
        addressDecoderFieldID = (*env)->GetFieldID(env, cls, "addressDecoder", "J");
    }

    OpusDecoder* dec = (OpusDecoder*)((*env)->GetLongField(env, obj, addressDecoderFieldID));
    if (dec == NULL) {
        return OPUS_BAD_ARG; // Handle null decoder pointer
    }

    jint inputArraySize = (*env)->GetArrayLength(env, in);
    jbyte* encodedData = (*env)->GetByteArrayElements(env, in, 0);
    jbyte* decodedData = (*env)->GetByteArrayElements(env, out, 0);
    int samples = opus_decode(dec, (const unsigned char *) encodedData, inputArraySize, (opus_int16 *) decodedData, frames, 0);

    (*env)->ReleaseByteArrayElements(env, in, encodedData, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, out, decodedData, 0);

    return samples;
}

JNIEXPORT jint JNICALL Java_com_grill_opuscodec_OpusCodec_nativeDecodeByteBuffer (JNIEnv *env, jobject obj, jobject encodedBuffer, jobject decodedBuffer, jint frames)
{
    // Cache the jfieldID on the first call
    if (addressDecoderFieldID == NULL) {
        jclass cls = (*env)->GetObjectClass(env, obj);
        addressDecoderFieldID = (*env)->GetFieldID(env, cls, "addressDecoder", "J");
    }

    OpusDecoder* dec = (OpusDecoder*)((*env)->GetLongField(env, obj, addressDecoderFieldID));
    if (dec == NULL) {
        return OPUS_BAD_ARG; // Handle null decoder pointer
    }

    // Use GetDirectBufferAddress to access the native memory of the direct buffers
    jbyte* encodedData = (jbyte*)(*env)->GetDirectBufferAddress(env, encodedBuffer);
    jbyte* decodedData = (jbyte*)(*env)->GetDirectBufferAddress(env, decodedBuffer);

    if (encodedData == NULL || decodedData == NULL) {
        return -1; // Buffer must not be null
    }

    jint encodedBufferSize = (*env)->GetDirectBufferCapacity(env, encodedBuffer);
    int samples = opus_decode(dec, (const unsigned char*)encodedData, encodedBufferSize, (opus_int16*)decodedData, frames, 0);

    return samples;
}

JNIEXPORT jboolean JNICALL Java_com_grill_opuscodec_OpusCodec_nativeReleaseDecoder (JNIEnv *env, jobject obj)
{
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, "addressDecoder", "J");
    OpusDecoder* enc = (OpusDecoder*)((*env)->GetLongField(env, obj, fid));
    free(enc);
    (*env)->SetLongField(env, obj, fid, (jlong)NULL);
    return 1;
}
/*** encoding ***/
JNIEXPORT jint JNICALL Java_com_grill_opuscodec_OpusCodec_nativeInitEncoder(JNIEnv *env, jobject obj,
                                                           jint samplingRate, jint numberOfChannels,
                                                           jint application) {
    int error = 0;
    int size;

    size = opus_encoder_get_size(numberOfChannels);
    OpusEncoder *enc = malloc(size);
    if (enc == NULL) {
        return -7; // OPUS_ALLOC_FAIL
    }
    error = opus_encoder_init(enc, samplingRate, numberOfChannels, application);

    opus_encoder_ctl(enc, OPUS_SET_SIGNAL_REQUEST, 3002);
    opus_encoder_ctl(enc, OPUS_SET_BITRATE_REQUEST, 32000);
    opus_encoder_ctl(enc, OPUS_SET_VBR_REQUEST, 0);

    if (error) {
        free(enc);
    } else {
        jclass cls = (*env)->GetObjectClass(env, obj);
        jfieldID fid = (*env)->GetFieldID(env, cls, "addressEncoder", "J");
        (*env)->SetLongField(env, obj, fid, (jlong) enc);
    }

    return error;
}

JNIEXPORT jint JNICALL Java_com_grill_opuscodec_OpusCodec_nativeSetBitrate(JNIEnv *env, jobject obj, jint bitrate) {
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, "addressEncoder", "J");
    OpusEncoder *enc = (OpusEncoder *) ((*env)->GetLongField(env, obj, fid));
    return opus_encoder_ctl(enc, OPUS_SET_BITRATE(bitrate));
}

JNIEXPORT jint JNICALL Java_com_grill_opuscodec_OpusCodec_nativeSetComplexity(JNIEnv *env, jobject obj,
                                                             jint complexity) {
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, "addressEncoder", "J");
    OpusEncoder *enc = (OpusEncoder *) ((*env)->GetLongField(env, obj, fid));

    return opus_encoder_ctl(enc, OPUS_SET_COMPLEXITY(complexity));
}

static jfieldID addressEncoderFieldID = NULL;

JNIEXPORT jint JNICALL Java_com_grill_opuscodec_OpusCodec_nativeEncodeShorts(JNIEnv *env, jobject obj,
                                                            jshortArray in, jint frames,
                                                            jbyteArray out) {
    // Cache the jfieldID on the first call
    if (addressEncoderFieldID == NULL) {
        jclass cls = (*env)->GetObjectClass(env, obj);
        addressEncoderFieldID = (*env)->GetFieldID(env, cls, "addressEncoder", "J");
    }

    OpusEncoder *enc = (OpusEncoder *) ((*env)->GetLongField(env, obj, addressEncoderFieldID));
    if (enc == NULL) {
        return OPUS_BAD_ARG; // Handle null encoder pointer
    }

    jint outputArraySize = (*env)->GetArrayLength(env, out);

    jshort *audioSignal = (*env)->GetShortArrayElements(env, in, 0);
    jbyte *encodedSignal = (*env)->GetByteArrayElements(env, out, 0);

    int dataArraySize = opus_encode(enc, audioSignal, frames,
                                    (unsigned char *) encodedSignal, outputArraySize);

    (*env)->ReleaseShortArrayElements(env, in, audioSignal, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, out, encodedSignal, 0);

    return dataArraySize;
}

JNIEXPORT jint JNICALL Java_com_grill_opuscodec_OpusCodec_nativeEncodeBytes(JNIEnv *env, jobject obj, jbyteArray in,
                                                           jint frames, jbyteArray out) {
    // Cache the jfieldID on the first call
    if (addressEncoderFieldID == NULL) {
        jclass cls = (*env)->GetObjectClass(env, obj);
        addressEncoderFieldID = (*env)->GetFieldID(env, cls, "addressEncoder", "J");
    }

    OpusEncoder *enc = (OpusEncoder *) ((*env)->GetLongField(env, obj, addressEncoderFieldID));
    if (enc == NULL) {
        return OPUS_BAD_ARG; // Handle null encoder pointer
    }

    jint outputArraySize = (*env)->GetArrayLength(env, out);

    jbyte *audioSignal = (*env)->GetByteArrayElements(env, in, 0);
    jbyte *encodedSignal = (*env)->GetByteArrayElements(env, out, 0);

    // Check for unaligned audio signal
    if (((unsigned long) audioSignal) % 2) {
        return OPUS_BAD_ARG;
    }

    int dataArraySize = opus_encode(enc, (const opus_int16 *) audioSignal, frames,
                                    (unsigned char *) encodedSignal, outputArraySize);

    (*env)->ReleaseByteArrayElements(env, in, audioSignal, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, out, encodedSignal, 0);

    return dataArraySize;
}

JNIEXPORT jboolean JNICALL Java_com_grill_opuscodec_OpusCodec_nativeReleaseEncoder(JNIEnv *env, jobject obj) {
    jclass cls = (*env)->GetObjectClass(env, obj);
    jfieldID fid = (*env)->GetFieldID(env, cls, "addressEncoder", "J");
    OpusEncoder *enc = (OpusEncoder *) ((*env)->GetLongField(env, obj, fid));
    free(enc);
    (*env)->SetLongField(env, obj, fid, (jlong) NULL);
    return 1;
}