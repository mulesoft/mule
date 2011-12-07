/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.security.SecurityContext;
import org.mule.util.SerializationUtils;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultMuleSessionTestCase
{

    @Test
    public void create()
    {
        DefaultMuleSession session = new DefaultMuleSession(Mockito.mock(MuleContext.class));
        assertNull(session.getFlowConstruct());
    }

    @Test
    public void createWithFlowConstuct()
    {
        FlowConstruct flowConstruct = Mockito.mock(FlowConstruct.class);
        DefaultMuleSession session = new DefaultMuleSession(flowConstruct, Mockito.mock(MuleContext.class));
        assertSame(flowConstruct, session.getFlowConstruct());
    }

    protected void assertCreate(DefaultMuleSession session)
    {
        assertNotNull(session.getId());
        assertNull(session.getSecurityContext());
        assertNotNull(session.getPropertyNamesAsSet());
        assertTrue(session.getPropertyNamesAsSet().isEmpty());
        assertTrue(session.isValid());
    }

    @Test
    public void copy()
        throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException
    {
        FlowConstruct flowConstruct = Mockito.mock(FlowConstruct.class);
        DefaultMuleSession original = new DefaultMuleSession(flowConstruct, Mockito.mock(MuleContext.class));
        original.setValid(false);
        original.setSecurityContext(Mockito.mock(SecurityContext.class));
        original.setProperty("foo", "bar");

        DefaultMuleSession copy = new DefaultMuleSession(original, Mockito.mock(MuleContext.class));

        assertCopy(original, copy);

        assertSame(copy.getFlowConstruct(), original.getFlowConstruct());

        // properties are copied but a new map instance is used
        assertSame(original.getProperty("foo"), copy.getProperty("foo"));
        copy.setProperty("new", "bar");
        assertNull(original.getProperty("new"));
    }

    @Test
    public void copyWithFlowConstruct()
        throws IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException
    {
        FlowConstruct originalFlow = Mockito.mock(FlowConstruct.class);
        FlowConstruct newFlow = Mockito.mock(FlowConstruct.class);
        DefaultMuleSession original = new DefaultMuleSession(originalFlow, Mockito.mock(MuleContext.class));
        original.setValid(false);
        original.setSecurityContext(Mockito.mock(SecurityContext.class));
        original.setProperty("foo", "bar");

        DefaultMuleSession copy = new DefaultMuleSession(original, newFlow);

        assertCopy(original, copy);

        assertSame(copy.getFlowConstruct(), newFlow);

        // properties map is copied
        assertSame(original.getProperty("foo"), copy.getProperty("foo"));
        copy.setProperty("new", "bar");
        assertNotNull(original.getProperty("new"));
    }

    protected void assertCopy(DefaultMuleSession original, DefaultMuleSession copy)
    {
        assertSame(copy.getId(), original.getId());
        assertSame(copy.isValid(), original.isValid());
        assertSame(copy.getSecurityContext(), original.getSecurityContext());
    }

    @Test
    public void valid()
    {
        DefaultMuleSession session = new DefaultMuleSession(Mockito.mock(MuleContext.class));
        assertTrue(session.isValid());
        session.setValid(false);
        assertFalse(session.isValid());
        session.setValid(true);
        assertTrue(session.isValid());
    }

    @Test
    public void propertiesCaseInsensitive()
    {
        DefaultMuleSession session = new DefaultMuleSession(Mockito.mock(MuleContext.class));
        session.setProperty("key1", "value1");
        assertSame("value1", session.getProperty("key1"));

        // properties are case-insenstive
        session.setProperty("KEY1", "value2");
        assertSame("value2", session.getProperty("key1"));
    }

    @Test
    public void propertiesCaseInsensitiveAfterCopy()
    {
        DefaultMuleSession original = new DefaultMuleSession(Mockito.mock(MuleContext.class));
        DefaultMuleSession copy = new DefaultMuleSession(original, Mockito.mock(MuleContext.class));

        copy.setProperty("key1", "value1");
        assertSame("value1", copy.getProperty("key1"));

        // properties are case-insenstive
        copy.setProperty("KEY1", "value2");
        assertSame("value2", copy.getProperty("key1"));
    }

    @Test
    public void merge()
    {
        DefaultMuleSession copy1 = new DefaultMuleSession(Mockito.mock(MuleContext.class));
        DefaultMuleSession copy2 = new DefaultMuleSession(Mockito.mock(MuleContext.class));

        Object nonSerializableValue2 = new Object();
        Object nonSerializableValue3 = new Object();

        copy1.setProperty("key1", "value1");
        copy1.setProperty("key2", nonSerializableValue2);
        copy1.setProperty("key3", nonSerializableValue3);
        copy1.setProperty("key4", "value4");
        copy1.setProperty("key5", "value5");
        copy1.setProperty("key6", "value6");

        copy2.setProperty("key1", "value1");
        copy2.setProperty("key2", "value2");
        copy2.setProperty("KEY4", "value4");
        copy2.setProperty("KEY5", "value5NEW");
        copy2.setProperty("key7", "value7");

        int copy2PropertiesHashCode = copy2.getPropertyNamesAsSet().hashCode();

        copy1.merge(copy2);

        assertEquals(6, copy1.getPropertyNamesAsSet().size());
        assertEquals("value1", copy1.getProperty("key1"));
        assertEquals("value2", copy1.getProperty("key2"));
        assertEquals(nonSerializableValue3, copy1.getProperty("key3"));
        assertEquals("value4", copy1.getProperty("key4"));
        assertEquals("value5NEW", copy1.getProperty("key5"));
        assertNull(copy1.getProperty("key6"));
        assertEquals("value7", copy1.getProperty("key7"));

        assertEquals(5, copy2.getPropertyNamesAsSet().size());
        assertEquals(copy2PropertiesHashCode, copy2.getPropertyNamesAsSet().hashCode());
    }

    @Test
    @Ignore
    public void serialization()
    {
        DefaultMuleSession before = new DefaultMuleSession(Mockito.mock(FlowConstruct.class),
            Mockito.mock(MuleContext.class));
        before.setValid(false);
        before.setSecurityContext(Mockito.mock(SecurityContext.class));
        before.setProperty("foo", "bar");
        before.setProperty("notSerializableValue", new Object());

        DefaultMuleSession after = (DefaultMuleSession) SerializationUtils.deserialize(
            SerializationUtils.serialize(before), getClass().getClassLoader());

        assertEquals(before.getId(), after.getId());

    }

}
