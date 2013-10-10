/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    public static final String JSON = "application/json";
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
