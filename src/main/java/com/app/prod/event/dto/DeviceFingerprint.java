package com.app.prod.event.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DeviceFingerprint {

    /** random UUID kept in localStorage - the strongest signal, but only within one browser */
    @Size(max = 64) private String deviceId;

    private Integer screenWidth;
    private Integer screenHeight;
    private Double pixelRatio;
    private Integer colorDepth;
    private Integer hardwareConcurrency;
    private Double deviceMemory;
    private Integer maxTouchPoints;

    @Size(max = 40) private String timeZone;
    private Integer timeZoneOffset;
    @Size(max = 120) private String languages;

    /** navigator.userAgentData.getHighEntropyValues - survives a change of browser on the same phone */
    @Size(max = 60) private String deviceModel;
    @Size(max = 20) private String platformVersion;

    /** WEBGL_debug_renderer_info, hashed on arrival */
    @Size(max = 120) private String renderer;
    @Size(max = 64) private String canvasHash;
}
