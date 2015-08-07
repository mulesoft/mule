/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.functional;

import org.junit.Test;


public class DecryptSecurityFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "decrypt-security-config.xml";
    }

    @Test
    public void responseDecryptedWithValidKeyReturnsExpectedResult() throws Exception
    {
        assertValidResponse("vm://responseValidKey");
    }

    @Test
    public void responseDecryptedWithInvalidKeyFails() throws Exception
    {
        assertSoapFault("vm://responseInvalidKey", "Client");
    }

    @Test
    public void responseNotEncryptedFailsToDecrypt() throws Exception
    {
        assertSoapFault("vm://responseNoEncryption", "InvalidSecurity");
    }




}
