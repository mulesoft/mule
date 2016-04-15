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
import javax.xml.ws.Holder;

/**
 * Web service used by WS Consumer tests.
 */
@WebService(portName = "TestPort", serviceName = "TestService")
public class TestService
{
    @WebResult(name = "text")
    @WebMethod(action = "echo")
    public String echo(@WebParam(name = "text") String s)
    {
        return s;
    }

    @WebResult(name = "text")
    @WebMethod(action = "fail")
    public String fail(@WebParam(name = "text") String s) throws EchoException
    {
        throw new EchoException(s);
    }

    @WebResult(name = "text")
    @WebMethod(action = "echoWithHeaders")
    public String echoWithHeaders(@WebParam(name = "headerIn", header = true, mode = WebParam.Mode.IN) String headerIn,
                                  @WebParam(name = "headerOut", header = true, mode = WebParam.Mode.OUT) Holder<String> headerOut,
                                  @WebParam(name = "headerInOut", header = true, mode = WebParam.Mode.INOUT) Holder<String> headerInOut,
                                  @WebParam(name = "text") String s)
    {
        headerOut.value = headerIn + " OUT";
        headerInOut.value = headerInOut.value + " INOUT";
        return s;
    }

    @WebResult(name = "text")
    @WebMethod(action = "noParams")
    public String noParams()
    {
        return "TEST";
    }

    @WebResult(name = "text")
    @WebMethod(action = "noParams")
    public String noParamsWithHeader(@WebParam(name = "header", header = true, mode = WebParam.Mode.IN) String header)
    {
        return header;
    }


}
