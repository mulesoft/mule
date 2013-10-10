/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.annotations.param;

import org.mule.api.expression.RequiredValueException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.simple.ObjectToString;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class LookupInjectionTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testInject() throws Exception
    {
        LookupComponent component = new LookupComponent();

        muleContext.getRegistry().registerObject("transformer1", new ObjectToString());
        muleContext.getRegistry().registerObject("lookup", component);

        //Check that we got the transformers injected
        assertNotNull(component.getTransformer1());
        //optional
        assertNull(component.getTransformer2());
        assertNotNull(component.getTransformer3());

    }

    @Test
    public void testInjectFail() throws Exception
    {
        try
        {
            muleContext.getRegistry().registerObject("lookup", new LookupComponent());
            fail("Required object 'transformer1' not in the registry");
        }
        catch (RequiredValueException e)
        {
            //expected
        }


    }
}
