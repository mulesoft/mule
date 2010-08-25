/*
 * $Id: JsonAutoTransformerWithMixinsTestCase.java 302 2010-02-17 07:57:47Z ross $
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
import org.mule.api.transformer.DataType;
import org.mule.module.json.JsonData;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import org.codehaus.jackson.map.ObjectMapper;
import org.mule.transformer.types.DataTypeFactory;

//TODO: IBEANS-141. No support for Mixin resolution yet
public class JsonAutoTransformerWithMixinsTestCase extends AbstractMuleTestCase
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

    public void testCustomTransform() throws Exception
    {
        //Though the data is simple we are testing two things -
        //1) Mixins are recognised by the Transformer resolver
        //2) that we successfully marshal and marshal an object that is not annotated directly
        MuleMessage message = new DefaultMuleMessage(APPLE_JSON, muleContext);
        Apple apple = (Apple) message.getPayload(DataTypeFactory.create(Apple.class));
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
