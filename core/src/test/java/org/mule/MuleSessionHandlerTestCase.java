/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.apache.commons.lang.SerializationUtils.serialize;
import static org.apache.xmlbeans.impl.util.Base64.encode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.api.config.MuleProperties.MULE_SESSION_PROPERTY;
import static org.mule.session.AbstractSessionHandler.ACTIVATE_NATIVE_SESSION_SERIALIZATION_PROPERTY;
import static org.mule.tck.SerializationTestUtils.addJavaSerializerToMockMuleContext;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.serialization.SerializationException;
import org.mule.api.transport.SessionHandler;
import org.mule.session.AbstractSessionHandler;
import org.mule.session.DefaultMuleSession;
import org.mule.session.LegacySessionHandler;
import org.mule.session.SerializeAndEncodeSessionHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import com.google.common.base.Charsets;

@Ignore("This test has to be run alone, because the system property it sets invalidates the run of SerializeAndEncodeSessionHandlerTestCase afterwards")
public class MuleSessionHandlerTestCase extends AbstractMuleTestCase
{

    private static String originalEncoding;

    private MuleContext muleContext;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private SerializeAndEncodeSessionHandler handler;
    private DefaultMuleMessage message;

    @ClassRule
    public static SystemProperty sessionSignKey = new SystemProperty(ACTIVATE_NATIVE_SESSION_SERIALIZATION_PROPERTY, "true");

    @Before
    public void setUp() throws Exception
    {
        muleContext = mock(MuleContext.class);
        MuleConfiguration configuration = mock(MuleConfiguration.class);
        when(configuration.getDefaultEncoding()).thenReturn(Charsets.UTF_8.name());
        when(muleContext.getConfiguration()).thenReturn(configuration);
        when(muleContext.getExecutionClassLoader()).thenReturn(Thread.currentThread().getContextClassLoader());
        addJavaSerializerToMockMuleContext(muleContext);

        handler = new SerializeAndEncodeSessionHandler();

        message = new DefaultMuleMessage("Test Message", muleContext);
        message.setInboundProperty("MULE_ENDPOINT", "http://whatever");
    }

    @BeforeClass
    public static void setUpEncoding()
    {
        originalEncoding = System.getProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY);
        setProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY, "UTF-8");
    }

    @AfterClass
    public static void restoreEncoding()
    {
        if (originalEncoding == null)
        {
            clearProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY);
        }
        else
        {
            setProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY, originalEncoding);
        }
    }

    @Test
    public void testPrimitiveTypesInSessionProperties() throws Exception
    {
        MuleSession session = new DefaultMuleSession();

        int anInteger = 1;
        session.setProperty("anInteger", anInteger);

        boolean aBoolean = true;
        session.setProperty("aBoolean", aBoolean);

        char aCharacter = 'a';
        session.setProperty("aCharacter", aCharacter);

        short aShort = 2;
        session.setProperty("aShort", aShort);

        long aLong = 3;
        session.setProperty("aLong", aLong);

        float aFloat = 4;
        session.setProperty("aFloat", aFloat);

        double aDouble = 5;
        session.setProperty("aDouble", aDouble);

        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MULE_SESSION_PROPERTY);
        message.setInboundProperty(MULE_SESSION_PROPERTY, s);

        Object obj = session.getProperty("anInteger");
        assertTrue(obj instanceof Integer);
        assertEquals(obj, anInteger);

        obj = session.getProperty("aBoolean");
        assertTrue(obj instanceof Boolean);
        assertEquals(obj, aBoolean);

        obj = session.getProperty("aCharacter");
        assertTrue(obj instanceof Character);
        assertEquals(obj, aCharacter);

        obj = session.getProperty("aShort");
        assertTrue(obj instanceof Short);
        assertEquals(obj, aShort);

        obj = session.getProperty("aLong");
        assertTrue(obj instanceof Long);
        assertEquals(obj, aLong);

        obj = session.getProperty("aFloat");
        assertTrue(obj instanceof Float);
        assertEquals(obj, aFloat);

        obj = session.getProperty("aDouble");
        assertTrue(obj instanceof Double);
        assertEquals(obj, aDouble);
    }

    @Test
    public void testPrimitiveWrappersInSessionProperties() throws Exception
    {
        MuleSession session = new DefaultMuleSession();

        Integer anInteger = 1;
        session.setProperty("anInteger", anInteger);

        Boolean aBoolean = true;
        session.setProperty("aBoolean", aBoolean);

        Character aCharacter = 'a';
        session.setProperty("aCharacter", aCharacter);

        Short aShort = 2;
        session.setProperty("aShort", aShort);

        Long aLong = Long.valueOf(3);
        session.setProperty("aLong", aLong);

        Float aFloat = Float.valueOf(4);
        session.setProperty("aFloat", aFloat);

        Double aDouble = Double.valueOf(5);
        session.setProperty("aDouble", aDouble);

        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MULE_SESSION_PROPERTY);
        message.setInboundProperty(MULE_SESSION_PROPERTY, s);

        Object obj = session.getProperty("anInteger");
        assertTrue(obj instanceof Integer);
        assertEquals(obj, anInteger);

        obj = session.getProperty("aBoolean");
        assertTrue(obj instanceof Boolean);
        assertEquals(obj, aBoolean);

        obj = session.getProperty("aCharacter");
        assertTrue(obj instanceof Character);
        assertEquals(obj, aCharacter);

        obj = session.getProperty("aShort");
        assertTrue(obj instanceof Short);
        assertEquals(obj, aShort);

        obj = session.getProperty("aLong");
        assertTrue(obj instanceof Long);
        assertEquals(obj, aLong);

        obj = session.getProperty("aFloat");
        assertTrue(obj instanceof Float);
        assertEquals(obj, aFloat);

        obj = session.getProperty("aDouble");
        assertTrue(obj instanceof Double);
        assertEquals(obj, aDouble);
    }

    @Test
    public void testNonPrimitiveAllowedSessionProperties() throws Exception
    {
        MuleSession session = new DefaultMuleSession();

        Date date = new Date(0);
        session.setProperty("fooDate", date);

        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MULE_SESSION_PROPERTY);
        message.setInboundProperty(MULE_SESSION_PROPERTY, s);
        session = handler.retrieveSessionInfoFromMessage(message);

        Object obj = session.getProperty("fooDate");
        assertTrue("Object should be a Date but is " + obj.getClass().getName(), obj instanceof Date);
        assertEquals(date, obj);
    }

    @Test
    public void testNullSessionProperty() throws Exception
    {
        MuleSession session = new DefaultMuleSession();

        String nullString = null;
        session.setProperty("nullstr", nullString);

        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MULE_SESSION_PROPERTY);
        message.setInboundProperty(MULE_SESSION_PROPERTY, s);
        session = handler.retrieveSessionInfoFromMessage(message);

        Object obj = session.getProperty("nullstr");
        assertEquals(nullString, obj);
    }

    /**
     * see EE-1705/MULE-4567
     */
    @Test
    public void testSessionProperties() throws Exception
    {
        MuleSession session = new DefaultMuleSession();

        String string = "bar";
        session.setProperty("fooString", string);

        int nativeInteger = 1;
        session.setProperty("nativeInteger", nativeInteger);

        Integer wrappedInteger = 2;
        session.setProperty("wrappedInteger", wrappedInteger);

        Date date = new Date(0);
        session.setProperty("fooDate", date);

        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MULE_SESSION_PROPERTY);
        message.setInboundProperty(MULE_SESSION_PROPERTY, s);
        session = handler.retrieveSessionInfoFromMessage(message);

        Object obj = session.getProperty("fooString");
        assertTrue(obj instanceof String);
        assertEquals(string, obj);

        obj = session.getProperty("nativeInteger");
        assertTrue(obj instanceof Integer);
        assertEquals(obj, 1);

        obj = session.getProperty("wrappedInteger");
        assertTrue(obj instanceof Integer);
        assertEquals(obj, 2);

        obj = session.getProperty("fooDate");
        assertTrue("Object should be a Date but is " + obj.getClass().getName(), obj instanceof Date);
        assertEquals(date, obj);
    }

    @Test
    public void testDontDeserializeOtherThanMuleSession() throws MuleException
    {
        String encodedSet = new String(encode(serialize(new HashSet<String>())));

        message.setInboundProperty(MULE_SESSION_PROPERTY, encodedSet);

        MuleSession session = handler.retrieveSessionInfoFromMessage(message);
        assertNull(session);
    }
    
    /**
     * see EE-1774
     */
    @Test
    public void testNonSerializableSessionProperties() throws Exception
    {
        MuleSession session = new DefaultMuleSession();

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
