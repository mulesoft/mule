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
package org.mule.extras.spring.config;

import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.tck.NamedTestCase;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EmbeddedBeansXmlTestCase extends NamedTestCase
{
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("org.mule.xml.validate", "false");
        if (MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        ConfigurationBuilder configBuilder = new MuleXmlConfigurationBuilder();
        configBuilder.configure("test-embedded-spring-config.xml");
    }

    protected void tearDown() throws Exception {
        System.setProperty("org.mule.xml.validate", "true");
        super.tearDown();
    }

    public void testContainer() throws Exception
    {
        UMOContainerContext context = MuleManager.getInstance().getContainerContext();
        assertNotNull(context);
        assertNotNull(context.getComponent("Apple"));
        assertNotNull(context.getComponent("Banana"));

        try {
            context.getComponent("Orange");
            fail("Object should  not found");
        } catch (ObjectNotFoundException e) {
            //ignore
        }
    }
}
