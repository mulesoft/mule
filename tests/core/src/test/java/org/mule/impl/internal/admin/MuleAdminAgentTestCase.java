/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.admin;

import org.mule.MuleManager;
import org.mule.tck.AbstractMuleTestCase;

/**
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 * $Id$
 */
public class MuleAdminAgentTestCase extends AbstractMuleTestCase
{

    /**
     * Print the name of this test to standard output.
     */
    protected void doSetUp() throws Exception
    {
        getManager(false);
    }


    public void testEmptyServerUrl() throws Exception
    {
        final String serverEndpoint = "";
        MuleManager.getConfiguration().setServerUrl(serverEndpoint);
        MuleAdminAgent agent = new MuleAdminAgent();
        agent.initialise();

        // if it doesn't here fail, it has been registered
    }

    public void testNonEmptyServerUrl() throws Exception
    {
        final String serverEndpoint = "test://12345";
        MuleManager.getConfiguration().setServerUrl(serverEndpoint);
        MuleAdminAgent agent = new MuleAdminAgent();
        agent.initialise();

        // if it doesn't here fail, it has been registered
    }

}
