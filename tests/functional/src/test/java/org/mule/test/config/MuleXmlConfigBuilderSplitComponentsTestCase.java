/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.model.UMOModel;

/**
 * Test for MULE-858
 */
public class MuleXmlConfigBuilderSplitComponentsTestCase extends FunctionalTestCase
{

    public MuleXmlConfigBuilderSplitComponentsTestCase()
    {
        super();
        setDisposeManagerPerSuite(true);
    }

    public String getConfigResources()
    {
        return "split-components-1.xml, split-components-2.xml, split-components-3.xml";
    }

    /**
     * Make sure all the components from all the config files have been created.
     */
    public void testSplitComponentsConfig() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");
        assertNotNull(model);
        assertNotNull(model.getComponent("Component1"));
        assertNotNull(model.getComponent("Component2"));
        assertNotNull(model.getComponent("Component3"));
        assertNotNull(model.getComponent("Component4"));
        assertNotNull(model.getComponent("Component5"));
    }
}
