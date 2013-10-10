/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.testmodels;

import java.util.concurrent.CountDownLatch;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public class AsyncServiceWithSoapAction
{

    private CountDownLatch latch = new CountDownLatch(1);

    @WebMethod(action="send")
    @Oneway
    public void send(@WebParam(name = "text") String s)
    {
        latch.countDown();
    }

    @WebMethod(exclude = true)
    public CountDownLatch getLatch()
    {
        return latch;
    }
}
