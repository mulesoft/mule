/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.types;

/**
 * Common mime types used in Mule
 *
 * @since 3.0
 */
public interface MimeTypes
{
    public static final String ANY = "*/*";

    public static final String JSON = "text/json";
    public static final String APPLICATION_JSON = "application/json";
    public static final String ATOM = "application/atom+xml";
    public static final String RSS = "application/rss+xml";
    public static final String APPLICATION_XML = "application/xml";
    public static final String XML = "text/xml";
    public static final String TEXT = "text/plain";
    public static final String HTML = "text/html";

    public static final String BINARY = "application/octet-stream";
    public static final String UNKNOWN = "content/unknown";
    public static final String MULTIPART_MIXED="multipart/mixed";
    public static final String MULTIPART_RELATED="multipart/related";
    public static final String MULTIPART_X_MIXED_REPLACE="multipart/x-mixed-replace";

}
