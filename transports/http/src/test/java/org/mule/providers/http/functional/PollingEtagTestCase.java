/*
 * $Id: PollingReceiversRestartTestCase.java 8077 2007-08-27 20:15:25Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.http.functional;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;

public class PollingEtagTestCase extends FunctionalTestCase
{
    private static final int WAIT_TIME = 2500;

    public PollingEtagTestCase()
    {
    }

    protected String getConfigResources()
    {
        return "polling-etag-test.xml";
    }

    public void testPollingReceiversRestart() throws Exception
    {
        FunctionalTestComponent ftc = lookupTestComponent("main","Test");

        AtomicInteger pollCounter = new AtomicInteger(0);
        ftc.setEventCallback(new CounterCallback(pollCounter));

        // should be enough to poll for multiple messages
        Thread.sleep(WAIT_TIME);

        assertEquals(1, pollCounter.get());
    }


}

