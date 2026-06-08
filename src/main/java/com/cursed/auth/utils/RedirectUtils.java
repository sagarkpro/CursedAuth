package com.cursed.auth.utils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.web.util.UriComponentsBuilder;

public final class RedirectUtils {

    private RedirectUtils() {
    }

    /**
     * Appends query params to an already-valid base URI, percent-encoding each value
     * exactly once. The base URI is treated as already-encoded ({@code build(true)}) so it
     * is never re-encoded (no double-encoding of escapes already present in a registered
     * redirect_uri). Null values are skipped. Pass an ordered map to keep param order.
     */
    public static URI withParams(String baseUri, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUri);
        params.forEach((key, value) -> {
            if (value != null) {
                // URLEncoder is form-style (space -> '+'); normalize to RFC 3986 '%20'.
                String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
                builder.queryParam(key, encoded);
            }
        });
        return builder.build(true).toUri();
    }
}
