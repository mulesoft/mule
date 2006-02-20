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
package org.mule.test.usecases.sync;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class TcpToFileTestCase extends FunctionalTestCase {

    protected String getConfigResources() {
        return "tcp-to-file.xml";
    }

    public void testSyncResponse() throws Exception
    {
        MuleClient client = new MuleClient();
        client.sendNoReceive("tcp://localhost:4444", "payload", null);
        Thread.sleep(1000);
        UMOMessage msg = client.receive("file://C:/temp/tests/mule", 5000);
        assertNotNull(msg);
    }
}
