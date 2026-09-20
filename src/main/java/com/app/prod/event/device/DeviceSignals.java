package com.app.prod.event.device;

import com.app.prod.utils.Sha256;

import java.util.stream.Stream;

public record DeviceSignals(
        String deviceIdHash,
        String ipHash,
        String ipNetworkHash,
        String userAgentHash,
        String rendererHash,
        String canvasHash,
        String osFamily,
        String osVersion,
        String browserFamily,
        Integer browserMajor,
        String deviceModel,
        String screen,
        String pixelRatio,
        Integer colorDepth,
        Integer hardwareConcurrency,
        String deviceMemory,
        Integer maxTouchPoints,
        String timeZone,
        Integer timeZoneOffset,
        String languages
) {

    public String deviceKey() {
        if (deviceIdHash != null) {
            return deviceIdHash;
        }

        String hardware = Stream.of(userAgentHash, osFamily, deviceModel, screen, rendererHash)
                .map(value -> value == null ? "" : value)
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
        return Sha256.hex(hardware);
    }
}
