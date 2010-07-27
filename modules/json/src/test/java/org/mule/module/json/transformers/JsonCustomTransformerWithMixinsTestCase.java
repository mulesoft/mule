/*
 * $Id: JsonCustomTransformerWithMixinsTestCase.java 302 2010-02-17 07:57:47Z ross $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.json.JsonData;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformer.types.DataTypeFactory;

public class JsonCustomTransformerWithMixinsTestCase extends AbstractMuleTestCase
{
    public static final String APPLE_JSON = "{\"washed\":false,\"bitten\":true}";


    @Override
    protected void doSetUp() throws Exception
    {
        muleContext.getRegistry().registerObject("trans", new JsonCustomTransformerWithMixins());
    }

    public void testCustomTransform() throws Exception
    {
        //THough the data is simple we are testing two things -
        //1) Mixins are recognised by the Transformer resolver
        //2) that we successfully marshal and marshal an object that is not annotated directly
        MuleMessage message=  new DefaultMuleMessage(APPLE_JSON, muleContext);

        Apple apple = (Apple) message.getPayload(DataTypeFactory.create(Apple.class));
        assertNotNull(apple);
        assertFalse(apple.isWashed());
        assertTrue(apple.isBitten());

        message=  new DefaultMuleMessage(apple, muleContext);
        String json = (String) message.getPayload(DataTypeFactory.create(String.class));
        assertNotNull(json);
        JsonData data = new JsonData(json);
        assertEquals("true", data.get("bitten"));
        assertEquals("false", data.get("washed"));
    }
}