/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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

    public static final String JSON = "application/json";
    public static final String ATOM = "application/atom+xml";
    public static final String RSS = "application/rss+xml";
    public static final String XML = "text/xml";
    public static final String TEXT = "text/plain";
    public static final String HTML = "text/html";
    public static final String BINARY = "application/octet-stream";

}
