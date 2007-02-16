/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.MuleManager;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

public class EmbeddedBeansXmlTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/spring/test-embedded-spring-config.xml";
    }

    protected void doFunctionalSetUp() throws Exception
    {
        System.setProperty("org.mule.xml.validate", "false");
    }

    protected void doFunctionalTearDown() throws Exception
    {
        System.setProperty("org.mule.xml.validate", "true");
    }

    public void testContainer() throws Exception
    {
        UMOContainerContext context = MuleManager.getInstance().getContainerContext();
        assertNotNull(context);
        assertNotNull(context.getComponent("Apple"));
        assertNotNull(context.getComponent("Banana"));

        try
        {
            context.getComponent("Orange");
            fail("Object should  not found");
        }
        catch (ObjectNotFoundException e)
        {
            // ignore
        }
    }
}
