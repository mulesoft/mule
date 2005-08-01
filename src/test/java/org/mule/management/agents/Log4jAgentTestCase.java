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
package org.mule.management.agents;

import org.apache.log4j.jmx.HierarchyDynamicMBean;
import org.mule.tck.AbstractMuleJmxTestCase;

import javax.management.ObjectName;

/**
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 * 
 * $Id$
 */
public class Log4jAgentTestCase extends AbstractMuleJmxTestCase
{
    public void testRedeploy() throws Exception
    {
        mBeanServer.registerMBean(new HierarchyDynamicMBean(), ObjectName.getInstance(Log4jAgent.JMX_OBJECT_NAME));

        Log4jAgent agent = new Log4jAgent();
        agent.initialise();
    }
}
