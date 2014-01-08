/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

@WebService
public class Echo
{
    @WebResult(name = "text")
    @WebMethod(action = "echo")
    public String echo(@WebParam(name = "text") String s)
    {
        return s;
    }

    @WebResult(name = "text")
    @WebMethod(action = "echo")
    public String fail(@WebParam(name = "text") String s) throws IllegalArgumentException
    {
        throw new IllegalArgumentException(s);
    }
}
