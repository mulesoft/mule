/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.Authentication;
import org.mule.api.security.Credentials;
import org.mule.api.security.SecurityContext;
import org.mule.api.transport.PropertyScope;
import org.mule.api.transport.SessionHandler;
import org.mule.security.DefaultMuleAuthentication;
import org.mule.security.DefaultSecurityContextFactory;
import org.mule.security.MuleCredentials;
import org.mule.session.LegacySessionHandler;
import org.mule.session.SerializeAndEncodeSessionHandler;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.SerializationException;

public class MuleSessionHandlerTestCase extends AbstractMuleTestCase
{
    /** @see EE-1705/MULE-4567 */
    public void testSessionProperties() throws Exception 
    {
        MuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        SessionHandler handler = new SerializeAndEncodeSessionHandler();
        MuleSession session = new DefaultMuleSession(muleContext);
        
        String string = "bar";
        session.setProperty("fooString", string);

        Date date = new Date(0);
        session.setProperty("fooDate", date);
        
        List list = new ArrayList();
        list.add("bar1");
        list.add("bar2");
        session.setProperty("fooList", list);
        
        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MuleProperties.MULE_SESSION_PROPERTY);
        message.setProperty(MuleProperties.MULE_SESSION_PROPERTY, s, PropertyScope.INBOUND);
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

    /** @see EE-1774 */
    public void testNonSerializableSessionProperties() throws Exception 
    {
        MuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        MuleSession session = new DefaultMuleSession(muleContext);
        SessionHandler handler = new SerializeAndEncodeSessionHandler();
        
        NotSerializableClass clazz = new NotSerializableClass();
        session.setProperty("foo", clazz);
        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MuleProperties.MULE_SESSION_PROPERTY);
        message.setProperty(MuleProperties.MULE_SESSION_PROPERTY, s, PropertyScope.INBOUND);
        session = handler.retrieveSessionInfoFromMessage(message);
        // Property was removed because it could not be serialized
        assertNull(session.getProperty("foo"));
    }    

    /** @see EE-1820 */
    public void testBackwardsCompatibility() throws Exception 
    {
        MuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        SessionHandler legacyHandler = new LegacySessionHandler();
        MuleSession session = new DefaultMuleSession(muleContext);
        
        String string = "bar";
        session.setProperty("fooString", string);

        Date date = new Date(0);
        session.setProperty("fooDate", date);
        
        List list = new ArrayList();
        list.add("bar1");
        list.add("bar2");
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
    
    /** @see EE-1820 */
    public void testSessionPropertiesLegacyFormat() throws Exception 
    {
        MuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        SessionHandler handler = new LegacySessionHandler();
        MuleSession session = new DefaultMuleSession(muleContext);
        
        String string = "bar";
        session.setProperty("fooString", string);

        Date date = new Date(0);
        session.setProperty("fooDate", date);
        
        List list = new ArrayList();
        list.add("bar1");
        list.add("bar2");
        session.setProperty("fooList", list);

        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MuleProperties.MULE_SESSION_PROPERTY);
        message.setProperty(MuleProperties.MULE_SESSION_PROPERTY, s, PropertyScope.INBOUND);
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

    /** @see MULE-4720 */
    public void testSecurityContext() throws Exception 
    {
        MuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        SessionHandler handler = new SerializeAndEncodeSessionHandler();
        MuleSession session = new DefaultMuleSession(muleContext);
        
        Credentials credentials = new MuleCredentials("joe", "secret".toCharArray());
        SecurityContext sc = new DefaultSecurityContextFactory().create(new DefaultMuleAuthentication(credentials));
        session.setSecurityContext(sc);

        handler.storeSessionInfoToMessage(session, message);
        // store save session to outbound, move it to the inbound
        // for retrieve to deserialize
        Object s = message.removeProperty(MuleProperties.MULE_SESSION_PROPERTY);
        message.setProperty(MuleProperties.MULE_SESSION_PROPERTY, s, PropertyScope.INBOUND);
        session = handler.retrieveSessionInfoFromMessage(message);

        sc = session.getSecurityContext();
        assertEquals("joe", sc.getAuthentication().getPrincipal());
    }    
    
    /** @see EE-1774 */
    public void testNotSerializableSecurityContext() throws Exception 
    {
        MuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        SessionHandler handler = new SerializeAndEncodeSessionHandler();
        MuleSession session = new DefaultMuleSession(muleContext);
        
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
    
    private class NotSerializableClass
    {
        // empty
    };
    
    private class NotSerializableSecurityContext implements SecurityContext
    {
        public void setAuthentication(Authentication authentication) { }

        public Authentication getAuthentication() { return null; }
    }
    
}


