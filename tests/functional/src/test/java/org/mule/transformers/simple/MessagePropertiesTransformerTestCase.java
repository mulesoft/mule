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
import org.mule.api.transformer.TransformerException;
import org.mule.tck.FunctionalTestCase;
import org.mule.transformer.simple.MessagePropertiesTransformer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MessagePropertiesTransformerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "message-properties-transformer-config.xml";
    }

    public void testOverwriteFlagEnabledByDefault() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map add = new HashMap();
        add.put("addedProperty", "overwrittenValue");
        t.setAddProperties(add);
        t.setMuleContext(muleContext);

        MuleMessage msg = new DefaultMuleMessage("message", (Map) null, muleContext);
        msg.setProperty("addedProperty", "originalValue");
        MuleEventContext ctx = getTestEventContext(msg);
        // context clones message
        msg = ctx.getMessage();
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        // property values will be different
        assertEquals(msg.getPropertyNames(), transformed.getPropertyNames());

        assertEquals("overwrittenValue", transformed.getProperty("addedProperty"));
    }

    public void testOverwriteFalsePreservesOriginal() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map add = new HashMap();
        add.put("addedProperty", "overwrittenValue");
        t.setAddProperties(add);
        t.setOverwrite(false);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", (Map) null, muleContext);
        msg.setProperty("addedProperty", "originalValue");
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        assertEquals(msg.getPropertyNames(), transformed.getPropertyNames());

        assertEquals("originalValue", transformed.getProperty("addedProperty"));
    }

    public void testExpressionsInAddProperties() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map add = new HashMap();
        add.put("Foo", "#[header:public-house]");
        t.setAddProperties(add);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", (Map) null, muleContext);
        msg.setProperty("public-house", "Bar");
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        assertEquals(msg.getPropertyNames(), transformed.getPropertyNames());

        assertEquals("Bar", transformed.getProperty("Foo"));
    }

    public void testRenameProperties() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map add = new HashMap();
        add.put("Foo", "Baz");
        t.setRenameProperties(add);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", (Map) null, muleContext);
        msg.setProperty("Foo", "Bar");
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        assertEquals(msg.getPropertyNames(), transformed.getPropertyNames());

        assertEquals("Bar", transformed.getProperty("Baz"));
    }

    public void testDeleteUsingPropertyName() throws Exception
    {
        final String expression = "badProperty";
        final String[] validProperties = new String[] {"somethingnotsobad"};
        final String[] invalidProperties = new String[] {"badProperty"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    public void testDeletePropertiesStartingWithExpression() throws Exception
    {
        final String expression = "^bad.*";
        final String[] validProperties = new String[] {"somethingnotsobad"};
        final String[] invalidProperties = new String[] {"badProperty", "badThing"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    public void testDeletePropertiesEndingWithExpression() throws Exception
    {
        final String expression = ".*bad$";
        final String[] validProperties = new String[] {"badProperty", "badThing"};
        final String[] invalidProperties = new String[] {"somethingnotsobad"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    public void testDeletePropertiesContainingExpression() throws Exception
    {
        final String expression = ".*bad.*";
        final String[] validProperties = new String[] {};
        final String[] invalidProperties = new String[] {"badProperty", "badThing", "somethingnotsobad"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    private void doTestMessageTransformationWithExpression(String expression, String[] validProperties, String[] invalidProperties)
            throws TransformerException
    {
        MessagePropertiesTransformer t = createTransformerWithExpression(expression);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", (Map) null, muleContext);

        addPropertiesToMessage(validProperties, msg);
        addPropertiesToMessage(invalidProperties, msg);

        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        assertEquals(msg.getPropertyNames(), transformed.getPropertyNames());
        assertNotNull(transformed.getPropertyNames());
        assertMessageContainsExpectedProperties(validProperties, invalidProperties, transformed);
    }

    private void assertMessageContainsExpectedProperties(String[] validProperties, String[] invalidProperties, DefaultMuleMessage transformed)
    {
        for (String property : validProperties)
        {
            assertTrue("Should contain property: " + property, transformed.getPropertyNames().contains(property));
        }

        for (String property : invalidProperties)
        {
            assertFalse("Should not contain property: " + property, transformed.getPropertyNames().contains(property));
        }
    }

    private MessagePropertiesTransformer createTransformerWithExpression(String expression)
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        t.setDeleteProperties(Collections.singletonList(expression));
        t.setMuleContext(muleContext);
        return t;
    }

    private void addPropertiesToMessage(String[] validProperties, DefaultMuleMessage msg)
    {
        for (String property : validProperties)
        {
            msg.setProperty(property, "defaultPropertyValue");
        }
    }

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
}
