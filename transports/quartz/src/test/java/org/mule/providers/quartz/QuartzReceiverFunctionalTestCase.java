/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.tck.AbstractMuleTestCase;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class QuartzReceiverFunctionalTestCase extends AbstractMuleTestCase
{

    public QuartzReceiverFunctionalTestCase()
    {
        super();
        this.setDisposeManagerPerSuite(true);
    }

    public void testMuleReceiverJob() throws Exception
    {
        CountDownLatch counter = TestComponent.QuartzCounter;
        assertEquals(3, counter.getCount());

        ConfigurationBuilder configBuilder = new MuleXmlConfigurationBuilder();
        configBuilder.configure("quartz-receive.xml");

        // we wait up to 60 seconds here which is WAY too long for three ticks with 1
        // second interval, but it seems that "sometimes" it takes a very long time
        // for Quartz go kick in. Once it starts ticking everything is fine.
        if (!counter.await(60, TimeUnit.SECONDS))
        {
            fail("CountDown timed out: expected 0, value is: " + counter.getCount());
        }
    }

}
