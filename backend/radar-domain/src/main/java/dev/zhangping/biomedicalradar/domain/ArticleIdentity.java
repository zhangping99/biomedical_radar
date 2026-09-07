package dev.zhangping.biomedicalradar.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class ArticleIdentity {
    private ArticleIdentity() {
    }

    public static String stableId(String sourceId, String sourceUniqueKey, String canonicalUrl) {
        String material = sourceId + "\n" + (sourceUniqueKey == null || sourceUniqueKey.isBlank()
                ? canonicalUrl : sourceUniqueKey);
        return UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static String contentHash(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : values) {
                digest.update((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
