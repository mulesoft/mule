/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xstream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.module.xml.transformer.AbstractXStreamTransformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

public class XStreamConfigurationTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "xstream-transformer-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        AbstractXStreamTransformer transformer =
            (AbstractXStreamTransformer)muleContext.getRegistry().lookupTransformer("ObjectToXml");

        assertNotNull(transformer);
        assertNotNull(transformer.getAliases());
        assertEquals(Apple.class, transformer.getAliases().get("apple"));
        assertNotNull(transformer.getConverters());
        assertEquals(1, transformer.getConverters().size());
        assertTrue(transformer.getConverters().contains(DummyConverter.class));

        Apple apple = new Apple();
        apple.wash();
        Object result = transformer.transform(apple);

        assertEquals("<apple>\n  <bitten>false</bitten>\n  <washed>true</washed>\n</apple>", result);
    }
}
