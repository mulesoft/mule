/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

public abstract class SingleRequestMockHttpServer extends MockHttpServer
{

    private final CountDownLatch testCompleteLatch;

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
        out.write(HTTP_STATUS_LINE_OK.getBytes());
        out.write('\n');
        out.flush();
        testCompleteLatch.countDown();
    }

}


