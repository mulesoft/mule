/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.activation.DataHandler;

import junit.framework.Assert;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.junit.Test;
import org.mockito.Mockito;

public class MessagePropertiesTestCase extends AbstractELTestCase
{
    public MessagePropertiesTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    @Test
    public void inboundPropertyMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setProperty("foo", "bar", PropertyScope.INBOUND);
        assertTrue(evaluate("message.inboundProperties", message) instanceof Map);
    }

    @Test
    public void assignToInboundPropertyMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertFinalProperty("message.inboundProperties='foo'", message);
    }

    @Test
    public void inboundProperty() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setProperty("foo", "bar", PropertyScope.INBOUND);
        assertEquals("bar", evaluate("message.inboundProperties['foo']", message));
    }

    @Test
    public void assignValueToInboundProperty() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setProperty("foo", "bar", PropertyScope.INBOUND);
        assertUnsupportedOperation("message.inboundProperties['foo']='bar'", message);
    }

    @Test
    public void assignValueToNewInboundProperty() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertUnsupportedOperation("message.inboundProperties['foo_new']='bar'", message);
    }

    @Test
    public void outboundPropertyMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setProperty("foo", "bar", PropertyScope.OUTBOUND);
        assertTrue(evaluate("message.outboundProperties", message) instanceof Map);
    }

    @Test
    public void assignToOutboundPropertyMap() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        assertFinalProperty("message.outboundProperties='foo'", message);
    }

    @Test
    public void outboundProperty() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "bar");
        assertEquals("bar", evaluate("message.outboundProperties['foo']", message));
    }

    @Test
    public void assignValueToOutboundProperty() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "bar_old");
        evaluate("message.outboundProperties['foo']='bar'", message);
        assertEquals("bar", message.getOutboundProperty("foo"));
    }

    @Test
    public void assignValueToNewOutboundProperty() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        evaluate("message.outboundProperties['foo']='bar'", message);
        assertEquals("bar", message.getOutboundProperty("foo"));
    }

    @Test
    public void inboundClear() throws Exception
    {
        assertUnsupportedOperation("message.inboundProperties.clear())", Mockito.mock(MuleMessage.class));
    }

    @Test
    public void inboundSize() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        message.setInboundProperty("foo", "abc");
        message.setInboundProperty("bar", "xyz");
        assertEquals(2, evaluate("message.inboundProperties.size()", message));
    }

    @Test
    public void inboundKeySet() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        message.setInboundProperty("foo", "abc");
        message.setInboundProperty("bar", "xyz");
        assertEquals("foo", evaluate("message.inboundProperties.keySet().toArray()[0]", message));
        assertEquals("bar", evaluate("message.inboundProperties.keySet().toArray()[1]", message));
    }

    @Test
    public void inboundContainsKey() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Mockito.mock(DataHandler.class);
        message.setInboundProperty("foo", "abc");
        Assert.assertTrue((Boolean)evaluate("message.inboundProperties.containsKey('foo')", message));
        Assert.assertFalse((Boolean)evaluate("message.inboundProperties.containsKey('bar')", message));
    }

    @Test
    public void inboundContainsValue() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setInboundProperty("foo", "abc");
        Assert.assertTrue((Boolean)evaluate("message.inboundProperties.containsValue('abc')", message));
        Assert.assertFalse((Boolean)evaluate("message.inboundProperties.containsValue('xyz')", message));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void inboundEntrySet() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setInboundProperty("foo", "abc");
        message.setInboundProperty("bar", "xyz");
        Set<Map.Entry<String, Object>> entrySet = (Set<Entry<String, Object>>)evaluate(
            "message.inboundProperties.entrySet()", message);
        assertEquals(2, entrySet.size());
        entrySet.contains(new DefaultMapEntry("foo", "abc"));
        entrySet.contains(new DefaultMapEntry("bar", "xyz"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void inboundValues() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setInboundProperty("foo", "abc");
        message.setInboundProperty("bar", "xyz");
        Collection<DataHandler> values = (Collection<DataHandler>)evaluate(
            "message.inboundProperties.values()", message);
        assertEquals(2, values.size());
        values.contains("abc");
        values.contains("xyz");
    }

    @Test
    public void inboundIsEmpty() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Assert.assertTrue((Boolean)evaluate("message.inboundProperties.isEmpty()", message));
        message.setInboundProperty("foo", "abc");
        message.setInboundProperty("bar", "xyz");
        Assert.assertFalse((Boolean)evaluate("message.inboundProperties.isEmpty()", message));
    }

    @Test
    public void inboundPutAll() throws Exception
    {
        assertUnsupportedOperation("message.inboundProperties.putAll(['foo': 'abc','bar': 'xyz'])",
            Mockito.mock(MuleMessage.class));
    }

    @Test
    public void inboundRemove() throws Exception
    {
        assertUnsupportedOperation("message.inboundProperties.remove('foo')", Mockito.mock(MuleMessage.class));
    }

    @Test
    public void outboundClear() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        evaluate("message.outboundProperties.clear()", message);
        assertEquals(0, message.getOutboundPropertyNames().size());
    }

    @Test
    public void outboundSize() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        assertEquals(2, evaluate("message.outboundProperties.size()", message));
    }

    @Test
    public void outboundKeySet() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        assertEquals("foo", evaluate("message.outboundProperties.keySet().toArray()[0]", message));
        assertEquals("bar", evaluate("message.outboundProperties.keySet().toArray()[1]", message));
    }

    @Test
    public void outboundContainsKey() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "abc");
        Assert.assertTrue((Boolean)evaluate("message.outboundProperties.containsKey('foo')", message));
        Assert.assertFalse((Boolean)evaluate("message.outboundProperties.containsKey('bar')", message));
    }

    @Test
    public void outboundContainsValue() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "abc");
        Assert.assertTrue((Boolean)evaluate("message.outboundProperties.containsValue('abc')", message));
        Assert.assertFalse((Boolean)evaluate("message.outboundProperties.containsValue('xyz')", message));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void outboundEntrySet() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        Set<Map.Entry<String, Object>> entrySet = (Set<Entry<String, Object>>)evaluate(
            "message.outboundProperties.entrySet()", message);
        assertEquals(2, entrySet.size());
        entrySet.contains(new DefaultMapEntry("foo", "abc"));
        entrySet.contains(new DefaultMapEntry("bar", "xyz"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void outboundValues() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        Collection<DataHandler> values = (Collection<DataHandler>)evaluate(
            "message.outboundProperties.values()", message);
        assertEquals(2, values.size());
        values.contains("abc");
        values.contains("xyz");
    }

    @Test
    public void outboundIsEmpty() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        Assert.assertTrue((Boolean)evaluate("message.outboundProperties.isEmpty()", message));
        message.setOutboundProperty("foo", "abc");
        message.setOutboundProperty("bar", "xyz");
        Assert.assertFalse((Boolean)evaluate("message.outboundProperties.isEmpty()", message));
    }

    @Test
    public void outboundPutAll() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        evaluate("message.outboundProperties.putAll(['foo': 'abc','bar': 'xyz'])", message);
        assertEquals("abc", evaluate("message.outboundProperties['foo']", message));
        assertEquals("xyz", evaluate("message.outboundProperties['bar']", message));
    }

    @Test
    public void outboundInboundRemove() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setOutboundProperty("foo", "abc");
        Assert.assertFalse((Boolean)evaluate("message.outboundProperties.isEmpty()", message));
        evaluate("message.outboundProperties.remove('foo')", message);
        Assert.assertTrue((Boolean)evaluate("message.outboundProperties.isEmpty()", message));
    }

}
