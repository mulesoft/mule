/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.vm;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.functional.StringAppendTestTransformer;
import org.mule.umo.UMOMessage;

public class TransformerAttributeTestCase extends FunctionalTestCase
{

    public static final String OUTBOUND_MESSAGE = "Test message";

    protected String getConfigResources()
    {
        return "vm/transformer-attribute-test.xml";
    }

    public void testSimple() throws Exception
    {
        UMOMessage message = new MuleClient().send("vm://simple", OUTBOUND_MESSAGE, null);
        assertNotNull(message);
        assertEquals(
                FunctionalTestComponent.received(
                        StringAppendTestTransformer.appendDefault(OUTBOUND_MESSAGE)),
                message.getPayloadAsString());
    }

    public void testThrough() throws Exception
    {
        UMOMessage message = new MuleClient().send("vm://chained", OUTBOUND_MESSAGE, null);
        assertNotNull(message);
        assertEquals(
                FunctionalTestComponent.received(
                        StringAppendTestTransformer.appendDefault(OUTBOUND_MESSAGE)),
                message.getPayloadAsString());
    }

}
