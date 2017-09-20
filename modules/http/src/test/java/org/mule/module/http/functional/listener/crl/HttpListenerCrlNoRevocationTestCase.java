/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener.crl;

import org.junit.Test;

public class HttpListenerCrlNoRevocationTestCase extends AbstractHttpListenerClrTestCase
{

    public HttpListenerCrlNoRevocationTestCase(String configFile)
    {
        super(configFile, EMPTY_CRL_FILE_PATH, ENTITY_CERTIFIED_NO_REVOCATION_SUB_PATH);
    }

    @Test
    public void testNotRevokedClient() throws Exception
    {
        verifyNotRevokedEntity();
    }

}
