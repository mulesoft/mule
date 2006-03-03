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
 */

package org.mule.providers.http;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.provider.UMOMessageAdapter;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class HttpMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{
    protected static final String TEST_MESSAGE = "Hello";

    private byte[] message = TEST_MESSAGE.getBytes();

    public Object getValidMessage() throws Exception
    {
        return message;
    }

    public UMOMessageAdapter createAdapter(Object payload) throws Exception
    {
        return new HttpMessageAdapter(payload);
    }

    public void testMessageRetrieval() throws Exception
    {
        Object message = getValidMessage();
        UMOMessageAdapter adapter = createAdapter(message);

        assertEquals("Hello", adapter.getPayloadAsString());
        byte[] bytes = adapter.getPayloadAsBytes();
        assertNotNull(bytes);

        String stringMessage = adapter.getPayloadAsString();
        assertNotNull(stringMessage);

        assertNotNull(adapter.getPayload());

        try {
            adapter = createAdapter(getInvalidMessage());
            fail("Message adapter should throw exception if an invalid messgae is set");
        }
        catch (Exception e) {
            // expected
        }
    }
}
