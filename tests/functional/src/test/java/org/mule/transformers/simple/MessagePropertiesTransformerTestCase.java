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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.FunctionalTestCase;
import org.mule.transformer.simple.MessagePropertiesTransformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MessagePropertiesTransformerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "message-properties-transformer-config.xml";
    }

    @Test
    public void testOverwriteFlagEnabledByDefault() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map<String, Object> add = new HashMap<String, Object>();
        add.put("addedProperty", "overwrittenValue");
        t.setAddProperties(add);
        t.setMuleContext(muleContext);

        MuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setOutboundProperty("addedProperty", "originalValue");
        MuleEventContext ctx = getTestEventContext(msg);
        // context clones message
        msg = ctx.getMessage();
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        assertEquals(msg.getPropertyNames(PropertyScope.INVOCATION), transformed.getPropertyNames(PropertyScope.INVOCATION));
        assertEquals(msg.getPropertyNames(PropertyScope.INBOUND), transformed.getPropertyNames(PropertyScope.INBOUND));
        assertEquals(msg.getPropertyNames(PropertyScope.OUTBOUND), transformed.getPropertyNames(PropertyScope.OUTBOUND));
        assertEquals(msg.getPropertyNames(PropertyScope.SESSION), transformed.getPropertyNames(PropertyScope.SESSION));

        assertEquals("overwrittenValue", transformed.getOutboundProperty("addedProperty"));
    }

    @Test
    public void testOverwriteFalsePreservesOriginal() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map<String, Object> add = new HashMap<String, Object>();
        add.put("addedProperty", "overwrittenValue");
        t.setAddProperties(add);
        t.setOverwrite(false);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setProperty("addedProperty", "originalValue", PropertyScope.INVOCATION);
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        compareProperties(msg, transformed);

        assertEquals("originalValue", transformed.getInvocationProperty("addedProperty"));
    }

    @Test
    public void testExpressionsInAddProperties() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map<String, Object> add = new HashMap<String, Object>();
        add.put("Foo", "#[header:public-house]");
        t.setAddProperties(add);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setOutboundProperty("public-house", "Bar");
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        compareProperties(msg, transformed);

        assertEquals("Bar", transformed.getOutboundProperty("Foo"));
    }

    @Test
    public void testRenameProperties() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map<String, String> add = new HashMap<String, String>();
        add.put("Foo", "Baz");
        t.setRenameProperties(add);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setProperty("Foo", "Bar", PropertyScope.INVOCATION);
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        compareProperties(msg, transformed);

        assertEquals("Bar", transformed.getInvocationProperty("Baz"));
    }

    @Test
    public void testDelete() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        t.setDeleteProperties(Collections.singletonList("badProperty"));
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setOutboundProperty("badProperty", "badValue");
        assertEquals("badValue", msg.<Object>getOutboundProperty("badProperty"));
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        compareProperties(msg, transformed);

        assertFalse(transformed.getPropertyNames(PropertyScope.INVOCATION).contains("badValue"));
        assertFalse(transformed.getPropertyNames(PropertyScope.INBOUND).contains("badValue"));
        assertFalse(transformed.getPropertyNames(PropertyScope.OUTBOUND).contains("badValue"));
        assertFalse(transformed.getPropertyNames(PropertyScope.SESSION).contains("badValue"));
    }

    @Test
    public void testTransformerConfig() throws Exception
    {
        MessagePropertiesTransformer transformer = (MessagePropertiesTransformer) muleContext.getRegistry().lookupTransformer("testTransformer");
        transformer.setMuleContext(muleContext);
        assertNotNull(transformer);
        assertNotNull(transformer.getAddProperties());
        assertNotNull(transformer.getDeleteProperties());
        assertEquals(2, transformer.getAddProperties().size());
        assertEquals(2, transformer.getDeleteProperties().size());
        assertEquals(1, transformer.getRenameProperties().size());
        assertTrue(transformer.isOverwrite());
        assertEquals("text/baz;charset=UTF-16BE", transformer.getAddProperties().get("Content-Type"));
        assertEquals("value", transformer.getAddProperties().get("key"));
        assertEquals("test-property1", transformer.getDeleteProperties().get(0));
        assertEquals("test-property2", transformer.getDeleteProperties().get(1));
        assertEquals("Faz", transformer.getRenameProperties().get("Foo"));
        assertEquals(null, transformer.getScope());
    }

    private void compareProperties(DefaultMuleMessage msg, DefaultMuleMessage transformed)
    {
        assertEquals(msg.getPropertyNames(PropertyScope.INVOCATION), transformed.getPropertyNames(PropertyScope.INVOCATION));
        assertEquals(msg.getPropertyNames(PropertyScope.INBOUND), transformed.getPropertyNames(PropertyScope.INBOUND));
        assertEquals(msg.getPropertyNames(PropertyScope.OUTBOUND), transformed.getPropertyNames(PropertyScope.OUTBOUND));
        assertEquals(msg.getPropertyNames(PropertyScope.SESSION), transformed.getPropertyNames(PropertyScope.SESSION));
    }

}
