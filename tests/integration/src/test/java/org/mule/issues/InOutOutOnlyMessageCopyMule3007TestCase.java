/*
 * $Id: JmsStreamMessageTestCase.java 11700 2008-05-08 11:16:01Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.issues;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class InOutOutOnlyMessageCopyMule3007TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/issues/inout-outonly-message-copy-mule3007-test.xml";
    }

    public void testStreamMessage() throws MuleException
    {
        MuleClient client = new MuleClient();
        MuleMessage response = client.send("http://localhost:38900/services", "test", null);
        assertNull(response.getExceptionPayload());
    }
    
}


