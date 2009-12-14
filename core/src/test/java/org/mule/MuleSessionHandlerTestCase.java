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
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @see EE-1705
 */
public class MuleSessionHandlerTestCase extends AbstractMuleTestCase
{
    public void testStoreRetrieveSessionInfo() throws Exception 
    {
        MuleMessage message = new DefaultMuleMessage("Test Message", muleContext);
        MuleSessionHandler handler = new MuleSessionHandler();
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
        handler.retrieveSessionInfoFromMessage(message, session);
        
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
}


