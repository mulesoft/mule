/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api;

import org.mule.runtime.api.metadata.MediaType;

/**
 * Provides the constants for the standard HTTP header names and values
 */
public abstract class HttpHeaders {

  /**
   * Standard HTTP header names.
   */
  public static final class Names {

    /**
     * {@value "Accept"}
     */
    public static final String ACCEPT = "Accept";
    /**
     * {@value "Accept-Charset"}
     */
    public static final String ACCEPT_CHARSET = "Accept-Charset";
    /**
     * {@value "Accept-Encoding"}
     */
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    /**
     * {@value "Accept-Language"}
     */
    public static final String ACCEPT_LANGUAGE = "Accept-Language";
    /**
     * {@value "Accept-Ranges"}
     */
    public static final String ACCEPT_RANGES = "Accept-Ranges";
    /**
     * {@value "Accept-Patch"}
     */
    public static final String ACCEPT_PATCH = "Accept-Patch";
    /**
     * {@value "Access-Control-Allow-Credentials"}
     */
    public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    /**
     * {@value "Access-Control-Allow-Headers"}
     */
    public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    /**
     * {@value "Access-Control-Allow-Methods"}
     */
    public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    /**
     * {@value "Access-Control-Allow-Origin"}
     */
    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    /**
     * {@value "Access-Control-Expose-Headers"}
     */
    public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    /**
     * {@value "Access-Control-Max-Age"}
     */
    public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    /**
     * {@value "Access-Control-Request-Headers"}
     */
    public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
    /**
     * {@value "Access-Control-Request-Method"}
     */
    public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    /**
     * {@value "Age"}
     */
    public static final String AGE = "Age";
    /**
     * {@value "Allow"}
     */
    public static final String ALLOW = "Allow";
    /**
     * {@value "Authorization"}
     */
    public static final String AUTHORIZATION = "Authorization";
    /**
     * {@value "Cache-Control"}
     */
    public static final String CACHE_CONTROL = "Cache-Control";
    /**
     * {@value "Connection"}
     */
    public static final String CONNECTION = "Connection";
    /**
     * {@value "Content-Base"}
     */
    public static final String CONTENT_BASE = "Content-Base";
    /**
     * {@value "Content-Encoding"}
     */
    public static final String CONTENT_ENCODING = "Content-Encoding";
    /**
     * {@value "Content-Disposition"}
     */
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    /**
     * {@value "Content-Language"}
     */
    public static final String CONTENT_LANGUAGE = "Content-Language";
    /**
     * {@value "Content-Id"}
     */
    public static final String CONTENT_ID = "Content-Id";
    /**
     * {@value "Content-Length"}
     */
    public static final String CONTENT_LENGTH = "Content-Length";
    /**
     * {@value "Content-Location"}
     */
    public static final String CONTENT_LOCATION = "Content-Location";
    /**
     * {@value "Content-Transfer-Encoding"}
     */
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    /**
     * {@value "Content-MD5"}
     */
    public static final String CONTENT_MD5 = "Content-MD5";
    /**
     * {@value "Content-Range"}
     */
    public static final String CONTENT_RANGE = "Content-Range";
    /**
     * {@value "Content-Type"}
     */
    public static final String CONTENT_TYPE = "Content-Type";
    /**
     * {@value "Cookie"}
     */
    public static final String COOKIE = "Cookie";
    /**
     * {@value "Date"}
     */
    public static final String DATE = "Date";
    /**
     * {@value "ETag"}
     */
    public static final String ETAG = "ETag";
    /**
     * {@value "Expect"}
     */
    public static final String EXPECT = "Expect";
    /**
     * {@value "Expires"}
     */
    public static final String EXPIRES = "Expires";
    /**
     * {@value "From"}
     */
    public static final String FROM = "From";
    /**
     * {@value "Host"}
     */
    public static final String HOST = "Host";
    /**
     * {@value "If-Match"}
     */
    public static final String IF_MATCH = "If-Match";
    /**
     * {@value "If-Modified-Since"}
     */
    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
    /**
     * {@value "If-None-Match"}
     */
    public static final String IF_NONE_MATCH = "If-None-Match";
    /**
     * {@value "If-Range"}
     */
    public static final String IF_RANGE = "If-Range";
    /**
     * {@value "If-Unmodified-Since"}
     */
    public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
    /**
     * {@value "Last-Modified"}
     */
    public static final String LAST_MODIFIED = "Last-Modified";
    /**
     * {@value "Location"}
     */
    public static final String LOCATION = "Location";
    /**
     * {@value "Max-Forwards"}
     */
    public static final String MAX_FORWARDS = "Max-Forwards";
    /**
     * {@value "Origin"}
     */
    public static final String ORIGIN = "Origin";
    /**
     * {@value "Pragma"}
     */
    public static final String PRAGMA = "Pragma";
    /**
     * {@value "Proxy-Authenticate"}
     */
    public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
    /**
     * {@value "Proxy-Authorization"}
     */
    public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
    /**
     * {@value "Range"}
     */
    public static final String RANGE = "Range";
    /**
     * {@value "Referer"}
     */
    public static final String REFERER = "Referer";
    /**
     * {@value "Retry-After"}
     */
    public static final String RETRY_AFTER = "Retry-After";
    /**
     * {@value "Sec-WebSocket-Key1"}
     */
    public static final String SEC_WEBSOCKET_KEY1 = "Sec-WebSocket-Key1";
    /**
     * {@value "Sec-WebSocket-Key2"}
     */
    public static final String SEC_WEBSOCKET_KEY2 = "Sec-WebSocket-Key2";
    /**
     * {@value "Sec-WebSocket-Location"}
     */
    public static final String SEC_WEBSOCKET_LOCATION = "Sec-WebSocket-Location";
    /**
     * {@value "Sec-WebSocket-Origin"}
     */
    public static final String SEC_WEBSOCKET_ORIGIN = "Sec-WebSocket-Origin";
    /**
     * {@value "Sec-WebSocket-Protocol"}
     */
    public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
    /**
     * {@value "Sec-WebSocket-Version"}
     */
    public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";
    /**
     * {@value "Sec-WebSocket-Key"}
     */
    public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
    /**
     * {@value "Sec-WebSocket-Accept"}
     */
    public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";
    /**
     * {@value "Server"}
     */
    public static final String SERVER = "Server";
    /**
     * {@value "Set-Cookie"}
     */
    public static final String SET_COOKIE = "Set-Cookie";
    /**
     * {@value "Set-Cookie2"}
     */
    public static final String SET_COOKIE2 = "Set-Cookie2";
    /**
     * {@value "TE"}
     */
    public static final String TE = "TE";
    /**
     * {@value "Trailer"}
     */
    public static final String TRAILER = "Trailer";
    /**
     * {@value "Transfer-Encoding"}
     */
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    /**
     * {@value "Upgrade"}
     */
    public static final String UPGRADE = "Upgrade";
    /**
     * {@value "User-Agent"}
     */
    public static final String USER_AGENT = "User-Agent";
    /**
     * {@value "Vary"}
     */
    public static final String VARY = "Vary";
    /**
     * {@value "Via"}
     */
    public static final String VIA = "Via";
    /**
     * {@value "Warning"}
     */
    public static final String WARNING = "Warning";
    /**
     * {@value "WebSocket-Location"}
     */
    public static final String WEBSOCKET_LOCATION = "WebSocket-Location";
    /**
     * {@value "WebSocket-Origin"}
     */
    public static final String WEBSOCKET_ORIGIN = "WebSocket-Origin";
    /**
     * {@value "WebSocket-Protocol"}
     */
    public static final String WEBSOCKET_PROTOCOL = "WebSocket-Protocol";
    /**
     * {@value "WWW-Authenticate"}
     */
    public static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    /**
     * {@value "X-Forwarded-For"}
     */
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private Names() {}
  }

  /**
   * Standard HTTP header values.
   */
  public static final class Values {

    /**
     * {@value "application/x-www-form-urlencoded"}
     */
    public static final MediaType APPLICATION_X_WWW_FORM_URLENCODED = MediaType.create("application", "x-www-form-urlencoded");
    /**
     * {@value "base64"}
     */
    public static final String BASE64 = "base64";
    /**
     * {@value "binary"}
     */
    public static final String BINARY = "binary";
    /**
     * {@value "boundary"}
     */
    public static final String BOUNDARY = "boundary";
    /**
     * {@value "bytes"}
     */
    public static final String BYTES = "bytes";
    /**
     * {@value "charset"}
     */
    public static final String CHARSET = "charset";
    /**
     * {@value "chunked"}
     */
    public static final String CHUNKED = "chunked";
    /**
     * {@value "close"}
     */
    public static final String CLOSE = "close";
    /**
     * {@value "compress"}
     */
    public static final String COMPRESS = "compress";
    /**
     * {@value "100-continue"}
     */
    public static final String CONTINUE = "100-continue";
    /**
     * {@value "deflate"}
     */
    public static final String DEFLATE = "deflate";
    /**
     * {@value "gzip"}
     */
    public static final String GZIP = "gzip";
    /**
     * {@value "identity"}
     */
    public static final String IDENTITY = "identity";
    /**
     * {@value "keep-alive"}
     */
    public static final String KEEP_ALIVE = "keep-alive";
    /**
     * {@value "max-age"}
     */
    public static final String MAX_AGE = "max-age";
    /**
     * {@value "max-stale"}
     */
    public static final String MAX_STALE = "max-stale";
    /**
     * {@value "min-fresh"}
     */
    public static final String MIN_FRESH = "min-fresh";
    /**
     * {@value "multipart/form-data"}
     */
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    /**
     * {@value "must-revalidate"}
     */
    public static final String MUST_REVALIDATE = "must-revalidate";
    /**
     * {@value "no-cache"}
     */
    public static final String NO_CACHE = "no-cache";
    /**
     * {@value "no-store"}
     */
    public static final String NO_STORE = "no-store";
    /**
     * {@value "no-transform"}
     */
    public static final String NO_TRANSFORM = "no-transform";
    /**
     * {@value "none"}
     */
    public static final String NONE = "none";
    /**
     * {@value "only-if-cached"}
     */
    public static final String ONLY_IF_CACHED = "only-if-cached";
    /**
     * {@value "private"}
     */
    public static final String PRIVATE = "private";
    /**
     * {@value "proxy-revalidate"}
     */
    public static final String PROXY_REVALIDATE = "proxy-revalidate";
    /**
     * {@value "public"}
     */
    public static final String PUBLIC = "public";
    /**
     * {@value "quoted-printable"}
     */
    public static final String QUOTED_PRINTABLE = "quoted-printable";
    /**
     * {@value "s-maxage"}
     */
    public static final String S_MAXAGE = "s-maxage";
    /**
     * {@value "trailers"}
     */
    public static final String TRAILERS = "trailers";
    /**
     * {@value "Upgrade"}
     */
    public static final String UPGRADE = "Upgrade";
    /**
     * {@value "WebSocket"}
     */
    public static final String WEBSOCKET = "WebSocket";

    private Values() {}
  }

}
