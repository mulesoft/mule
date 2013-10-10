/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.transformers;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.json.JsonData;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformer.types.DataTypeFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JsonCustomTransformerWithMixinsTestCase extends AbstractMuleContextTestCase
{
    public static final String APPLE_JSON = "{\"washed\":false,\"bitten\":true}";

    @Override
    protected void doSetUp() throws Exception
    {
        muleContext.getRegistry().registerObject("trans", new JsonCustomTransformerWithMixins());
    }

    @Test
    public void testCustomTransform() throws Exception
    {
        //THough the data is simple we are testing two things -
        //1) Mixins are recognised by the Transformer resolver
        //2) that we successfully marshal and marshal an object that is not annotated directly
        MuleMessage message=  new DefaultMuleMessage(APPLE_JSON, muleContext);

        Apple apple = message.getPayload(DataTypeFactory.create(Apple.class));
        assertNotNull(apple);
        assertFalse(apple.isWashed());
        assertTrue(apple.isBitten());

        message=  new DefaultMuleMessage(apple, muleContext);
        String json = message.getPayload(DataTypeFactory.STRING);
        assertNotNull(json);
        JsonData data = new JsonData(json);
        assertEquals("true", data.getAsString("bitten"));
        assertEquals("false", data.getAsString("washed"));
    }
}
