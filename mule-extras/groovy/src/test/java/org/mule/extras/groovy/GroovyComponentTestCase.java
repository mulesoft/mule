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
package org.mule.extras.groovy;

import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.tck.NamedTestCase;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GroovyComponentTestCase extends NamedTestCase
{
    public void setUp() throws Exception
    {
        super.setUp();
        if (MuleManager.isInstanciated())
            MuleManager.getInstance().dispose();
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure("groovy-mule-config.xml");
    }

    protected void tearDown() throws Exception
    {
        MuleManager.getInstance().dispose();
    }

    public void testFunctionBehaviour() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("vm://localhost/groovy.1", "Groovy Test: ", null);
        assertNotNull(m);
        assertEquals("Groovy Test: Received by component 1: Received by component 2:", m.getPayloadAsString());
    }
}
