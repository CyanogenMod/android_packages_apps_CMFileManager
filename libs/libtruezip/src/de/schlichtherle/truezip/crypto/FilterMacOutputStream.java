package de.schlichtherle.truezip.crypto;

import libtruezip.lcrypto.crypto.Mac;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FilterMacOutputStream extends FilterOutputStream {
    protected Mac mac;

    public FilterMacOutputStream(OutputStream out, Mac mac) {
        super(out);
        this.mac = mac;
    }

    public void write(int b) throws IOException {
        mac.update((byte)b);
        out.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        mac.update(b, off, len);
        out.write(b, off, len);
    }

    public Mac getMac() {
        return mac;
    }
}