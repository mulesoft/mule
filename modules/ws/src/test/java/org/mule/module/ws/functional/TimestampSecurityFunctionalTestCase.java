/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;

import org.junit.Test;

public class TimestampSecurityFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "timestamp-security-config.xml";
    }

    @Test
    public void requestWithTimestampReturnsExpectedResult() throws Exception
    {
        assertValidResponse("vm://clientWithTimestamp");
    }

    @Test
    public void requestWithoutTimestampFail() throws Exception
    {
        assertSoapFault("vm://clientWithoutTimestamp", "InvalidSecurity");
    }

}
