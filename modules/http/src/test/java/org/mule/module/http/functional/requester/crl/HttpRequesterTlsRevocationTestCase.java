/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester.crl;

import static org.junit.Assert.fail;

import org.junit.Test;

public class HttpRequesterTlsRevocationTestCase extends AbstractHttpRequesterTlsRevocationTestCase
{

    public HttpRequesterTlsRevocationTestCase(String configFile)
    {
        super(configFile, REVOKED_CRL_FILE_PATH, ENTITY_CERTIFIED_REVOCATION_SUB_PATH);
    }

    @Test
    public void testServerCertifiedAndRevoked() throws Exception
    {
        try
        {
            runRevocationTestFlow();
            fail("CertificateRevokedException should have been thrown.");
        }
        catch (Exception e)
        {
            verifyRevocationException(e);
        }
    }

}
