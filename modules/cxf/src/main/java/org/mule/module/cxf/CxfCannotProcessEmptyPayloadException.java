/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

public class CxfCannotProcessEmptyPayloadException extends Exception
{
    private int responseCode;

    public CxfCannotProcessEmptyPayloadException(Integer responseStatusCode)
    {
        super("Invoked service responded with status code '" + responseStatusCode.toString() + "' and an empty body");
        responseCode = responseStatusCode;
    }
}
