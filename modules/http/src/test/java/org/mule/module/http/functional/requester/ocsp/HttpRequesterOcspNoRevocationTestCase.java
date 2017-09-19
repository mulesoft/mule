/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester.ocsp;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class HttpRequesterOcspNoRevocationTestCase extends AbstractHttpOscpRevocationTestCase
{

    public HttpRequesterOcspNoRevocationTestCase()
    {
        super(ENTITY_CERTIFIED_REVOCATION_OCSP_SUB_PATH, VALID_OCSP_LIST);
    }

    @Test
    public void testNoRevokedEntity() throws Exception
    {
        verifyNotRevokedEntity();
    }

}
