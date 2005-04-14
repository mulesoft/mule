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
package org.mule.extras.client;

import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleClientRemotingHttpTestCase extends MuleClientRemotingTestCase
{
    public void setUp() throws Exception
    {
        System.setProperty("org.mule.disable.server.connections", "false");
        if(MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("test-client-mule-config-remote-http.xml");
        System.setProperty("org.mule.disable.server.connections", "true");
    }

    public String getServerUrl()
    {
        return "http://localhost:60504";
    }
}
