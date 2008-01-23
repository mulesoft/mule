/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.issues;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class JmsRemoteSyncMule2868TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "jms-remote-sync-mule2868.xml";
    }

    public void testMule2868() throws MuleException
    {
        MuleClient muleClient = new MuleClient();
        MuleMessage response = muleClient.send("vm://in", "test", null);
        assertEquals("test", response.getPayload());
    }

}
