/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.Arrays;

public class CorrelationResequencerTestCase extends FunctionalTestCase
{

    private int timeout = getTimeoutSecs() / 20 * 1000;

    protected String getConfigResources()
    {
        return "correlation-resequencer-test.xml";
    }

    public void testResequencer() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://splitter", Arrays.asList("a", "b", "c", "d", "e", "f"), null);

        FunctionalTestComponent resequencer = getFunctionalTestComponent("test validator");

        Thread.sleep(timeout);

        assertEquals("Wrong number of messages received.", 6, resequencer.getReceivedMessagesCount());
        assertEquals("Sequence wasn't reordered.", "a", resequencer.getReceivedMessage(1));
        assertEquals("Sequence wasn't reordered.", "b", resequencer.getReceivedMessage(2));
        assertEquals("Sequence wasn't reordered.", "c", resequencer.getReceivedMessage(3));
        assertEquals("Sequence wasn't reordered.", "d", resequencer.getReceivedMessage(4));
        assertEquals("Sequence wasn't reordered.", "e", resequencer.getReceivedMessage(5));
        assertEquals("Sequence wasn't reordered.", "f", resequencer.getReceivedMessage(6));
    }
}
