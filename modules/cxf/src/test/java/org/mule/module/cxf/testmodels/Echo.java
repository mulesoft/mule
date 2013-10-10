/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.testmodels;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import org.junit.Assert;

@WebService
public class Echo
{
    @Resource
    private WebServiceContext context;
    
    @WebResult(name = "text")
    @WebMethod
    public String echo(@WebParam(name = "text") String s)
    {
        return s;
    }

    @WebResult(name = "output")
    @WebMethod
    public String ensureWebSerivceContextIsSet(@WebParam(name = "input") String input)
    {
        Assert.assertNotNull(context);
        return input;
    }
}
