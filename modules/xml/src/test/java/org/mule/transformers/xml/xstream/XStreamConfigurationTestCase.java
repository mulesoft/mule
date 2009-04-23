/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xstream;

import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.module.xml.transformer.AbstractXStreamTransformer;
import org.mule.transformers.xml.xstream.DummyConverter;

import com.thoughtworks.xstream.converters.basic.ByteConverter;

public class XStreamConfigurationTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "xstream-transformer-config.xml";
    }

    public void testConfig() throws Exception
    {
        AbstractXStreamTransformer trans = (AbstractXStreamTransformer)muleContext.getRegistry().lookupTransformer("ObjectToXml");

        assertNotNull(trans);
        assertNotNull(trans.getAliases());
        assertEquals(Apple.class, trans.getAliases().get("apple"));
        assertNotNull(trans.getConverters());
        assertEquals(1, trans.getConverters().size());
        assertTrue(trans.getConverters().contains(DummyConverter.class));

        Apple apple = new Apple();
        apple.wash();
        Object result = trans.transform(apple);

        assertEquals("<apple>\n  <bitten>false</bitten>\n  <washed>true</washed>\n</apple>", result);


    }

}
