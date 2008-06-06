/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.streaming;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class JmsStreamMessageTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/streaming/jms-stream-message.xml";
    }

    public void testStreamMessage() throws MuleException
    {
        MuleClient client = new MuleClient();
        MuleMessage response = client.send("http://localhost:8080/services", "test", null);
        assertNull(response.getExceptionPayload());
    }
    
}


