/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.transformer.types.MimeTypes;

import java.util.Set;

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
        mpc.setProperty("FOO", "BAR", PropertyScope.OUTBOUND);
        mpc.setProperty("ABC", "abc", PropertyScope.OUTBOUND);
        doTest(mpc);
    }

    @Test
    public void testPropertyScopeOrder() throws Exception
    {
        MuleEvent e = getTestEvent("testing");
        e.getSession().setProperty("Prop", "session");

        MuleMessage message = e.getMessage();
        //Note that we cannot write to the Inbound scope, its read only
        message.setOutboundProperty("Prop", "outbound");

        assertEquals("outbound", message.getOutboundProperty("Prop"));
        message.removeOutboundProperty("Prop");

        assertNull(message.getInboundProperty("Prop"));
        assertNull(message.getOutboundProperty("Prop"));
    }

    @Test
    public void testPropertiesCaseAfterSerialization() throws Exception
    {
        //Default scope
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.setProperty("FOO", "BAR", PropertyScope.OUTBOUND);
        mpc.setProperty("ABC", "abc", PropertyScope.OUTBOUND);
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
        mpc.setProperty("FOO", "BAR", PropertyScope.OUTBOUND);
        mpc.setProperty("ABC", "abc", PropertyScope.OUTBOUND);

        MessagePropertiesContext copy = new MessagePropertiesContext(mpc);
        
        assertNotSame(mpc.inboundMap, copy.inboundMap);
        assertNotSame(mpc.outboundMap, copy.outboundMap);
        doTest(copy);
        
        // Mutate original
        mpc.setProperty("FOO", "OTHER", PropertyScope.OUTBOUND);
        assertSame(copy.getProperty("FOO", PropertyScope.OUTBOUND), "BAR");

        // Mutate copy
        copy.setProperty("ABC", "OTHER", PropertyScope.OUTBOUND);
        assertSame(mpc.getProperty("ABC", PropertyScope.OUTBOUND), "abc");
    }

    protected void doTest(MessagePropertiesContext mpc)
    {
        //Look in all scopes
        assertEquals("BAR", mpc.getProperty("foo", PropertyScope.OUTBOUND));
        assertEquals("abc", mpc.getProperty("abc", PropertyScope.OUTBOUND));

        //Look in specific scope
        assertEquals("BAR", mpc.getProperty("foO", PropertyScope.OUTBOUND)); //default scope

        //Not found using other specific scopes
        assertNull(mpc.getProperty("doo", PropertyScope.INBOUND));
        assertNull(mpc.getProperty("doo", PropertyScope.OUTBOUND));

        Set<String> keys = mpc.getPropertyNames(PropertyScope.OUTBOUND);
        assertEquals(2, keys.size());

        for (String key : keys)
        {
            assertTrue(key.equals("FOO") || key.equals("DOO") || key.equals("ABC"));
            assertFalse(key.equals("foo") || key.equals("doo") || key.equals("abc"));
        }
    }
    
    @Test(expected=UnsupportedOperationException.class)
    public void testInboundPropertyNamesImmutable() throws Exception
    {
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.getPropertyNames(PropertyScope.INBOUND).add("other");
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testOutboundPropertyNamesImmutable() throws Exception
    {
        MessagePropertiesContext mpc = new MessagePropertiesContext();
        mpc.getPropertyNames(PropertyScope.OUTBOUND).add("other");
    }

    @Test
    public void setsDefaultScopedPropertyMetaData() throws Exception
    {
        DataType dataType = DataTypeFactory.create(Integer.class, MimeTypes.APPLICATION_XML);
        dataType.setEncoding(CUSTOM_ENCODING);
        MessagePropertiesContext properties = new MessagePropertiesContext();
        properties.setProperty("Prop", "foo", PropertyScope.OUTBOUND, dataType);
        DataType<?> actualDataType = properties.getPropertyDataType("Prop", PropertyScope.OUTBOUND);

        assertThat(actualDataType, like(dataType));
    }
}
