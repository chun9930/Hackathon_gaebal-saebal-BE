package com.mcm.privatecircle.stamp.dto;

import java.util.Map;

public final class StampImageResolver {

    private static final Map<String, String> STORE_STAMP_IMAGE_URLS = Map.ofEntries(
        Map.entry("MCM HAUS", "/images/stamps/journey-stamp-seoul-haus-flagship.png"),
        Map.entry("MCM 하우스 플래그십스토어", "/images/stamps/journey-stamp-seoul-haus-flagship.png"),
        Map.entry("MCM 롯데백화점 본점", "/images/stamps/journey-stamp-seoul-lotte-main.png"),
        Map.entry("MCM 롯데백화점 잠실점", "/images/stamps/journey-stamp-seoul-lotte-jamsil.png"),
        Map.entry("MCM 신라면세점 서울점", "/images/stamps/journey-stamp-seoul-shilla-duty-free.png")
    );

    private StampImageResolver() {
    }

    public static String resolve(String storeName) {
        if (storeName == null) {
            return null;
        }
        return STORE_STAMP_IMAGE_URLS.get(storeName);
    }
}
