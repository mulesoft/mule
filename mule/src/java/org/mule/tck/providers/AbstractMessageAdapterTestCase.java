/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.tck.providers;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * @author Ross Mason <p/> //TODO document
 */
public abstract class AbstractMessageAdapterTestCase extends AbstractMuleTestCase
{
    public void testMessageRetrieval() throws Exception
    {
        Object message = getValidMessage();
        UMOMessageAdapter adapter = createAdapter(message);

        assertEquals(message, adapter.getPayload());
        byte[] bytes = adapter.getPayloadAsBytes();
        assertNotNull(bytes);

        String stringMessage = adapter.getPayloadAsString();
        assertNotNull(stringMessage);

        assertNotNull(adapter.getPayload());

        try {
            adapter = createAdapter(getInvalidMessage());
            fail("Message adapter should throw exception if an invalid messgae is set");
        } catch (Exception e) {
            // expected
        }
    }

    public void testMessageProps() throws Exception
    {
        UMOMessageAdapter adapter = createAdapter(getValidMessage());

        adapter.setProperty("TestString", "Test1");
        adapter.setProperty("TestLong", new Long(20000000));
        adapter.setProperty("TestInt", new Integer(200000));
        Object prop;
        assertNotNull(adapter.getPropertyNames());

        prop = adapter.getProperty("TestString");
        assertNotNull(prop);
        assertEquals("Test1", prop);

        prop = adapter.getProperty("TestLong");
        assertNotNull(prop);
        assertEquals(new Long(20000000), prop);

        prop = adapter.getProperty("TestInt");
        assertNotNull(prop);
        assertEquals(new Integer(200000), prop);
    }

    public Object getInvalidMessage()
    {
        return new InvalidMessage();
    }

    public abstract Object getValidMessage() throws Exception;

    public abstract UMOMessageAdapter createAdapter(Object payload) throws Exception;

    final class InvalidMessage
    {
        public String toString()
        {
            return "invalid message";
        }
    }

}
