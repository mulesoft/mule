/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
