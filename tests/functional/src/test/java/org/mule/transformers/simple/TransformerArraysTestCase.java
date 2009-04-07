/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.api.transformer.Transformer;

public class TransformerArraysTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "transformer-arrays-config.xml";
    }

    public void testArrayReturnType() throws Exception
    {

        Transformer trans = muleContext.getRegistry().lookupTransformer("testTrans");

        assertNotNull(trans);
        assertEquals(Orange[].class, trans.getReturnClass());
    }
}
