package com.prayas.platform.auth;

/**
 * Present on a request only while a lead/faculty member is using the local
 * "test as" switch (see DevActAsFilter). Reports who is *really* signed in,
 * so the UI can show a clear "you are testing as someone else" banner.
 */
public record ActingAs(String realName, String realEmail) {
    public static final String REQUEST_ATTRIBUTE = "prayas.dev.actingAs";
}
