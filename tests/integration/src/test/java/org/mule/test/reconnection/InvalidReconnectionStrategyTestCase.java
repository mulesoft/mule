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

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.containsString;


public class InvalidReconnectionStrategyTestCase extends AbstractSchemaValidationTestCase
{
    private static final String ERROR_MESSAGE  = "Invalid content was found starting with element 'reconnect'";

    @Test
    public void testInvalidReconnectStrategyWithinInboundEndpoint() throws Exception
    {
        addSchema("http://www.mulesoft.org/schema/mule/ftp","META-INF/mule-ftp.xsd");
        try
        {
            doTest("org/mule/test/reconnection/invalid-reconnection-within-inbound-endpoint-config.xml");
            fail("SaxException must be triggered, because it's an invalid configuration.");
        }
        catch (SAXException e)
        {
            assertThat(e.getMessage(), containsString(ERROR_MESSAGE));
        }
    }

    @Test
    public void testInvalidReconnectStrategyWithinOutboundEndpoint() throws Exception
    {
        addSchema("http://www.mulesoft.org/schema/mule/ftp","META-INF/mule-ftp.xsd");
        try
        {
            doTest("org/mule/test/reconnection/invalid-reconnection-within-outbound-endpoint-config.xml");
            fail("SaxException must be triggered, because it's an invalid configuration.");
        }
        catch (SAXException e)
        {
            assertThat(e.getMessage(), containsString(ERROR_MESSAGE));
        }
    }

}
