package de.lgohlke.pdf;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ConversionConfig {

    final int dpi;
    final int leftPercent;
    final int topPercent;
    final int widthPercent;
    final int heightPercent;
    final int compressionQuality;
}
