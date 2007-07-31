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

import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.FruitBowl;

public class MultipleSpringContextsTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "multiple-spring-contexts-mule.xml";
    }

    public void testMultiptleSpringContexts() throws Exception
    {
        Object bowl1 = AbstractMuleTestCase.managementContext.getRegistry().lookupObject("org.mule.tck.testmodels.fruit.FruitBowl", FruitBowl.class);
        assertNotNull(bowl1);
        Object bowl2 = AbstractMuleTestCase.managementContext.getRegistry().lookupObject("org.mule.tck.testmodels.fruit.FruitBowl2", FruitBowl.class);
        assertNotNull(bowl2);
    }
}
