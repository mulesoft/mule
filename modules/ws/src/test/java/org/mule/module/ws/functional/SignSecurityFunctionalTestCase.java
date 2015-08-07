/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.functional;

import org.junit.Test;


public class SignSecurityFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "sign-security-config.xml";
    }

    @Test
    public void requestWithSignatureReturnsExpectedResult() throws Exception
    {
        assertValidResponse("vm://requestWithSignature");
    }

    @Test
    public void requestWithoutSignatureFails() throws Exception
    {
        assertSoapFault("vm://requestWithoutSignature", "InvalidSecurity");
    }

}
