/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.simple.MessagePropertiesTransformer;
import org.mule.transformer.types.MimeTypes;
import org.mule.transformer.types.TypedValue;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class MessagePropertiesTransformerTestCase extends FunctionalTestCase
{

    public static final String BAR_PROPERTY = "bar";
    public static final String FOO_PROPERTY = "foo";
    public static final String TEXT_BAZ_MIME_TYPE = "text/baz";
    public static final String FOO_VALUE = "fooValue";

    @Override
    protected String getConfigFile()
    {
        return "message-properties-transformer-config.xml";
    }

    @Test
    public void testOverwriteFlagEnabledByDefault() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map<String, TypedValue> add = new HashMap<>();
        add.put("addedProperty", new TypedValue("overwrittenValue", DataType.STRING_DATA_TYPE));
        t.setAddTypedProperties(add);
        t.setMuleContext(muleContext);

        MuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setOutboundProperty("addedProperty", "originalValue");
        MuleEventContext ctx = getTestEventContext(msg);
        // context clones message
        msg = ctx.getMessage();
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, (String)null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        compareProperties(msg, transformed);

        assertEquals("overwrittenValue", transformed.getOutboundProperty("addedProperty"));
    }

    @Test
    public void testOverwriteFalsePreservesOriginal() throws Exception
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        Map<String, TypedValue> add = new HashMap<>();
        add.put("addedProperty", new TypedValue("overwrittenValue", DataType.STRING_DATA_TYPE));
        t.setAddTypedProperties(add);
        t.setOverwrite(false);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setProperty("addedProperty", "originalValue", PropertyScope.INVOCATION);
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, (String)null);
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
        Map<String, TypedValue> add = new HashMap<>();
        add.put("Foo", new TypedValue("#[header:public-house]", DataType.STRING_DATA_TYPE));
        t.setAddTypedProperties(add);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setOutboundProperty("public-house", "Bar");
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, (String)null);
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
        t.setScope(PropertyScope.INVOCATION);
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setInvocationProperty("Foo", "Bar");
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg);
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
        t.setDeleteProperties("badProperty");
        t.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        msg.setOutboundProperty("badProperty", "badValue");
        assertEquals("badValue", msg.<Object>getOutboundProperty("badProperty"));
        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg, (String)null);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        compareProperties(msg, transformed);

        assertFalse(transformed.getInvocationPropertyNames().contains("badValue"));
        assertFalse(transformed.getInboundPropertyNames().contains("badValue"));
        assertFalse(transformed.getOutboundPropertyNames().contains("badValue"));
        assertFalse(transformed.getSessionPropertyNames().contains("badValue"));
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

        TypedValue typedValue = transformer.getAddProperties().get("Content-Type");
        assertThat((String) typedValue.getValue(), equalTo("text/baz;charset=UTF-16BE"));
        assertThat(typedValue.getDataType(), like(Object.class, MimeTypes.ANY, null));

        typedValue = transformer.getAddProperties().get("key");
        assertThat((String) typedValue.getValue(), equalTo("value"));
        assertThat(typedValue.getDataType(), like(String.class, MimeTypes.ANY, null));

        assertEquals("test-property1", transformer.getDeleteProperties().get(0));
        assertEquals("test-property2", transformer.getDeleteProperties().get(1));
        assertEquals("Faz", transformer.getRenameProperties().get("Foo"));
        assertEquals(PropertyScope.OUTBOUND, transformer.getScope());
    }

    @Test
    public void setsDataTypeFromConfig() throws Exception
    {
        MessagePropertiesTransformer transformer = (MessagePropertiesTransformer) muleContext.getRegistry().lookupTransformer("addPropertyWithDataType");
        transformer.setMuleContext(muleContext);
        assertNotNull(transformer.getAddProperties());
        assertEquals(1, transformer.getAddProperties().size());

        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        DefaultMuleMessage transformed = (DefaultMuleMessage) transformer.transform(msg);

        assertThat(FOO_VALUE, equalTo(transformed.getOutboundProperty(FOO_PROPERTY)));
        assertThat(transformed.getPropertyDataType(FOO_PROPERTY, PropertyScope.OUTBOUND), like(String.class, TEXT_BAZ_MIME_TYPE, StandardCharsets.UTF_16BE.name()));
    }

    @Test
    public void copiesDataTypeOnRenameProperty() throws Exception
    {
        MessagePropertiesTransformer transformer = (MessagePropertiesTransformer) muleContext.getRegistry().lookupTransformer("renamePropertyWithDataType");
        transformer.setMuleContext(muleContext);

        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        DefaultMuleMessage transformed = (DefaultMuleMessage) transformer.transform(msg);

        assertThat(FOO_VALUE, equalTo(transformed.getOutboundProperty(BAR_PROPERTY)));
        assertThat(transformed.getPropertyDataType(BAR_PROPERTY, PropertyScope.OUTBOUND), like(String.class, TEXT_BAZ_MIME_TYPE, StandardCharsets.UTF_16BE.name()));
    }

    @Test
    public void testDeleteUsingPropertyName() throws Exception
    {
        final String expression = "badProperty";
        final String[] validProperties = new String[] {"somethingnotsobad"};
        final String[] invalidProperties = new String[] {"badProperty"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    @Test
    public void testDeletePropertiesStartingWithExpression() throws Exception
    {
        final String expression = "^bad.*";
        final String[] validProperties = new String[] {"somethingnotsobad"};
        final String[] invalidProperties = new String[] {"badProperty", "badThing"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    @Test
    public void testDeletePropertiesCaseInsensitiveRegex() throws Exception
    {
        final String expression = "(?i)^BAD.*";
        final String[] validProperties = new String[] {"somethingnotsobad"};
        final String[] invalidProperties = new String[] {"badProperty", "badThing"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    @Test
    public void testDeletePropertiesEndingWithExpression() throws Exception
    {
        final String expression = ".*bad$";
        final String[] validProperties = new String[] {"badProperty", "badThing"};
        final String[] invalidProperties = new String[] {"somethingnotsobad"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    @Test
    public void testDeletePropertiesContainingExpression() throws Exception
    {
        final String expression = ".*bad.*";
        final String[] validProperties = new String[] {};
        final String[] invalidProperties = new String[] {"badProperty", "badThing", "somethingnotsobad"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    @Test
    public void testDeletePropertiesUsingWildcard() throws Exception
    {
        final String expression = "bad*";
        final String[] validProperties = new String[] {"somethingnotsobad"};
        final String[] invalidProperties = new String[] {"badProperty", "badThing"};

        doTestMessageTransformationWithExpression(expression, validProperties, invalidProperties);
    }

    private void doTestMessageTransformationWithExpression(String expression, String[] validProperties, String[] invalidProperties)
            throws TransformerException
    {
        MessagePropertiesTransformer t = createTransformerWithExpression(expression);

        DefaultMuleMessage msg = new DefaultMuleMessage("message", muleContext);
        addPropertiesToMessage(validProperties, msg);
        addPropertiesToMessage(invalidProperties, msg);

        DefaultMuleMessage transformed = (DefaultMuleMessage) t.transform(msg);
        assertSame(msg, transformed);
        assertEquals(msg.getUniqueId(), transformed.getUniqueId());
        assertEquals(msg.getPayload(), transformed.getPayload());
        assertMessageContainsExpectedProperties(validProperties, invalidProperties, transformed);
    }

    private void assertMessageContainsExpectedProperties(String[] validProperties, String[] invalidProperties, DefaultMuleMessage transformed)
    {
        for (String property : validProperties)
        {
            assertTrue("Should contain property: " + property, transformed.getOutboundPropertyNames().contains(property));
        }

        for (String property : invalidProperties)
        {
            assertFalse("Should not contain property: " + property, transformed.getOutboundPropertyNames().contains(property));
        }
    }

    private MessagePropertiesTransformer createTransformerWithExpression(String expression)
    {
        MessagePropertiesTransformer t = new MessagePropertiesTransformer();
        t.setDeleteProperties(expression);
        t.setMuleContext(muleContext);
        return t;
    }

    private void addPropertiesToMessage(String[] validProperties, DefaultMuleMessage msg)
    {
        for (String property : validProperties)
        {
            msg.setOutboundProperty(property, "defaultPropertyValue");
        }
    }

    private void compareProperties(MuleMessage msg, MuleMessage transformed)
    {
        assertEquals(msg.getInvocationPropertyNames(), transformed.getInvocationPropertyNames());
        assertEquals(msg.getInboundPropertyNames(), transformed.getInboundPropertyNames());
        assertEquals(msg.getOutboundPropertyNames(), transformed.getOutboundPropertyNames());
        assertEquals(msg.getSessionPropertyNames(), transformed.getSessionPropertyNames());
    }

}
