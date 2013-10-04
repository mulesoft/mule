/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;



public class MuleClientRemotingJmsTestCase extends AbstractClientRemotingTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/client/client-remote-dispatcher-common-config.xml, " +
                "org/mule/test/integration/client/test-client-mule-config-remote-jms.xml";
    }

    @Override
    public String getRemoteEndpointUri()
    {
        return "jms://mule.sys.queue";
    }

}
