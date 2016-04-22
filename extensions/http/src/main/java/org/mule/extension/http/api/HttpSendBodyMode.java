/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api;

/**
 * Defines if the request should contain a body or not.
 */
public enum HttpSendBodyMode
{
    /**
     * Will send a body, regardless of the HTTP method selected.
     */
    ALWAYS,
    /**
     * Will send a body depending on the HTTP method selected (GET, HEAD and OPTIONS will not send a body).
     */
    AUTO,
    /**
     * Will not send a body, regardless of the HTTP method selected.
     */
    NEVER;
}
