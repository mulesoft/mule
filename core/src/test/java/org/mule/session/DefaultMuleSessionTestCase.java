/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleSession;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.serialization.ObjectSerializer;
import org.mule.construct.Flow;
import org.mule.security.DefaultMuleAuthentication;
import org.mule.security.DefaultSecurityContextFactory;
import org.mule.security.MuleCredentials;
import org.mule.serialization.internal.JavaObjectSerializer;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultMuleSessionTestCase
{

    private static final int LATCH_TIMEOUT = 1000;

    private ObjectSerializer serializer;

    @Before
    public void before()
    {
        serializer = new JavaObjectSerializer();
    }

    @Test
    public void create()
    {
        DefaultMuleSession session = new DefaultMuleSession();
        assertCreate(session);
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
    public void copy() throws Exception
    {
        DefaultMuleSession original = new DefaultMuleSession();
        original.setValid(false);
        original.setSecurityContext(Mockito.mock(SecurityContext.class));
        original.setProperty("foo", "bar");

        DefaultMuleSession copy = new DefaultMuleSession(original);

        assertCopy(original, copy);

        // properties are copied but a new map instance is used
        assertSame(original.getProperty("foo"), copy.getProperty("foo"));
        copy.setProperty("new", "bar");
        assertNull(original.getProperty("new"));
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
        DefaultMuleSession session = new DefaultMuleSession();
        assertTrue(session.isValid());
        session.setValid(false);
        assertFalse(session.isValid());
        session.setValid(true);
        assertTrue(session.isValid());
    }

    @Test
    public void propertiesCaseInsensitive()
    {
        DefaultMuleSession session = new DefaultMuleSession();
        session.setProperty("key1", "value1");
        assertSame("value1", session.getProperty("key1"));

        // properties are case-insenstive
        session.setProperty("KEY1", "value2");
        assertSame("value2", session.getProperty("key1"));
    }

    @Test
    public void propertiesCaseInsensitiveAfterCopy()
    {
        DefaultMuleSession original = new DefaultMuleSession();
        DefaultMuleSession copy = new DefaultMuleSession(original);

        copy.setProperty("key1", "value1");
        assertSame("value1", copy.getProperty("key1"));

        // properties are case-insenstive
        copy.setProperty("KEY1", "value2");
        assertSame("value2", copy.getProperty("key1"));
    }

    @Test
    public void merge()
    {
        DefaultMuleSession copy1 = new DefaultMuleSession();
        DefaultMuleSession copy2 = new DefaultMuleSession();

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
    public void serialization() throws MuleException
    {
        Flow flow = new Flow("flow", Mockito.mock(MuleContext.class, RETURNS_DEEP_STUBS));
        DefaultMuleSession before = new DefaultMuleSession();
        before.setValid(false);
        before.setSecurityContext(createTestAuthentication());
        before.setProperty("foo", "bar");

        // Create mock muleContext
        MuleContext muleContext = Mockito.mock(MuleContext.class);
        MuleRegistry registry = Mockito.mock(MuleRegistry.class);
        Mockito.when(muleContext.getRegistry()).thenReturn(registry);
        Mockito.when(muleContext.getExecutionClassLoader()).thenReturn(getClass().getClassLoader());
        Mockito.when(registry.lookupFlowConstruct("flow")).thenReturn(flow);

        ((MuleContextAware) serializer).setMuleContext(muleContext);
        // Serialize and then deserialize
        DefaultMuleSession after = serializer.deserialize(serializer.serialize(before));

        // assertions
        assertEquals(before.getId(), after.getId());
        assertEquals(before.isValid(), after.isValid());
        assertEquals(before.getProperty("foo"), after.getProperty("foo"));
        assertEquals(before.getSecurityContext().getAuthentication().getPrincipal(),
            after.getSecurityContext().getAuthentication().getPrincipal());
        assertEquals(before.getSecurityContext().getAuthentication().getProperties().get("key1"),
            after.getSecurityContext().getAuthentication().getProperties().get("key1"));
        assertEquals(before.getSecurityContext().getAuthentication().getCredentials(),
            after.getSecurityContext().getAuthentication().getCredentials());
        // assertEquals(before.getSecurityContext().getAuthentication().getEvent().getId(),
        // after.getSecurityContext().getAuthentication().getEvent().getId());

        after.setProperty("new", "value");
        assertNull(before.getProperty("new"));

    }

    @Test
    @SuppressWarnings(value = {"deprecation"})
    public void serializationWithNonSerializableProperty() throws MuleException
    {
        MuleSession before = new DefaultMuleSession();
        Object nonSerializable = new Object();
        before.setProperty("foo", nonSerializable);
        before.setProperty("foo2", "bar2");

        MuleSession after = serializer.deserialize(serializer.serialize(before), getClass().getClassLoader());

        assertNotNull(after);
        assertNotSame(after, before);
        assertEquals("bar2", after.getProperty("foo2"));
        assertNull(after.getProperty("foo"));
    }

    private SecurityContext createTestAuthentication()
    {
        Authentication auth = new DefaultMuleAuthentication(new MuleCredentials("dan", new char[]{'d', 'f'}));
        auth.setProperties(Collections.<String, Object>singletonMap("key1", "value1"));
        SecurityContext securityContext = new DefaultSecurityContextFactory().create(auth);
        return securityContext;
    }
}
