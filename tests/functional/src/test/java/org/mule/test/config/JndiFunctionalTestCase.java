/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JndiFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/spring/jndi-functional-test.xml";
    }

    @Test
    public void testJndi()
    {
        Object obj;
        
        obj = muleContext.getRegistry().lookupObject(new String("apple"));
        assertNotNull(obj);
        assertEquals(Apple.class, obj.getClass());

        obj = muleContext.getRegistry().lookupObject(new String("orange"));
        assertNotNull(obj);
        assertEquals(Orange.class, obj.getClass());
        assertEquals(new Integer(8), ((Orange) obj).getSegments());
        assertEquals("Florida Sunny", ((Orange) obj).getBrand());
    }
}


