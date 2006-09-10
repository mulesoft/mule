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

    public void testMuleReceiverJob() throws Exception
    {
        ConfigurationBuilder configBuilder = new MuleXmlConfigurationBuilder();
        configBuilder.configure("quartz-receive.xml");

        CountDownLatch counter = TestComponent.getQuartzCounter();
        if (!counter.await(5000, TimeUnit.MILLISECONDS))
        {
            fail("CountDown failed: expected 0, value is: " + counter.getCount());
        }
    }

}
