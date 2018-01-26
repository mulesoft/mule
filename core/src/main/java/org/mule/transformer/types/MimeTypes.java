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
    String ANY = "*/*";

    String JSON = "text/json";
    String APPLICATION_JSON = "application/json";
    String ATOM = "application/atom+xml";
    String RSS = "application/rss+xml";
    String APPLICATION_XML = "application/xml";
    String XML = "text/xml";
    String TEXT = "text/plain";
    String HTML = "text/html";

    String BINARY = "application/octet-stream";
    String UNKNOWN = "content/unknown";
    String MULTIPART_MIXED="multipart/mixed";
    String MULTIPART_RELATED="multipart/related";
    String MULTIPART_X_MIXED_REPLACE="multipart/x-mixed-replace";

}
