/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.agents;

import mx4j.tools.adaptor.http.HttpAdaptor;
import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.management.support.JmxModernSupport;
import org.mule.management.support.JmxSupport;

import javax.management.ObjectName;

/**
 * Test that the HttpAdaptor lifecycle is properly managed.
 */
public class Mx4jAgentTestCase extends AbstractMuleJmxTestCase
{
    public void testRedeploy() throws Exception
    {
        final String name = JmxSupport.DEFAULT_JMX_DOMAIN_PREFIX +
                                  ".Mx4jAgentTest:" + Mx4jAgent.HTTP_ADAPTER_OBJECT_NAME;
        mBeanServer.registerMBean(new HttpAdaptor(), ObjectName.getInstance(name));

        Mx4jAgent agent = new Mx4jAgent();
        agent.initialise();
    }
}
