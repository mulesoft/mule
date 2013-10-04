/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.jaxb;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.jaxb.model.Item;
import org.mule.module.xml.util.XMLUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;

import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JaxbAutoTransformerTestCase extends AbstractMuleContextTestCase
{
    public static final String ITEM_XML = "<item><code>1234</code><description>Vacuum Cleaner</description><in-stock>true</in-stock></item>";

    @Test
    public void testCustomTransform() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(ITEM_XML, muleContext);
        Item item = message.getPayload(DataTypeFactory.create(Item.class));

        assertNotNull(item);
        assertEquals("1234", item.getCode());
        assertEquals("Vacuum Cleaner", item.getDescription());
        assertTrue(item.isInStock());

        //and back again
        Document doc = message.getPayload(DataTypeFactory.create(Document.class));
        
        assertNotNull(doc);
        assertEquals("1234", XMLUtils.selectValue("/item/code", doc));
        assertEquals("Vacuum Cleaner", XMLUtils.selectValue("/item/description", doc));
        assertEquals("true", XMLUtils.selectValue("/item/in-stock", doc));
    }
}
