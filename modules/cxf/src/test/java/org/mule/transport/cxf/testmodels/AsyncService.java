/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.testmodels;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

@WebService
public class AsyncService
{

    private CountDownLatch latch = new CountDownLatch(1);

    @WebMethod
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
