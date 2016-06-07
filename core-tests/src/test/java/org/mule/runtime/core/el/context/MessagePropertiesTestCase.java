/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.context;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.activation.DataHandler;

import junit.framework.Assert;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.junit.Before;
import org.junit.Test;

public class MessagePropertiesTestCase extends AbstractELTestCase
{

    private MuleEvent event;

    public MessagePropertiesTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    @Before
    public void setup() throws Exception
    {
        event = getTestEvent("");
    }

    @Test
    public void inboundPropertyMap() throws Exception
    {
        event = getTestEvent(new DefaultMuleMessage("", singletonMap("foo", "bar"), null, null, muleContext));
        assertTrue(evaluate("message.inboundProperties", event) instanceof Map);
    }

    @Test
    public void assignToInboundPropertyMap() throws Exception
    {
        assertFinalProperty("message.inboundProperties='foo'", event);
    }

    @Test
    public void inboundProperty() throws Exception
    {
        event = getTestEvent(new DefaultMuleMessage("", singletonMap("foo", "bar"), null, null, muleContext));
        assertEquals("bar", evaluate("message.inboundProperties['foo']", event));
    }

    @Test
    public void assignValueToInboundProperty() throws Exception
    {
        event = getTestEvent(new DefaultMuleMessage("", singletonMap("foo", "bar"), null, null, muleContext));
        assertUnsupportedOperation("message.inboundProperties['foo']='bar'", event);
    }

    @Test
    public void assignValueToNewInboundProperty() throws Exception
    {
        assertUnsupportedOperation("message.inboundProperties['foo_new']='bar'", event);
    }

    @Test
    public void outboundPropertyMap() throws Exception
    {
        event.getMessage().setOutboundProperty("foo", "bar");
        assertTrue(evaluate("message.outboundProperties", event) instanceof Map);
    }

    @Test
    public void assignToOutboundPropertyMap() throws Exception
    {
        assertFinalProperty("message.outboundProperties='foo'", event);
    }

    @Test
    public void outboundProperty() throws Exception
    {
        event.getMessage().setOutboundProperty("foo", "bar");
        assertEquals("bar", evaluate("message.outboundProperties['foo']", event));
    }

    @Test
    public void assignValueToOutboundProperty() throws Exception
    {
        event.getMessage().setOutboundProperty("foo", "bar_old");
        evaluate("message.outboundProperties['foo']='bar'", event);
        assertEquals("bar", event.getMessage().getOutboundProperty("foo"));
    }

    @Test
    public void assignValueToNewOutboundProperty() throws Exception
    {
        evaluate("message.outboundProperties['foo']='bar'", event);
        assertEquals("bar", event.getMessage().getOutboundProperty("foo"));
    }

    @Test
    public void inboundClear() throws Exception
    {
        assertUnsupportedOperation("message.inboundProperties.clear())", event);
    }

    @Test
    public void inboundSize() throws Exception
    {
        DefaultMuleMessage message = (DefaultMuleMessage) event.getMessage();
        mock(DataHandler.class);
        message.setInboundProperty("foo", "abc");
        message.setInboundProperty("bar", "xyz");
        assertEquals(2, evaluate("message.inboundProperties.size()", event));
    }

    @Test
    public void inboundKeySet() throws Exception
    {
        DefaultMuleMessage message = (DefaultMuleMessage) event.getMessage();
        mock(DataHandler.class);
        message.setInboundProperty("foo", "abc");
        message.setInboundProperty("bar", "xyz");
        assertEquals("foo", evaluate("message.inboundProperties.keySet().toArray()[0]", event));
        assertEquals("bar", evaluate("message.inboundProperties.keySet().toArray()[1]", event));
    }

    @Test
    public void inboundContainsKey() throws Exception
    {
        DefaultMuleMessage message = (DefaultMuleMessage) event.getMessage();
        mock(DataHandler.class);
        message.setInboundProperty("foo", "abc");
        Assert.assertTrue((Boolean)evaluate("message.inboundProperties.containsKey('foo')", event));
        Assert.assertFalse((Boolean)evaluate("message.inboundProperties.containsKey('bar')", event));
    }

    @Test
    public void inboundContainsValue() throws Exception
    {
        DefaultMuleMessage message = (DefaultMuleMessage) event.getMessage();
        message.setInboundProperty("foo", "abc");
        Assert.assertTrue((Boolean)evaluate("message.inboundProperties.containsValue('abc')", event));
        Assert.assertFalse((Boolean)evaluate("message.inboundProperties.containsValue('xyz')", event));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void inboundEntrySet() throws Exception
    {
        DefaultMuleMessage message = (DefaultMuleMessage) event.getMessage();
        message.setInboundProperty("foo", "abc");
        message.setInboundProperty("bar", "xyz");
        Set<Map.Entry<String, Object>> entrySet = (Set<Entry<String, Object>>)evaluate(
            "message.inboundProperties.entrySet()", event);
        assertEquals(2, entrySet.size());
        entrySet.contains(new DefaultMapEntry("foo", "abc"));
        entrySet.contains(new DefaultMapEntry("bar", "xyz"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void inboundValues() throws Exception
    {
        DefaultMuleMessage message = (DefaultMuleMessage) event.getMessage();
        message.setInboundProperty("foo", "abc");
        message.setInboundProperty("bar", "xyz");
        Collection<DataHandler> values = (Collection<DataHandler>)evaluate(
            "message.inboundProperties.values()", event);
        assertEquals(2, values.size());
        values.contains("abc");
        values.contains("xyz");
    }

    @Test
    public void inboundIsEmpty() throws Exception
    {
        DefaultMuleMessage message = (DefaultMuleMessage) event.getMessage();
        Assert.assertTrue((Boolean)evaluate("message.inboundProperties.isEmpty()", event));
        message.setInboundProperty("foo", "abc");
        message.setInboundProperty("bar", "xyz");
        Assert.assertFalse((Boolean)evaluate("message.inboundProperties.isEmpty()", event));
    }

    @Test
    public void inboundPutAll() throws Exception
    {
        assertUnsupportedOperation("message.inboundProperties.putAll(['foo': 'abc','bar': 'xyz'])", event);
    }

    @Test
    public void inboundRemove() throws Exception
    {
        assertUnsupportedOperation("message.inboundProperties.remove('foo')", event);
    }

    @Test
    public void outboundClear() throws Exception
    {
        MuleMessage message = event.getMessage();
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        evaluate("message.outboundProperties.clear()", event);
        assertEquals(0, message.getOutboundPropertyNames().size());
    }

    @Test
    public void outboundSize() throws Exception
    {
        MuleMessage message = event.getMessage();
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        assertEquals(2, evaluate("message.outboundProperties.size()", event));
    }

    @Test
    public void outboundKeySet() throws Exception
    {
        MuleMessage message = event.getMessage();
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        assertEquals("foo", evaluate("message.outboundProperties.keySet().toArray()[0]", event));
        assertEquals("bar", evaluate("message.outboundProperties.keySet().toArray()[1]", event));
    }

    @Test
    public void outboundContainsKey() throws Exception
    {
        event.getMessage().setOutboundProperty("foo", "abc");
        Assert.assertTrue((Boolean)evaluate("message.outboundProperties.containsKey('foo')", event));
        Assert.assertFalse((Boolean)evaluate("message.outboundProperties.containsKey('bar')", event));
    }

    @Test
    public void outboundContainsValue() throws Exception
    {
        event.getMessage().setOutboundProperty("foo", "abc");
        Assert.assertTrue((Boolean)evaluate("message.outboundProperties.containsValue('abc')", event));
        Assert.assertFalse((Boolean)evaluate("message.outboundProperties.containsValue('xyz')", event));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void outboundEntrySet() throws Exception
    {
        MuleMessage message = event.getMessage();
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        Set<Map.Entry<String, Object>> entrySet = (Set<Entry<String, Object>>)evaluate(
            "message.outboundProperties.entrySet()", event);
        assertEquals(2, entrySet.size());
        entrySet.contains(new DefaultMapEntry("foo", "abc"));
        entrySet.contains(new DefaultMapEntry("bar", "xyz"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void outboundValues() throws Exception
    {
        MuleMessage message = event.getMessage();
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        Collection<DataHandler> values = (Collection<DataHandler>)evaluate(
            "message.outboundProperties.values()", event);
        assertEquals(2, values.size());
        values.contains("abc");
        values.contains("xyz");
    }

    @Test
    public void outboundIsEmpty() throws Exception
    {
        MuleMessage message = event.getMessage();
        Assert.assertTrue((Boolean)evaluate("message.outboundProperties.isEmpty()", event));
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        Assert.assertFalse((Boolean)evaluate("message.outboundProperties.isEmpty()", event));
    }

    @Test
    public void outboundPutAll() throws Exception
    {
        MuleMessage message = event.getMessage();
        evaluate("message.outboundProperties.putAll(['foo': 'abc','bar': 'xyz'])", event);
        assertEquals("abc", evaluate("message.outboundProperties['foo']", event));
        assertEquals("xyz", evaluate("message.outboundProperties['bar']", event));
    }

    @Test
    public void outboundInboundRemove() throws Exception
    {
        MuleMessage message = event.getMessage();
        message.setOutboundProperty("foo", "abc");
        Assert.assertFalse((Boolean)evaluate("message.outboundProperties.isEmpty()", event));
        evaluate("message.outboundProperties.remove('foo')", event);
        Assert.assertTrue((Boolean)evaluate("message.outboundProperties.isEmpty()", event));
    }

}
