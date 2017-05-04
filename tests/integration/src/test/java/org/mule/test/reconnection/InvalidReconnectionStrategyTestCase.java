/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.reconnection;

import org.junit.Test;
import org.mule.config.spring.AbstractSchemaValidationTestCase;
import org.xml.sax.SAXException;

public class InvalidReconnectionStrategyTestCase extends AbstractSchemaValidationTestCase
{

    @Test(expected = SAXException.class)
    public void testInvalidReconnectStrategyWithinInboundEndpoint() throws Exception
    {
        addSchema("http://www.mulesoft.org/schema/mule/ftp","META-INF/mule-ftp.xsd");
        doTest("org/mule/test/reconnection/invalid-reconnection-within-inbound-endpoint-config.xml");
    }

    @Test(expected = SAXException.class)
    public void testInvalidReconnectStrategyWithinOutboundEndpoint() throws Exception
    {
        addSchema("http://www.mulesoft.org/schema/mule/ftp","META-INF/mule-ftp.xsd");
        doTest("org/mule/test/reconnection/invalid-reconnection-within-outbound-endpoint-config.xml");
    }

}
