/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.api.config.MuleProperties;
import org.mule.util.MapUtils;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections.map.CaseInsensitiveMap;

/**
 * <code>HttpConstants</code> for request and response headers
 */
// @ThreadSafe
public class HttpConstants
{
    // HTTP prefix
    public static final String HTTP10 = "HTTP/1.0";
    public static final String HTTP1X = "HTTP/1.x";
    public static final String HTTP11 = "HTTP/1.1";
    public static final String DEFAULT_HTTP_VERSION = HttpConstants.HTTP11;

    // Default HTTP port
    public static final int DEFAULT_HTTP_PORT = 80;

    // HTTP Methods
    public static final String METHOD_OPTIONS = "OPTIONS";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_PATCH = "PATCH";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_TRACE = "TRACE";
    public static final String METHOD_CONNECT = "CONNECT";

    // Date header format
    public static final String DATE_FORMAT = "EEE, dd MMM yyyy hh:mm:ss zzz";
    public static final String DATE_FORMAT_RFC822 = "EEE, dd MMM yyyy HH:mm:ss Z";

    // Newline
    public static final String CRLF = "\r\n";
    // Mime/Content separator
    public static final String HEADER_CONTENT_SEPARATOR = CRLF + CRLF;

    // The default content type
    public static final String DEFAULT_CONTENT_TYPE = "text/plain";

    //form-urlencoded content type
    public static final String FORM_URLENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded";

    // Headers
    public static final String HEADER_ACCEPT = "Accept"; // [Request]
    public static final String HEADER_ACCEPT_CHARSET = "Accept-Charset"; // [Request]
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding"; // [Request]
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language"; // [Request]
    public static final String HEADER_ACCEPT_RANGES = "Accept-Ranges"; // [Response]
    public static final String HEADER_AGE = "Age"; // [Response]
    public static final String HEADER_ALLOW = "Allow"; // [Entity]
    public static final String HEADER_AUTHORIZATION = "Authorization"; // [Request]
    public static final String HEADER_CACHE_CONTROL = "Cache-Control"; // [General]
    public static final String HEADER_CONNECTION = "Connection"; // [General]
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding"; // [Entity]
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition"; // [Entity]
    public static final String HEADER_CONTENT_LANGUAGE = "Content-Language"; // [Entity]
    public static final String HEADER_CONTENT_LENGTH = "Content-Length"; // [Entity]
    public static final String HEADER_CONTENT_LOCATION = "Content-Location"; // [Entity]
    public static final String HEADER_CONTENT_MD5 = "Content-MD5"; // [Entity]
    public static final String HEADER_CONTENT_RANGE = "Content-Range"; // [Entity]
    public static final String HEADER_CONTENT_TYPE = "Content-Type"; // [Entity]
    public static final String HEADER_COOKIE = "Cookie"; // [Request]
    public static final String HEADER_COOKIE_SET = "Set-Cookie"; // [Response]
    public static final String HEADER_DATE = "Date"; // [General]
    public static final String HEADER_ETAG = "ETag"; // [Response]
    public static final String HEADER_EXPECT = "Expect"; // [Request]
    public static final String HEADER_EXPIRES = "Expires"; // [Entity]
    public static final String HEADER_FROM = "From"; // [Request]
    public static final String HEADER_HOST = "Host"; // [Request]
    public static final String HEADER_IF_MATCH = "If-Match"; // [Request]
    public static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since"; // [Request]
    public static final String HEADER_IF_NONE_MATCH = "If-None-Match"; // [Request]
    public static final String HEADER_IF_RANGE = "If-Range"; // [Request]
    public static final String HEADER_IF_UNMODIFIED_SINCE = "If-Unmodified-Since"; // [Request]
    public static final String HEADER_KEEP_ALIVE = "Keep-Alive"; // [Entity]
    public static final String HEADER_LAST_MODIFIED = "Last-Modified"; // [Entity]
    public static final String HEADER_LOCATION = "Location"; // [Response]
    public static final String HEADER_MAX_FORWARDS = "Max-Forwards"; // [Request]
    public static final String HEADER_PRAGMA = "Pragma"; // [General]
    public static final String HEADER_PROXY_AUTHENTICATE = "Proxy-Authenticate"; // [Response]
    public static final String HEADER_PROXY_AUTHORIZATION = "Proxy-Authorization"; // [Request]
    public static final String HEADER_RANGE = "Range"; // [Request]
    public static final String HEADER_REFERER = "Referer"; // [Request]
    public static final String HEADER_RETRY_AFTER = "Retry-After"; // [Response]
    public static final String HEADER_SERVER = "Server"; // [Response]
    public static final String HEADER_SLUG = "Slug"; // [Response]
    public static final String HEADER_TE = "TE"; // [Request]
    public static final String HEADER_TRAILER = "Trailer"; // [General]
    public static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding"; // [General]
    public static final String HEADER_UPGRADE = "Upgrade"; // [General]
    public static final String HEADER_USER_AGENT = "User-Agent"; // [Request]
    public static final String HEADER_VARY = "Vary"; // [Response]
    public static final String HEADER_VIA = "Via"; // [General]
    public static final String HEADER_WARNING = "Warning"; // [General]
    public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate"; // [Response]
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For"; // [Request]

    // This is a POST-related request header value
    public static final String HEADER_EXPECT_CONTINUE_REQUEST_VALUE = "100-continue"; // [Request]

    // Chunked transfer encoding indicator
    public static final String TRANSFER_ENCODING_CHUNKED = "chunked";

    // Custom http header prefix
    public static final String CUSTOM_HEADER_PREFIX = "X-";

    // Key for X-MULE headers
    public static final String X_PROPERTY_PREFIX = CUSTOM_HEADER_PREFIX + MuleProperties.PROPERTY_PREFIX;

    // Custom mule http header
    public static final String HEADER_MULE_SESSION = CUSTOM_HEADER_PREFIX + MuleProperties.MULE_SESSION_PROPERTY;

    // case-insenitive Maps of header names to their normalized representations
    public static final Map<String, String> REQUEST_HEADER_NAMES;
    public static final Map<String, String> RESPONSE_HEADER_NAMES;
    public static final Map<String, String> GENERAL_AND_ENTITY_HEADER_NAMES;
    public static final Map<String, String> ALL_HEADER_NAMES;

    // Status codes
    public static final int SC_CONTINUE = 100;
    public static final int SC_SWITCHING_PROTOCOLS = 101;
    public static final int SC_PROCESSING = 102;
    public static final int SC_OK = 200;
    public static final int SC_CREATED = 201;
    public static final int SC_ACCEPTED = 202;
    public static final int SC_NON_AUTHORITATIVE_INFORMATION = 203;
    public static final int SC_NO_CONTENT = 204;
    public static final int SC_RESET_CONTENT = 205;
    public static final int SC_PARTIAL_CONTENT = 206;
    public static final int SC_MULTI_STATUS = 207;
    public static final int SC_MULTIPLE_CHOICES = 300;
    public static final int SC_MOVED_PERMANENTLY = 301;
    public static final int SC_MOVED_TEMPORARILY = 302;
    public static final int SC_SEE_OTHER = 303;
    public static final int SC_NOT_MODIFIED = 304;
    public static final int SC_USE_PROXY = 305;
    public static final int SC_TEMPORARY_REDIRECT = 307;
    public static final int SC_BAD_REQUEST = 400;
    public static final int SC_UNAUTHORIZED = 401;
    public static final int SC_PAYMENT_REQUIRED = 402;
    public static final int SC_FORBIDDEN = 403;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_METHOD_NOT_ALLOWED = 405;
    public static final int SC_NOT_ACCEPTABLE = 406;
    public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
    public static final int SC_REQUEST_TIMEOUT = 408;
    public static final int SC_CONFLICT = 409;
    public static final int SC_GONE = 410;
    public static final int SC_LENGTH_REQUIRED = 411;
    public static final int SC_PRECONDITION_FAILED = 412;
    public static final int SC_REQUEST_TOO_LONG = 413;
    public static final int SC_REQUEST_URI_TOO_LONG = 414;
    public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    public static final int SC_EXPECTATION_FAILED = 417;
    public static final int SC_INSUFFICIENT_SPACE_ON_RESOURCE = 419;
    public static final int SC_METHOD_FAILURE = 420;
    public static final int SC_UNPROCESSABLE_ENTITY = 422;
    public static final int SC_LOCKED = 423;
    public static final int SC_FAILED_DEPENDENCY = 424;
    public static final int SC_INTERNAL_SERVER_ERROR = 500;
    public static final int SC_NOT_IMPLEMENTED = 501;
    public static final int SC_BAD_GATEWAY = 502;
    public static final int SC_SERVICE_UNAVAILABLE = 503;
    public static final int SC_GATEWAY_TIMEOUT = 504;
    public static final int SC_HTTP_VERSION_NOT_SUPPORTED = 505;
    public static final int SC_INSUFFICIENT_STORAGE = 507;

    static
    {
        String[] strings = new String[]{HEADER_ACCEPT, HEADER_ACCEPT_CHARSET, HEADER_ACCEPT_ENCODING,
            HEADER_ACCEPT_LANGUAGE, HEADER_AUTHORIZATION, HEADER_COOKIE, HEADER_EXPECT, HEADER_FROM,
            HEADER_HOST, HEADER_IF_MATCH, HEADER_IF_MODIFIED_SINCE, HEADER_IF_NONE_MATCH,
            HEADER_IF_RANGE, HEADER_IF_UNMODIFIED_SINCE, HEADER_MAX_FORWARDS, HEADER_PROXY_AUTHORIZATION,
            HEADER_RANGE, HEADER_REFERER, HEADER_TE, HEADER_USER_AGENT, HEADER_SLUG, HEADER_X_FORWARDED_FOR};

        REQUEST_HEADER_NAMES = Collections.unmodifiableMap(MapUtils.mapWithKeysAndValues(
            CaseInsensitiveMap.class, strings, strings));

        strings = new String[]{HEADER_ACCEPT_RANGES, HEADER_AGE, HEADER_CONTENT_DISPOSITION,
            HEADER_COOKIE_SET, HEADER_ETAG, HEADER_LOCATION, HEADER_PROXY_AUTHENTICATE,
            HEADER_RETRY_AFTER, HEADER_SERVER, HEADER_VARY, HEADER_WWW_AUTHENTICATE, HEADER_TRANSFER_ENCODING}; // TODO: remove HEADER_TRANSFER_ENCODING

        RESPONSE_HEADER_NAMES = Collections.unmodifiableMap(MapUtils.mapWithKeysAndValues(
            CaseInsensitiveMap.class, strings, strings));

        strings = new String[]{HEADER_ALLOW, HEADER_CACHE_CONTROL, HEADER_CONNECTION, HEADER_CONTENT_ENCODING,
            HEADER_CONTENT_LANGUAGE, HEADER_CONTENT_LENGTH, HEADER_CONTENT_LOCATION, HEADER_CONTENT_MD5,
            HEADER_CONTENT_RANGE, HEADER_CONTENT_TYPE, HEADER_DATE, HEADER_EXPIRES, HEADER_KEEP_ALIVE,
            HEADER_LAST_MODIFIED, HEADER_PRAGMA, HEADER_TRAILER, /*HEADER_TRANSFER_ENCODING, */ HEADER_UPGRADE,
            HEADER_VIA, HEADER_WARNING};

        GENERAL_AND_ENTITY_HEADER_NAMES = Collections.unmodifiableMap(MapUtils.mapWithKeysAndValues(
            CaseInsensitiveMap.class, strings, strings));

        Map<String, String> allHeaders = MapUtils.mapWithKeysAndValues(CaseInsensitiveMap.class, strings,
            strings);
        allHeaders.putAll(REQUEST_HEADER_NAMES);
        allHeaders.putAll(RESPONSE_HEADER_NAMES);
        ALL_HEADER_NAMES = Collections.unmodifiableMap(allHeaders);
    }

}
