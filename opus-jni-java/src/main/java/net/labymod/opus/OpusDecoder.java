package com.grill.opuscodec;

import java.nio.ByteBuffer;

public class OpusDecoder {

    private final OpusCodec opusDecoder;

    public OpusDecoder() {
        this.opusDecoder = new OpusCodec();
    }

    public void initDecoder(final int sampleRate, final int channels) {
        this.opusDecoder.initDecoder(sampleRate, channels);
    }

    public int decode(final byte[] encodedBuffer, final short[] buffer, final int frames) {
        return this.opusDecoder.decode(encodedBuffer, buffer, frames);
    }

    public int decode(final byte[] encodedBuffer, final byte[] buffer, final int frames) {
        return this.opusDecoder.decode(encodedBuffer, buffer, frames);
    }

    public int decode(ByteBuffer encodedBuffer, final ByteBuffer buffer, final int frames) {
        return this.opusDecoder.decode(encodedBuffer, buffer, frames);
    }

    public void closeDecoder() {
        this.opusDecoder.closeDecoder();
    }

}
