/*
 * $Id:MultipleSpringContextsTestCase.java 5187 2007-02-16 18:00:42Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.tck.FunctionalTestCase;
import org.mule.umo.manager.UMOContainerContext;

public class MultipleSpringContextsTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/spring/multiple-spring-contexts-mule.xml";
    }

    public void testMultiptleSpringContexts() throws Exception
    {
        UMOContainerContext context =managementContext.getRegistry().getContainerContext();
        assertNotNull(context);
        Object bowl1 = context.getComponent("org.mule.tck.testmodels.fruit.FruitBowl");
        assertNotNull(bowl1);
        Object bowl2 = context.getComponent("org.mule.tck.testmodels.fruit.FruitBowl2");
        assertNotNull(bowl2);
    }
}
