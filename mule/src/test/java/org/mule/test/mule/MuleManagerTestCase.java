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
package org.mule.test.mule;

import org.mule.management.agents.JmxAgent;
import org.mule.management.agents.Mx4jAgent;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.manager.UMOManager;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:aperepel@itci.com">Andrew Perepelytsya</a>
 * @version $Revision$
 */

public class MuleManagerTestCase extends AbstractMuleTestCase
{

    private UMOManager manager;

    /**
     * Print the name of this test to standard output
     */
    protected void doSetUp() throws Exception
    {
        manager = getManager();
    }

    public void testRemoveNonExistentAgent() throws Exception
    {
        manager.removeAgent("DOES_NOT_EXIST");

        // should not throw NPE
    }

    public void testAgentsRegistrationOrder() throws Exception
    {
        JmxAgent agentFirst = new JmxAgent();
        // If you specified "JmxAgent", it was the first one in the map,
        // but for "jmxAgent" the order was not preserved.
        // MX4JAgent depends on JmxAgent having finished initilisation
        // before proceeding, otherwise it is not able to find any
        // MBeanServer.
        agentFirst.setName("jmxAgent");
        manager.registerAgent(agentFirst);

        Mx4jAgent agentSecond = new Mx4jAgent();
        agentSecond.setName("mx4jAgent");
        manager.registerAgent(agentSecond);

        manager.start();

        // should not throw an exception
    }

}
