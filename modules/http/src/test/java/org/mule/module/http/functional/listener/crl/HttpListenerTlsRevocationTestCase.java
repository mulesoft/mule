/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener.crl;

import static org.junit.Assert.fail;

import org.junit.Test;

public class HttpListenerTlsRevocationTestCase extends AbstractHttpListenerTlsRevocationTestCase
{

    public HttpListenerTlsRevocationTestCase(String configFile)
    {
        super(configFile, REVOKED_CRL_FILE_PATH, ENTITY_CERTIFIED_3_SUB_PATH);
    }

    @Test
    public void testClientCertifiedAndRevoked() throws Exception
    {
        revocationException = null;
        try
        {
            runRevocationTestFlow();
            fail("Remotely Closed exception has been thrown");
        }
        catch (Exception e)
        {
            verifyRemotelyClosedCause(e);
        }
        finally
        {
            verifyRevocationException(revocationException);
        }
    }

}
