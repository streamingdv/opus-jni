package com.grill.opuscodec;

public class OpusEncoder {

    private final OpusCodec opusEncoder;

    public OpusEncoder() {
        this.opusEncoder = new OpusCodec();
    }

    public void initEncoder(final int sampleRate, final int channels, final int application) {
        this.opusEncoder.initEncoder(sampleRate, channels, application);
    }

    public void setEncoderBitrate(final int bitrate) {
        this.opusEncoder.setEncoderBitrate(bitrate);
    }

    public void setEncoderComplexity(final int complexity) {
        this.opusEncoder.setEncoderComplexity(complexity);
    }

    public int encode(final short[] buffer, final int frames, final byte[] out) {
        return this.opusEncoder.encode(buffer, frames, out);
    }

    public int encode(final byte[] buffer, final int frames, final byte[] out) {
        return this.opusEncoder.encode(buffer, frames, out);
    }

    public void closeEncoder() {
        this.opusEncoder.closeEncoder();
    }
}
