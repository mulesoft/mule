/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import org.junit.Ignore;

@Ignore("MULE-3000")
public class MuleClientRemotingTcpTestCase extends AbstractClientRemotingTestCase
{
    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
            "org/mule/test/integration/client/client-remote-dispatcher-common-config.xml",
            "org/mule/test/integration/client/test-client-mule-config-remote-tcp.xml"
        };
    }

    @Override
    public String getRemoteEndpointUri()
    {
        return "tcp://localhost:60504";
    }
}

