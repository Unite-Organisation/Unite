package com.app.prod.event.device;

import com.app.prod.event.dto.DeviceFingerprint;
import com.app.prod.event.web.RequestSignals;
import com.app.prod.utils.Sha256;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeviceSignalsFactory {

    private static final Pattern OS_VERSION = Pattern.compile("(?:Android|CPU(?: iPhone)? OS|Windows NT|Mac OS X) ([0-9_.]+)");
    private static final Pattern BROWSER = Pattern.compile("(Edg|OPR|SamsungBrowser|Firefox|Chrome|Safari)/([0-9]+)");

    public DeviceSignals from(RequestSignals request, DeviceFingerprint device) {
        String userAgent = request == null ? null : request.userAgent();
        DeviceFingerprint fingerprint = device == null ? new DeviceFingerprint() : device;

        return new DeviceSignals(
                Sha256.hexOrNull(fingerprint.getDeviceId()),
                Sha256.hexOrNull(request == null ? null : request.ipAddress()),
                Sha256.hexOrNull(networkOf(request == null ? null : request.ipAddress())),
                Sha256.hexOrNull(userAgent),
                Sha256.hexOrNull(fingerprint.getRenderer()),
                blankToNull(fingerprint.getCanvasHash()),
                osFamily(userAgent),
                osVersion(userAgent, fingerprint.getPlatformVersion()),
                browserFamily(userAgent),
                browserMajor(userAgent),
                blankToNull(fingerprint.getDeviceModel()),
                screen(fingerprint.getScreenWidth(), fingerprint.getScreenHeight()),
                decimal(fingerprint.getPixelRatio()),
                fingerprint.getColorDepth(),
                fingerprint.getHardwareConcurrency(),
                decimal(fingerprint.getDeviceMemory()),
                fingerprint.getMaxTouchPoints(),
                blankToNull(fingerprint.getTimeZone()),
                fingerprint.getTimeZoneOffset(),
                blankToNull(fingerprint.getLanguages())
        );
    }

    private static String screen(Integer width, Integer height) {
        if (width == null || height == null || width <= 0 || height <= 0) {
            return null;
        }
        return Math.min(width, height) + "x" + Math.max(width, height);
    }

    private static String networkOf(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }
        if (ipAddress.contains(":")) {
            String[] groups = ipAddress.split(":");
            return groups.length < 3 ? null : String.join(":", groups[0], groups[1], groups[2]);
        }
        String[] octets = ipAddress.split("\\.");
        return octets.length != 4 ? null : String.join(".", octets[0], octets[1], octets[2]);
    }

    private static String osFamily(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        if (userAgent.contains("Android")) return "ANDROID";
        if (userAgent.contains("iPhone") || userAgent.contains("iPad") || userAgent.contains("iPod")) return "IOS";
        if (userAgent.contains("Windows")) return "WINDOWS";
        if (userAgent.contains("Mac OS X")) return "MACOS";
        if (userAgent.contains("Linux")) return "LINUX";
        return null;
    }

    private static String osVersion(String userAgent, String platformVersion) {
        if (platformVersion != null && !platformVersion.isBlank()) {
            return platformVersion;
        }
        if (userAgent == null) {
            return null;
        }
        Matcher matcher = OS_VERSION.matcher(userAgent);
        return matcher.find() ? matcher.group(1).replace('_', '.') : null;
    }

    private static String browserFamily(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        if (userAgent.contains("FBAN") || userAgent.contains("FBAV")) return "FACEBOOK";
        if (userAgent.contains("Instagram")) return "INSTAGRAM";
        Matcher matcher = BROWSER.matcher(userAgent);
        return matcher.find() ? matcher.group(1).toUpperCase() : null;
    }

    private static Integer browserMajor(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        Matcher matcher = BROWSER.matcher(userAgent);
        return matcher.find() ? Integer.valueOf(matcher.group(2)) : null;
    }

    private static String decimal(Double value) {
        return value == null ? null : String.valueOf(Math.round(value * 100) / 100.0);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
