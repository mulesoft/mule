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
package org.mule.test.config;

import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.tck.NamedTestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AlwaysCreateConnectorTestCase extends NamedTestCase
{
    public void setUp() throws Exception
    {
        if (MuleManager.isInstanciated()) {
            MuleManager.getInstance().dispose();
        }
        System.setProperty(MuleProperties.DISABLE_SERVER_CONNECTIONS, "true");
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("always-create-connector-config.xml");
        System.setProperty(MuleProperties.DISABLE_SERVER_CONNECTIONS, "false");
    }

    public void testConnectorConfig() throws Exception
    {
        assertEquals(2, MuleManager.getInstance().getConnectors().size());
    }
}
