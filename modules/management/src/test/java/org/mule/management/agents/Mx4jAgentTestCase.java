/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.agents;

import org.mule.management.AbstractMuleJmxTestCase;

import mx4j.tools.adaptor.http.HttpAdaptor;

/**
 * Test that the HttpAdaptor lifecycle is properly managed.
 */
public class Mx4jAgentTestCase extends AbstractMuleJmxTestCase
{
    public void testRedeploy() throws Exception
    {
        managementContext.setId("Mx4jAgentTest");
        final String name = jmxSupport.getDomainName(managementContext) +
                            ":" + Mx4jAgent.HTTP_ADAPTER_OBJECT_NAME;
        mBeanServer.registerMBean(new HttpAdaptor(), jmxSupport.getObjectName(name));

        Mx4jAgent agent = new Mx4jAgent();
        agent.setManagementContext(managementContext);
        agent.initialise();
    }
}
