/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.Authentication;
import org.mule.api.security.Credentials;
import org.mule.api.security.SecurityContext;
import org.mule.api.transport.SessionHandler;
import org.mule.security.DefaultMuleAuthentication;
import org.mule.security.DefaultSecurityContextFactory;
import org.mule.security.MuleCredentials;
import org.mule.session.DefaultMuleSession;
import org.mule.session.LegacySessionHandler;
import org.mule.session.SerializeAndEncodeSessionHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.base.Charsets;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.SerializationException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static org.apache.commons.lang.SerializationUtils.serialize;
import static org.apache.xmlbeans.impl.util.Base64.encode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.api.config.MuleProperties.MULE_SESSION_PROPERTY;

import com.eaio.exec.Executable;

public class MuleSessionHandlerTestCase extends AbstractMuleTestCase
{

    private static String originalEncoding;

    private MuleContext muleContext;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception
    {
        muleContext = mock(MuleContext.class);
        MuleConfiguration configuration = mock(MuleConfiguration.class);
        when(configuration.getDefaultEncoding()).thenReturn(Charsets.UTF_8.name());
        when(muleContext.getConfiguration()).thenReturn(configuration);
        when(muleContext.getExecutionClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
    }

    @BeforeClass
    public static void setUpEncoding()
    {
        originalEncoding = System.getProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY);
        System.setProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY, "UTF-8");
    }

    @AfterClass
    public static void restoreEncoding()
    {
        if (originalEncoding == null)
        {
            System.clearProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY);
        }
        else
        {
            System.setProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY, originalEncoding);
        }
    }

    /**
     * see EE-1705/MULE-4567
     */
    @Test
    public void testSessionProperties() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        SessionHandler handler = new SerializeAndEncodeSessionHandler();
        MuleSession session = new DefaultMuleSession();

        String string = "bar";
        session.setProperty("fooString", string);

        Date date = new Date(0);
        session.setProperty("fooDate", date);

        List<String> list = createList();
        session.setProperty("fooList", list);

        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MULE_SESSION_PROPERTY);
        message.setInboundProperty(MULE_SESSION_PROPERTY, s);
        session = handler.retrieveSessionInfoFromMessage(message);

        Object obj = session.getProperty("fooString");
        assertTrue(obj instanceof String);
        assertEquals(string, obj);

        obj = session.getProperty("fooDate");
        assertTrue("Object should be a Date but is " + obj.getClass().getName(), obj instanceof Date);
        assertEquals(date, obj);

        obj = session.getProperty("fooList");
        assertTrue("Object should be a List but is " + obj.getClass().getName(), obj instanceof List);
        assertEquals(list, obj);
    }

    /**
     * see EE-1774
     */
    @Test
    public void testNonSerializableSessionProperties() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        MuleSession session = new DefaultMuleSession();
        SessionHandler handler = new SerializeAndEncodeSessionHandler();

        NotSerializableClass clazz = new NotSerializableClass();
        session.setProperty("foo", clazz);
        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MULE_SESSION_PROPERTY);
        message.setInboundProperty(MULE_SESSION_PROPERTY, s);
        session = handler.retrieveSessionInfoFromMessage(message);
        // Property was removed because it could not be serialized
        assertNull(session.getProperty("foo"));
    }

    /**
     * see EE-1820
     */
    @Test
    public void testBackwardsCompatibility() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        SessionHandler legacyHandler = new LegacySessionHandler();
        MuleSession session = new DefaultMuleSession();

        String string = "bar";
        session.setProperty("fooString", string);

        Date date = new Date(0);
        session.setProperty("fooDate", date);

        List<String> list = createList();
        session.setProperty("fooList", list);

        legacyHandler.storeSessionInfoToMessage(session, message);
        try
        {
            // Try to deserialize legacy format with new session handler
            session = new SerializeAndEncodeSessionHandler().retrieveSessionInfoFromMessage(message);
        }
        catch (SerializationException e)
        {
            // expected
        }
        session = legacyHandler.retrieveSessionInfoFromMessage(message);
    }

    /**
     * see EE-1820
     */
    @Test
    public void testSessionPropertiesLegacyFormat() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        SessionHandler handler = new LegacySessionHandler();
        MuleSession session = new DefaultMuleSession();

        String string = "bar";
        session.setProperty("fooString", string);

        Date date = new Date(0);
        session.setProperty("fooDate", date);

        List<String> list = createList();
        session.setProperty("fooList", list);

        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MULE_SESSION_PROPERTY);
        message.setInboundProperty(MULE_SESSION_PROPERTY, s);
        session = handler.retrieveSessionInfoFromMessage(message);

        Object obj = session.getProperty("fooString");
        assertTrue(obj instanceof String);
        assertEquals(string, obj);

        obj = session.getProperty("fooDate");
        // MULE-4567 / EE-1705 
        assertTrue(obj instanceof String);

        obj = session.getProperty("fooList");
        // MULE-4567 / EE-1705 
        assertTrue(obj instanceof String);
    }

    /**
     * see MULE-4720
     */
    @Test
    public void testSecurityContext() throws Exception
    {
        DefaultMuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        SessionHandler handler = new SerializeAndEncodeSessionHandler();
        MuleSession session = new DefaultMuleSession();

        Credentials credentials = new MuleCredentials("joe", "secret".toCharArray());
        SecurityContext sc = new DefaultSecurityContextFactory().create(new DefaultMuleAuthentication(credentials));
        session.setSecurityContext(sc);

        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MULE_SESSION_PROPERTY);
        message.setInboundProperty(MULE_SESSION_PROPERTY, s);
        session = handler.retrieveSessionInfoFromMessage(message);

        sc = session.getSecurityContext();
        assertEquals("joe", sc.getAuthentication().getPrincipal());
    }

    /**
     * see EE-1774
     */
    @Test
    public void testNotSerializableSecurityContext() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        SessionHandler handler = new SerializeAndEncodeSessionHandler();
        MuleSession session = new DefaultMuleSession();

        session.setSecurityContext(new NotSerializableSecurityContext());

        try
        {
            handler.storeSessionInfoToMessage(session, message);
            fail("Should throw a SerializationException");
        }
        catch (SerializationException e)
        {
            // expected
        }
    }

    @Test
    public void testDontDeserializeOtherThanMuleSession() throws MuleException
    {
        DefaultMuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        SessionHandler handler = new SerializeAndEncodeSessionHandler();

        String encodedSet = new String(encode(serialize(new HashSet<String>())));

        message.setInboundProperty(MULE_SESSION_PROPERTY, encodedSet);

        expectedException.expect(SerializationException.class);
        expectedException.expectMessage("Expecting an instance of 'org.mule.api.MuleSession' but 'java.util.HashSet' found");

        MuleSession session = handler.retrieveSessionInfoFromMessage(message);
        assertNull(session);
    }

    private List<String> createList()
    {
        List<String> list = new ArrayList<String>();
        list.add("bar1");
        list.add("bar2");
        return list;
    }

    private class NotSerializableClass
    {

        public NotSerializableClass()
        {
            super();
        }
    }

    private class NotSerializableSecurityContext implements SecurityContext
    {

        public NotSerializableSecurityContext()
        {
            super();
        }

        public void setAuthentication(Authentication authentication)
        {
            // nothing to do
        }

        public Authentication getAuthentication()
        {
            return null;
        }
    }

}
