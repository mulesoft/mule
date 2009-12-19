/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import org.mule.tck.FunctionalTestCase;

public class RssFunctionalTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "rss-test-config.xml";
    }

    public void testRSS() throws Exception
    {
        //lets wait to read the feed
        Thread.sleep(5000);
        RssEntryCounterComponent component = (RssEntryCounterComponent) this.getComponent("rssTester");
        assertTrue(component.getCount() > 2);

        RssEntryCounterComponent component2 = (RssEntryCounterComponent) this.getComponent("rssTester2");
        assertTrue(component2.getCount() > 2);

        assertTrue(component.getCount() >= component2.getCount());

    }
}
