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
package org.mule.test.usecases.routing;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ForwardingMessageSplitterTestCase extends FunctionalTestCase
{
    protected String getConfigResources() {
        return "forwarding-message-splitter.xml";
    }

    public void testSyncResponse() throws Exception
    {
        MuleClient client = new MuleClient();

        List payload = new ArrayList();
        payload.add("hello");
        payload.add(new Integer(3));
        payload.add(new Exception());
        client.send("vm://in.queue", payload, null);
        UMOMessage m = client.receive("vm://component.1", 2000);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        m = client.receive("vm://component.2", 2000);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof Integer);

        m = client.receive("vm://error.queue", 2000);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof Exception);
    }
}
