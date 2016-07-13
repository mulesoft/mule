/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.transformers.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class TransformerArraysTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "transformer-arrays-config.xml";
    }

    @Test
    public void testArrayReturnType() throws Exception
    {

        Transformer trans = muleContext.getRegistry().lookupTransformer("testTrans");

        assertNotNull(trans);
        assertEquals(Orange[].class, trans.getReturnDataType().getType());
    }
}
