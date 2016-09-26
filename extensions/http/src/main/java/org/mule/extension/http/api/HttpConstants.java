/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

import static org.mule.runtime.module.http.api.HttpConstants.RequestProperties.HTTP_PREFIX;

/**
 * General purpose HTTP constants
 */
public abstract class HttpConstants {

  public enum Methods {
    POST, GET, PUT, OPTIONS, HEAD, DELETE;
  }

  public static enum Protocols {
    HTTP("http", 80), HTTPS("https", 443);

    private final String scheme;
    private final int defaultPort;

    Protocols(String scheme, int defaultPort) {
      this.scheme = scheme;
      this.defaultPort = defaultPort;
    }

    public String getScheme() {
      return scheme;
    }

    public int getDefaultPort() {
      return defaultPort;
    }
  }

  public static final class RequestProperties {

    public static final String HTTP_PREFIX = "http.";
    public static final String HTTP_STATUS_PROPERTY = HTTP_PREFIX + "status";
    public static final String HTTP_VERSION_PROPERTY = HTTP_PREFIX + "version";
    public static final String HTTP_QUERY_PARAMS = HTTP_PREFIX + "query.params";
    public static final String HTTP_URI_PARAMS = HTTP_PREFIX + "uri.params";
    public static final String HTTP_QUERY_STRING = HTTP_PREFIX + "query.string";
    public static final String HTTP_METHOD_PROPERTY = HTTP_PREFIX + "method";
    public static final String HTTP_RELATIVE_PATH = HTTP_PREFIX + "relative.path";
    public static final String HTTP_REQUEST_PROPERTY = HTTP_PREFIX + "request";
    public static final String HTTP_REQUEST_PATH_PROPERTY = HTTP_PREFIX + "request.path";
    public static final String HTTP_CONTEXT_PATH_PROPERTY = HTTP_PREFIX + "context.path";
    public static final String HTTP_REQUEST_URI = HTTP_PREFIX + "request.uri";
    public static final String HTTP_REMOTE_ADDRESS = HTTP_PREFIX + "remote.address";
    public static final String HTTP_LISTENER_PATH = HTTP_PREFIX + "listener.path";
    public static final String HTTP_SCHEME = HTTP_PREFIX + "scheme";
    public static final String HTTP_CLIENT_CERTIFICATE = HTTP_PREFIX + "client.cert";
    public static final String HTTP_DISABLE_STATUS_CODE_EXCEPTION_CHECK = HTTP_PREFIX + "disable.status.code.exception.check";
  }

  public static final class ResponseProperties {

    public static final String HTTP_STATUS_PROPERTY = RequestProperties.HTTP_STATUS_PROPERTY;
    public static final String HTTP_REASON_PROPERTY = HTTP_PREFIX + "reason";
  }

  public static final String ALL_INTERFACES_IP = "0.0.0.0";

  public enum HttpStatus {
    CONTINUE(100, "Continue"), SWITCHING_PROTOCOLS(101, "Switching Protocols"), PROCESSING(102, "Processing"), OK(200,
        "OK"), CREATED(201, "Created"), ACCEPTED(202, "Accepted"), NON_AUTHORITATIVE_INFORMATION(203,
            "Non Authoritative Information"), NO_CONTENT(204, "No Content"), RESET_CONTENT(205, "Reset Content"), PARTIAL_CONTENT(
                206, "Partial Content"), MULTI_STATUS(207, "Multi-Status"), MULTIPLE_CHOICES(300,
                    "Multiple Choices"), MOVED_PERMANENTLY(301, "Moved Permanently"), MOVED_TEMPORARILY(302,
                        "Moved Temporarily"), SEE_OTHER(303, "See Other"), NOT_MODIFIED(304, "Not Modified"), USE_PROXY(305,
                            "Use Proxy"), TEMPORARY_REDIRECT(307, "Temporary Redirect"), BAD_REQUEST(400,
                                "Bad Request"), UNAUTHORIZED(401, "Unauthorized"), PAYMENT_REQUIRED(402,
                                    "Payment Required"), FORBIDDEN(403, "Forbidden"), NOT_FOUND(404,
                                        "Not Found"), METHOD_NOT_ALLOWED(405, "Method Not Allowed"), NOT_ACCEPTABLE(406,
                                            "Not Acceptable"), PROXY_AUTHENTICATION_REQUIRED(407,
                                                "Proxy Authentication Required"), REQUEST_TIMEOUT(408,
                                                    "Request Timeout"), CONFLICT(409, "Conflict"), GONE(410,
                                                        "Gone"), LENGTH_REQUIRED(411, "Length Required"), PRECONDITION_FAILED(412,
                                                            "Precondition Failed"), REQUEST_TOO_LONG(413,
                                                                "Request Entity Too Large"), REQUEST_URI_TOO_LONG(414,
                                                                    "Request-URI Too Long"), UNSUPPORTED_MEDIA_TYPE(415,
                                                                        "Unsupported Media Type"), REQUESTED_RANGE_NOT_SATISFIABLE(
                                                                            416,
                                                                            "Requested Range Not Satisfiable"), EXPECTATION_FAILED(
                                                                                417, "Expectation Failed"), INTERNAL_SERVER_ERROR(
                                                                                    500, "Server Error"), NOT_IMPLEMENTED(501,
                                                                                        "Not Implemented"), BAD_GATEWAY(502,
                                                                                            "Bad Gateway"), SERVICE_UNAVAILABLE(
                                                                                                503,
                                                                                                "Service Unavailable"), GATEWAY_TIMEOUT(
                                                                                                    504,
                                                                                                    "Gateway Timeout"), HTTP_VERSION_NOT_SUPPORTED(
                                                                                                        505,
                                                                                                        "HTTP Version Not Supported");

    private int statusCode;
    private String reasonPhrase;

    HttpStatus(int statusCode, String reasonPhrase) {
      this.statusCode = statusCode;
      this.reasonPhrase = reasonPhrase;
    }

    public int getStatusCode() {
      return statusCode;
    }

    public String getReasonPhrase() {
      return reasonPhrase;
    }

    public static String getReasonPhraseForStatusCode(int statusCode) {
      final HttpStatus statusByCode = getStatusByCode(statusCode);
      return statusByCode == null ? null : statusByCode.getReasonPhrase();
    }

    public static HttpStatus getStatusByCode(int statusCode) {
      for (HttpStatus httpStatus : HttpStatus.values()) {
        if (httpStatus.getStatusCode() == statusCode) {
          return httpStatus;
        }
      }

      return null;
    }
  }
}
