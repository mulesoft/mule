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
import org.mule.json.model.Item;
import org.mule.module.json.JsonData;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JsonAutoTransformerTestCase extends AbstractMuleContextTestCase
{
    public static final String ITEM_JSON = "{\"code\":\"1234\",\"description\":\"Vacuum Cleaner\",\"in-stock\":true}";

    @Test
    public void testCustomTransform() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(ITEM_JSON, muleContext);

        Item item = message.getPayload(DataTypeFactory.create(Item.class));
        assertNotNull(item);
        assertEquals("1234", item.getCode());
        assertEquals("Vacuum Cleaner", item.getDescription());
        assertTrue(item.isInStock());

        //and back again
        message = new DefaultMuleMessage(item, muleContext);
        String json = message.getPayload(DataType.STRING_DATA_TYPE);
        assertNotNull(json);
        assertEquals(ITEM_JSON, json);
        JsonData data = new JsonData(json);
        assertEquals("1234", data.getAsString("code"));
        assertEquals("Vacuum Cleaner", data.getAsString("description"));
        assertEquals("true", data.getAsString("in-stock"));
    }
}
