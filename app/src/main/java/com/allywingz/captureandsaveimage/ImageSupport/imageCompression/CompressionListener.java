package com.allywingz.captureandsaveimage.ImageSupport.imageCompression;

public interface CompressionListener {
    void onStart();

    void onCompressed(String filePath);
}
