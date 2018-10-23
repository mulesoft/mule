/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.mule.module.http.api.HttpConstants.HttpStatus.BAD_REQUEST;

/**
 * Request handle for request calls to a malformed path.
 */
public class MalformedUrlRequestHandler extends ErrorRequestHandler
{

    public static final String NON_DECODABLE_URL_ENTITY_FORMAT = "Unable to resolve malformed url: %s";
    private static MalformedUrlRequestHandler instance = new MalformedUrlRequestHandler();

    private MalformedUrlRequestHandler()
    {
        super(BAD_REQUEST.getStatusCode(), BAD_REQUEST.getReasonPhrase(), NON_DECODABLE_URL_ENTITY_FORMAT);
    }

    public static MalformedUrlRequestHandler getInstance()
    {
        return instance;
    }

}
