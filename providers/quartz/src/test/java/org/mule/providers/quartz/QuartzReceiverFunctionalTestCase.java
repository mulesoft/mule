/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.quartz;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.tck.NamedTestCase;
import org.mule.util.concurrent.CountDownLatch;

/**
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class QuartzReceiverFunctionalTestCase extends NamedTestCase
{

    protected static CountDownLatch countDown;

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        if (MuleManager.isInstanciated()) {
            MuleManager.getInstance().dispose();
        }
        super.tearDown();
    }

    public void testMuleReceiverJob() throws Exception
    {
        countDown = new CountDownLatch(3);
        ConfigurationBuilder configBuilder = new MuleXmlConfigurationBuilder();
        configBuilder.configure("quartz-receive.xml");
        assertTrue(countDown.tryLock(5000, TimeUnit.MILLISECONDS));
    }
}
