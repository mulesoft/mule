/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

/**
 * Streaming mode for HTTP. Streaming in HTTP implies using Transfer-Encoding: chunked
 */
public enum HttpStreamingType
{
    /**
     * Will stream based on the message content.
     */
    AUTO,
    /**
     * Will always use streaming.
     */
    ALWAYS,
    /**
     * Will never use streaming.
     */
    NEVER;
}
