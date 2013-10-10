/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import org.junit.Test;

public class FilteringXmlMessageSplitterFunctionalTestCase extends AbstractXmlSplitterOutboundFunctionalTestCase
{
    public FilteringXmlMessageSplitterFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);

    }

    @Test
    public void testSplit() throws Exception
    {
        doSend("split");
        assertService(SPLITTER_ENDPOINT_PREFIX, 1, SERVICE_SPLITTER);
        assertService(SPLITTER_ENDPOINT_PREFIX, 2, ROUND_ROBIN_DET);
    }
}
