/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformers.xml.xstream;

import org.mule.module.xml.transformer.AbstractXStreamTransformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XStreamConfigurationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
