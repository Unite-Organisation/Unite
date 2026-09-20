package com.app.prod.event.device;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Component
public class MembershipScorer {

    private static final List<Signal> SIGNALS = List.of(
            new Signal("deviceId", DeviceSignals::deviceIdHash, 120, 15),
            new Signal("deviceModel", DeviceSignals::deviceModel, 30, 25),
            new Signal("screen", DeviceSignals::screen, 25, 30),
            new Signal("ip", DeviceSignals::ipHash, 25, 5),
            new Signal("renderer", DeviceSignals::rendererHash, 22, 18),
            new Signal("canvas", DeviceSignals::canvasHash, 15, 5),
            new Signal("userAgent", DeviceSignals::userAgentHash, 15, 2),
            new Signal("osFamily", DeviceSignals::osFamily, 12, 40),
            new Signal("ipNetwork", DeviceSignals::ipNetworkHash, 12, 4),
            new Signal("browserFamily", DeviceSignals::browserFamily, 10, 2),
            new Signal("osVersion", DeviceSignals::osVersion, 8, 5),
            new Signal("pixelRatio", DeviceSignals::pixelRatio, 8, 10),
            new Signal("timeZone", DeviceSignals::timeZone, 6, 12),
            new Signal("languages", DeviceSignals::languages, 5, 8),
            new Signal("hardwareConcurrency", DeviceSignals::hardwareConcurrency, 5, 6),
            new Signal("browserMajor", DeviceSignals::browserMajor, 4, 1),
            new Signal("deviceMemory", DeviceSignals::deviceMemory, 4, 5),
            new Signal("maxTouchPoints", DeviceSignals::maxTouchPoints, 3, 6),
            new Signal("timeZoneOffset", DeviceSignals::timeZoneOffset, 3, 6),
            new Signal("colorDepth", DeviceSignals::colorDepth, 2, 2)
    );

    public int score(DeviceSignals caller, DeviceSignals stored) {
        if (caller == null || stored == null) {
            return 0;
        }

        int score = 0;
        for (Signal signal : SIGNALS) {
            Object callerValue = signal.value().apply(caller);
            Object storedValue = signal.value().apply(stored);
            if (callerValue == null || storedValue == null) {
                continue;
            }
            score += Objects.equals(callerValue, storedValue) ? signal.match() : -signal.mismatch();
        }
        return score;
    }

    public int bestScore(DeviceSignals caller, Collection<DeviceSignals> devices) {
        return devices.stream()
                .mapToInt(device -> score(caller, device))
                .max()
                .orElse(0);
    }

    private record Signal(String name, Function<DeviceSignals, Object> value, int match, int mismatch) {
    }
}
