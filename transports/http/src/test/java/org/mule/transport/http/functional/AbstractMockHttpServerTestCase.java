/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.tck.FunctionalTestCase;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public abstract class AbstractMockHttpServerTestCase extends FunctionalTestCase
{
    private CountDownLatch serverStartLatch = new CountDownLatch(1);

    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        MockHttpServer httpServer = getHttpServer(serverStartLatch);
        new Thread(httpServer).start();

        // wait for the simple server thread to come up
        assertTrue(serverStartLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    /**
     * Subclasses must implement this method to return their Subclass of {@link MockHttpServer}.
     */
    protected abstract MockHttpServer getHttpServer(CountDownLatch serverStartLatch);
}
