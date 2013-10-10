/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import org.mule.api.MuleException;

import java.io.IOException;

import org.junit.Test;

public class RoundRobinXmlSplitterFunctionalTestCase extends AbstractXmlSplitterOutboundFunctionalTestCase
{
    public RoundRobinXmlSplitterFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);

    }

    @Test
    public void testSimple() throws MuleException, IOException
    {
        doSend("roundrobin-det");
        assertServices(ROUND_ROBIN_ENDPOINT_PREFIX, 1,  new String[]{SERVICE_SPLITTER, ROUND_ROBIN_INDET});
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 2, ROUND_ROBIN_DET);
    }

    @Test
    public void testDeterministic() throws MuleException, IOException
    {
        doSend("roundrobin-det");
        assertServices(ROUND_ROBIN_ENDPOINT_PREFIX, 1,  new String[]{SERVICE_SPLITTER, ROUND_ROBIN_INDET});
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 2, ROUND_ROBIN_DET);
        doSend("roundrobin-det");
        assertServices(ROUND_ROBIN_ENDPOINT_PREFIX, 1, new String[]{SERVICE_SPLITTER, ROUND_ROBIN_INDET});
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 2, ROUND_ROBIN_DET);
        doSend("roundrobin-det");
        doSend("roundrobin-det");
        assertServices(ROUND_ROBIN_ENDPOINT_PREFIX, 1, new String[]{SERVICE_SPLITTER, ROUND_ROBIN_INDET, SERVICE_SPLITTER, ROUND_ROBIN_INDET});
        assertServices(ROUND_ROBIN_ENDPOINT_PREFIX, 2, new String[]{ROUND_ROBIN_DET, ROUND_ROBIN_DET});
    }

    @Test
    public void testIndeterministic() throws MuleException, IOException
    {
        doSend("roundrobin-indet");
        assertServices(ROUND_ROBIN_ENDPOINT_PREFIX, 1,  new String[]{SERVICE_SPLITTER, ROUND_ROBIN_INDET});
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 2, ROUND_ROBIN_DET);
        doSend("roundrobin-indet");
        assertServices(ROUND_ROBIN_ENDPOINT_PREFIX, 2,  new String[]{SERVICE_SPLITTER, ROUND_ROBIN_INDET});
        assertService(ROUND_ROBIN_ENDPOINT_PREFIX, 1, ROUND_ROBIN_DET);
    }
}
