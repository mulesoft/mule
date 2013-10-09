/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformers.simple;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.api.transformer.Transformer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransformerArraysTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "transformer-arrays-config.xml";
    }

    @Test
    public void testArrayReturnType() throws Exception
    {

        Transformer trans = muleContext.getRegistry().lookupTransformer("testTrans");

        assertNotNull(trans);
        assertEquals(Orange[].class, trans.getReturnClass());
    }
}
