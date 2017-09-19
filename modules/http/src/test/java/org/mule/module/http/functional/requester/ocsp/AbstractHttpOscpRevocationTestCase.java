/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester.ocsp;

import static java.lang.String.format;
import org.mule.module.http.functional.AbstractHttpTlsRevocationTestCase;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;

public abstract class AbstractHttpOscpRevocationTestCase extends AbstractHttpTlsRevocationTestCase
{


    private static String RUN_OCSP_SERVER_COMMAND = "openssl ocsp " +
                                                    "-index src/test/resources/tls/ocsp/%s " +
                                                    "-CA src/test/resources/tls/ocsp/server.crt " +
                                                    "-rkey src/test/resources/tls/ocsp/server_key.pem " +
                                                    "-port 1111 " +
                                                    "-rsigner src/test/resources/tls/ocsp/server.crt";

    static String REVOKED_OCSP_LIST = "revoked-ocsp.txt";

    static String VALID_OCSP_LIST = "valid-ocsp.txt";

    /**
     *  This certified entity was generated to test revocation with OCSP mechanism.
     */
    static String ENTITY_CERTIFIED_REVOCATION_OCSP_SUB_PATH = "entity4";

    private Process process ;

    private final String ocspList ;


    AbstractHttpOscpRevocationTestCase(String entityCertified, String ocspList)
    {
        super("http-requester-ocsp-revocation-config.xml", entityCertified);
        this.ocspList = ocspList;
    }


    @Before
    public void setUp() throws Exception
    {
        process = Runtime.getRuntime().exec(format(RUN_OCSP_SERVER_COMMAND, ocspList));
    }

    @After
    public void tearDown() throws IOException
    {
        process.destroy();
    }

}
