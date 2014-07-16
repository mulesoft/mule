/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.param;

import org.mule.api.model.InvocationResult;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.RedApple;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LookupEntrypointInjectionTestCase extends AbstractAnnotatedEntrypointResolverTestCase
{

    @Override
    protected Object getComponent()
    {
        return new LookupComponent();
    }

    @Test
    public void testLookups() throws Exception
    {
        RedApple redApple = new RedApple();
        redApple.wash();

        muleContext.getRegistry().registerObject("redApple", redApple);
        muleContext.getRegistry().registerObject("anotherRedApple", new RedApple());
        muleContext.getRegistry().registerObject("aBanana", new Banana());

        InvocationResult response = invokeResolver("listFruit", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        List<?> result = (List<?>) response.getResult();
        assertEquals(2, result.size());

        assertEquals(redApple, result.get(1));

        //Add optional Orange
        muleContext.getRegistry().registerObject("orange", new Orange());
        response = invokeResolver("listFruit", eventContext);
        assertTrue("Message payload should be a List", response.getResult() instanceof List);
        result = (List<?>) response.getResult();
        //We now have an orange
        assertEquals(3, result.size());

        //Remove required object
        muleContext.getRegistry().unregisterObject("redApple");

        try
        {
            invokeResolver("listFruit", eventContext);
            fail("redApple is a required property but not in the registry");
        }
        catch (Exception e)
        {
            //expected
        }
    }
}
