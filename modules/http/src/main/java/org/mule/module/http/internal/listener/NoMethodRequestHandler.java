/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.mule.module.http.api.HttpConstants.HttpStatus.METHOD_NOT_ALLOWED;

public class NoMethodRequestHandler extends ErrorRequestHandler
{

    private static NoMethodRequestHandler instance = new NoMethodRequestHandler();

    private NoMethodRequestHandler()
    {
        super(METHOD_NOT_ALLOWED.getStatusCode(), "Method not allowed for endpoint: %s", METHOD_NOT_ALLOWED.getReasonPhrase());
    }

    public static NoMethodRequestHandler getInstance()
    {
        return instance;
    }

}
