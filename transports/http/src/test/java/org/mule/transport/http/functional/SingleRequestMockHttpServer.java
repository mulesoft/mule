/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.functional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

public abstract class SingleRequestMockHttpServer extends MockHttpServer
{

    private CountDownLatch testCompleteLatch;

    public SingleRequestMockHttpServer(int listenPort, CountDownLatch startupLatch, CountDownLatch testCompleteLatch)
    {
        super(listenPort, startupLatch);
        this.testCompleteLatch = testCompleteLatch;
    }

    protected abstract void readHttpRequest(BufferedReader reader) throws Exception;

    @Override
    protected void processRequests(InputStream in, OutputStream out) throws Exception
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        readHttpRequest(reader);
        out.write("HTTP/1.1 200 OK\n\n".getBytes());
        out.flush();
        testCompleteLatch.countDown();
    }

}


