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
import org.mule.tck.functional.StringAppendTestTranformer;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

public class HttpTransformersMule1815TestCase extends FunctionalTestCase
{

    public static final String OUTBOUND_MESSAGE = "Test message";

    protected String getConfigResources()
    {
        return "http-transformers-mule-1815-test.xml";
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
     * Adapted model, which should not apply transformers
     *
     * @throws Exception
     */
    public void testAdapted() throws Exception
    {
        assertEquals(
                FunctionalTestComponent.received(OUTBOUND_MESSAGE),
                sendTo("adapted").getPayloadAsString());
    }

    /**
     * Transformers on the adapted model should be ignored
     *
     * @throws Exception
     */
    public void testIgnored() throws Exception
    {
        assertEquals(
                FunctionalTestComponent.received(OUTBOUND_MESSAGE),
                sendTo("ignored").getPayloadAsString());
    }

    /**
     * But transformers on the base model should be applied
     *
     * @throws Exception
     */
    public void testTransformed() throws Exception
    {
        assertEquals(
                // this reads backwards - innermost is first in chain
                FunctionalTestComponent.received(
                        StringAppendTestTranformer.append(" transformed 2",
                                StringAppendTestTranformer.appendDefault(
                                        OUTBOUND_MESSAGE))),
                sendTo("transformed").getPayloadAsString());
    }

}
