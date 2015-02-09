/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

/**
 * Request handle for request calls to paths with no listener configured.
 */
public class NoListenerRequestHandler extends NoMatchingListenerRequestHandler
{

    public static final int RESOURCE_NOT_FOUND_STATUS_CODE = 404;
    public static final String RESOURCE_NOT_FOUND = "Resource not found.";

    private static NoListenerRequestHandler instance = new NoListenerRequestHandler();

    private NoListenerRequestHandler()
    {
        super(RESOURCE_NOT_FOUND_STATUS_CODE, "No listener for endpoint: %s", RESOURCE_NOT_FOUND);
    }

    public static NoListenerRequestHandler getInstance()
    {
        return instance;
    }

}
