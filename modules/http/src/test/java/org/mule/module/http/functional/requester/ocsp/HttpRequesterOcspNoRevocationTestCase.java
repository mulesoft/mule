/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester.ocsp;

import static java.util.Arrays.asList;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpRequesterOcspNoRevocationTestCase extends AbstractHttpOcspRevocationTestCase
{

    public HttpRequesterOcspNoRevocationTestCase(String configFile, String certAlias)
    {
        super(configFile, VALID_OCSP_LIST, certAlias);
    }


    @Parameters
    public static Collection<Object[]> data()
    {
        return asList(new Object[][]
                              {
                                      { "http-requester-ocsp-revocation-config.xml", null },
                                      { "http-requester-ocsp-revocation-custom-provider-config.xml", "server" },
                                      { "http-requester-ocsp-revocation-custom-provider-config.xml", "thirdParty" }
                              });
    }

    @Test
    public void testNoRevokedEntity() throws Exception
    {
        verifyNotRevokedEntity();
    }

}
