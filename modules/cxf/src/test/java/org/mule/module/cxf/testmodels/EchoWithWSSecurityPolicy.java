/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.testmodels;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.annotations.Policy;

@WebService
public class EchoWithWSSecurityPolicy
{
    @Resource
    private WebServiceContext context;
    
    @WebResult(name = "text")
    @WebMethod
    @Policy(uri="policy.xml")
    public String echo(@WebParam(name = "text") String s)
    {
        return s;
    }
}
