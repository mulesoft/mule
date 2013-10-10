/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.transformers;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.module.json.JsonData;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformer.types.DataTypeFactory;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

//TODO: IBEANS-141. No support for Mixin resolution yet
public class JsonAutoTransformerWithMixinsTestCase extends AbstractMuleContextTestCase
{
    public static final String APPLE_JSON = "{\"washed\":false,\"bitten\":true}";

    @Override
    protected void doSetUp() throws Exception
    {
        //We don't register a custom transformer, instead we register a 'global' mapper that will
        //be used for Json transforms
        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(Apple.class, AppleMixin.class);
        mapper.getDeserializationConfig().addMixInAnnotations(Apple.class, AppleMixin.class);
        muleContext.getRegistry().registerObject("mapper", mapper);
    }

    @Test
    public void testCustomTransform() throws Exception
    {
        //Though the data is simple we are testing two things -
        //1) Mixins are recognised by the Transformer resolver
        //2) that we successfully marshal and marshal an object that is not annotated directly
        MuleMessage message = new DefaultMuleMessage(APPLE_JSON, muleContext);
        Apple apple = message.getPayload(DataTypeFactory.create(Apple.class));
        assertNotNull(apple);
        assertFalse(apple.isWashed());
        assertTrue(apple.isBitten());

        message = new DefaultMuleMessage(apple, muleContext);
        String json = message.getPayload(DataType.STRING_DATA_TYPE);
        assertNotNull(json);
        JsonData data = new JsonData(json);
        assertEquals("true", data.get("bitten"));
        assertEquals("false", data.get("washed"));
    }
}
