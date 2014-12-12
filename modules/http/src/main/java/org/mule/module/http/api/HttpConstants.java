/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api;

/**
 * General purpose HTTP constants
 */
public abstract class HttpConstants
{

    public enum Methods
    {
        POST, GET, PUT, OPTIONS, HEAD, DELETE;
    }

    public static final class Protocols
    {

        public static final String HTTP = "http";
        public static final String HTTPS = "https";
    }

    public static final class RequestProperties
    {
        public static final String HTTP_PREFIX = "http.";
        public static final String HTTP_STATUS_PROPERTY = HTTP_PREFIX + "status";
        public static final String HTTP_VERSION_PROPERTY = HTTP_PREFIX + "version";
        public static final String HTTP_QUERY_PARAMS = HTTP_PREFIX + "query.params";
        public static final String HTTP_URI_PARAMS = HTTP_PREFIX + "uri.params";
        public static final String HTTP_QUERY_STRING = HTTP_PREFIX + "query.string";
        public static final String HTTP_METHOD_PROPERTY = HTTP_PREFIX + "method";
        public static final String HTTP_REQUEST_PATH_PROPERTY = HTTP_PREFIX + "request.path";
        public static final String HTTP_REQUEST_URI = HTTP_PREFIX + "request.uri";
        public static final String HTTP_REMOTE_ADDRESS = HTTP_PREFIX + "remote.address";
        public static final String HTTP_LISTENER_PATH = HTTP_PREFIX + "listener.path";
        public static final String HTTP_SCHEME = HTTP_PREFIX + "scheme";
    }

    public static final class ResponseProperties
    {
        public static final String HTTP_STATUS_PROPERTY = RequestProperties.HTTP_STATUS_PROPERTY;
    }

    public static final String ALL_INTERFACES_IP = "0.0.0.0";
    public static final String COOKIE = "Cookie";
}
