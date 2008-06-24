/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.StringAppendTestTransformer;

public class TransformerAttributeTestCase extends FunctionalTestCase
{

    public static final String OUTBOUND_MESSAGE = "Test message";

    protected String getConfigResources()
    {
        return "vm/transformer-attribute-test.xml";
    }

    public void testSimple() throws Exception
    {
        MuleMessage message = new MuleClient().send("vm://simple", OUTBOUND_MESSAGE, null);
        assertNotNull(message);
        assertEquals(StringAppendTestTransformer.appendDefault(OUTBOUND_MESSAGE)  + " Received",
                message.getPayloadAsString());
    }

    public void testThrough() throws Exception
    {
        MuleMessage message = new MuleClient().send("vm://chained", OUTBOUND_MESSAGE, null);
        assertNotNull(message);
        assertEquals(StringAppendTestTransformer.appendDefault(OUTBOUND_MESSAGE)  + " Received",
                message.getPayloadAsString());
    }

}
