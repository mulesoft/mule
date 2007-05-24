/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

public class JndiFunctionalTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/spring/jndi-functional-test.xml";
    }

    public void testJndi()
    {
        Object obj;
        
        obj = managementContext.getRegistry().lookupObject(new String("apple"));
        assertNotNull(obj);
        assertEquals(Apple.class, obj.getClass());

        obj = managementContext.getRegistry().lookupObject(new String("orange"));
        assertNotNull(obj);
        assertEquals(Orange.class, obj.getClass());
        assertEquals(new Integer(8), ((Orange) obj).getSegments());
        assertEquals("Florida Sunny", ((Orange) obj).getBrand());
    }
}


