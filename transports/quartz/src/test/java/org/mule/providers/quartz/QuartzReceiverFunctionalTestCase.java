/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.tck.AbstractMuleTestCase;

public class QuartzReceiverFunctionalTestCase extends AbstractMuleTestCase
{
    protected static volatile CountDownLatch countDown;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        countDown = new CountDownLatch(3);
    }

    protected void doTearDown() throws Exception
    {
        countDown = null;
        super.doTearDown();
    }

    public void testMuleReceiverJob() throws Exception
    {
        ConfigurationBuilder configBuilder = new MuleXmlConfigurationBuilder();
        configBuilder.configure("quartz-receive.xml");
        if (!countDown.await(5000, TimeUnit.MILLISECONDS))
        {
            fail("CountDown failed: expected 0, value is: " + countDown.getCount());
        }
    }

}
