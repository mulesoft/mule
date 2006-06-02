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
package org.mule.test.integration.routing.replyto;

import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ReplytoChainIntegration2TestCase extends FunctionalTestCase
{
    public ReplytoChainIntegration2TestCase() {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources() {
        return "org/mule/test/integration/routing/replyto/injection2-test.xml";
    }

    public void testReplyToChain() throws Exception
    {
        String message = "test";

        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, "false");
        UMOMessage result = client.send("vm://pojo1", message, props);
        assertNotNull(result);
        // Te
        assertEquals("Received: " + message, result.getPayload());
    }
}
