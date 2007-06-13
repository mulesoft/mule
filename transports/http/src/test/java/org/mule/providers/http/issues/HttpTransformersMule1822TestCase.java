/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.issues;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.functional.StringAppendTestTransformer;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

public class HttpTransformersMule1822TestCase extends FunctionalTestCase
{

    public static final String OUTBOUND_MESSAGE = "Test message";

    protected String getConfigResources()
    {
        return "http-transformers-mule-1822-test.xml";
    }

    private UMOMessage sendTo(String uri) throws UMOException
    {
        MuleClient client = new MuleClient();
        UMOMessage message = client.send(uri, OUTBOUND_MESSAGE, null);
        assertNotNull(message);
        return message;
    }

    /**
     * With no transformer we expect just the modification from the FTC
     *
     * @throws Exception
     */
    public void testBase() throws Exception
    {
        assertEquals(
                FunctionalTestComponent.received(OUTBOUND_MESSAGE),
                sendTo("base").getPayloadAsString());
    }

    /**
     * But response transformers on the base model should be applied
     *
     * @throws Exception
     */
    public void testResponse() throws Exception
    {
        assertEquals(
                StringAppendTestTransformer.append(" response",
                        StringAppendTestTransformer.append(" response 2",
                                FunctionalTestComponent.received(
                                        OUTBOUND_MESSAGE))),
                sendTo("response").getPayloadAsString());
    }

    /**
     * Shouldalso work with inbound transformers
     *
     * @throws Exception
     */
    public void testBoth() throws Exception
    {
        assertEquals(
                StringAppendTestTransformer.append(" response",
                        StringAppendTestTransformer.append(" response 2",
                                FunctionalTestComponent.received(
                                        StringAppendTestTransformer.append(" transformed 2",
                                                StringAppendTestTransformer.appendDefault(
                                        OUTBOUND_MESSAGE))))),
                sendTo("both").getPayloadAsString());
    }

}