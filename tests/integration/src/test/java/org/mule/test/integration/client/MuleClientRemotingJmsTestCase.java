/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
