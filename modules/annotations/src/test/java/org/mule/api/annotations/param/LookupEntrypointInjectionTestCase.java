/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
