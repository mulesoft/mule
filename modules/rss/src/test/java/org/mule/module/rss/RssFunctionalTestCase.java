/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class RssFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "rss-test-config.xml";
    }

    @Test
    public void testRSS() throws Exception
    {
        if (isOffline(getClass().getName() + ".testRSS"))
        {
            return;
        }

        //lets wait to read the feed
        Thread.sleep(RECEIVE_TIMEOUT);
        EntryReceiver component = (EntryReceiver) getComponent("rssTester");
        assertTrue(component.getCount() > 2);

        EntryReceiver component2 = (EntryReceiver) getComponent("rssTester2");
        assertTrue(component2.getCount() > 2);

        assertTrue(component.getCount() >= component2.getCount());
    }
}
