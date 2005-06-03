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

import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.tck.NamedTestCase;

import EDU.oswego.cs.dl.util.concurrent.CountDown;

/**
 * TODO: document this class
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class QuartzFunctionalTestCase extends NamedTestCase
{

    protected static CountDown countDown;

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

    public void test() throws Exception
    {
        countDown = new CountDown(3);
        ConfigurationBuilder configBuilder = new MuleXmlConfigurationBuilder();
        configBuilder.configure("quartz.xml");
        assertTrue(countDown.attempt(5000));
    }

}
