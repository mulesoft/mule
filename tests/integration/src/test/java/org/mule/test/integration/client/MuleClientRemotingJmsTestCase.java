/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.client;

import org.mule.MuleManager;
import org.mule.extras.client.MuleClientRemotingTestCase;
import org.mule.config.builders.MuleXmlConfigurationBuilder;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class MuleClientRemotingJmsTestCase extends MuleClientRemotingTestCase
{
    public void setUp() throws Exception
    {
        System.setProperty("org.mule.disable.server.connections", "false");       
        if(MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("org/mule/test/integration/client/test-client-mule-config-remote-jms.xml");
    }

    public String getServerUrl()
    {
        return "jms://jmsSysProvider/mule.sys.queue?createConnector=NEVER";
    }
}
