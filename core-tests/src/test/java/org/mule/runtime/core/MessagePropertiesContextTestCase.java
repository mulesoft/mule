/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

public class MessagePropertiesContextTestCase extends AbstractMuleContextTestCase
{
    public static final String CUSTOM_ENCODING = "UTF-16";

    @Test
    public void testPropertiesCase() throws Exception
    {
        //Default scope
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.setOutboundProperty("FOO", "BAR");
        mpc.setOutboundProperty("ABC", "abc");
        doTest(mpc);
    }

    @Test
    public void testPropertyScopeOrder() throws Exception
    {
        MuleEvent event = getTestEvent("testing");
        event.getSession().setProperty("Prop", "session");
        MuleMessage message = event.getMessage();

        message = MuleMessage.builder(message).addOutboundProperty("Prop", "outbound").build();
        event.setMessage(message);

        assertEquals("outbound", message.getOutboundProperty("Prop"));
        message = MuleMessage.builder(message).removeOutboundProperty("Prop").build();
        event.setMessage(message);

        assertNull(message.getInboundProperty("Prop"));
        assertNull(message.getOutboundProperty("Prop"));
    }

    @Test
    public void testPropertiesCaseAfterSerialization() throws Exception
    {
        //Default scope
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.setOutboundProperty("FOO", "BAR");
        mpc.setOutboundProperty("ABC", "abc");
        doTest(mpc);

        //Serialize and deserialize
        byte[] bytes = SerializationUtils.serialize(mpc);
        mpc = (MessagePropertiesContext) SerializationUtils.deserialize(bytes);
        doTest(mpc);
    }
    
    @Test
    public void testCopyConstructor() throws Exception
    {
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.setOutboundProperty("FOO", "BAR");
        mpc.setOutboundProperty("ABC", "abc");

        MessagePropertiesContext copy = new MessagePropertiesContext(mpc);
        
        assertNotSame(mpc.inboundMap, copy.inboundMap);
        assertNotSame(mpc.outboundMap, copy.outboundMap);
        doTest(copy);
        
        // Mutate original
        mpc.setOutboundProperty("FOO", "OTHER");
        assertSame(copy.getOutboundProperty("FOO"), "BAR");

        // Mutate copy
        copy.setOutboundProperty("ABC", "OTHER");
        assertSame(mpc.getOutboundProperty("ABC"), "abc");
    }

    protected void doTest(MessagePropertiesContext mpc)
    {
        assertThat(mpc.getOutboundProperty("FOO"), equalTo("BAR"));
        assertThat(mpc.getOutboundProperty("ABC"), equalTo("abc"));
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testInboundPropertyNamesImmutable() throws Exception
    {
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.getInboundPropertyNames().add("other");
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testOutboundPropertyNamesImmutable() throws Exception
    {
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.getInboundPropertyNames().add("other");
    }

    @Test
    public void setsDefaultScopedPropertyMetaData() throws Exception
    {
        DataType dataType = DataType.builder().type(Integer.class).mediaType(APPLICATION_XML).charset(CUSTOM_ENCODING).build();
        MessagePropertiesContext properties = new MessagePropertiesContext();
        properties.setOutboundProperty("Prop", "foo", dataType);
        DataType<?> actualDataType = properties.getOutboundPropertyDataType("Prop");
        assertThat(actualDataType, like(dataType));
    }
}
