/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.config;

import org.mule.tck.FunctionalTestCase;
import org.mule.MuleManager;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.manager.UMOContainerContext;

/**
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 */
public class MultipleSpringContextsTestCase extends FunctionalTestCase {

    protected String getConfigResources () {
        return "multiple-spring-contexts-mule.xml";
    }

    public void testMultiptleSpringContexts() throws Exception {
        // initialize the manager
        UMOManager manager = MuleManager.getInstance();

        UMOContainerContext context = manager.getContainerContext();
        assertNotNull(context);
        Object bowl1 = context.getComponent("org.mule.tck.testmodels.fruit.FruitBowl");
        assertNotNull(bowl1);
        Object bowl2 = context.getComponent("org.mule.tck.testmodels.fruit.FruitBowl2");
        assertNotNull(bowl2);
    }
}
