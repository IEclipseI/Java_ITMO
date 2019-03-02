package ru.ifmo.rain.krivopaltsev.walk;

public class FNVHash {
    private static final int FNV_32_INIT = 0x811c9dc5;
    private static final int FNV_32_PRIME = 0x01000193;
    private int res;

    FNVHash() {
        res = FNV_32_INIT;
    }

    void hash32(final byte[] k, final int size) {
        for (int i = 0; i < size; i++) {
            res = (res * FNV_32_PRIME) ^ (k[i] & 0xff);
        }
    }

    int getHash() {
        return res;
    }
}
