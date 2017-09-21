/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.functional.requester.ocsp;

import static java.lang.String.format;
import static org.junit.Assume.assumeFalse;
import org.mule.module.http.functional.AbstractHttpTlsRevocationTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractHttpOcspRevocationTestCase extends AbstractHttpTlsRevocationTestCase
{

    @Rule
    public DynamicPort dynamicOcspPort ;

    @Rule
    public SystemProperty certAliasSystemProperty ;

    private static String RUN_OCSP_SERVER_COMMAND = "openssl ocsp " +
                                                    "-index src/test/resources/tls/ocsp/%s " +
                                                    "-port %d " +
                                                    "-CA src/test/resources/tls/ocsp/server.crt " +
                                                    "-rkey src/test/resources/tls/ocsp/%s.pem " +
                                                    "-rsigner src/test/resources/tls/ocsp/%s.crt";

    static final String REVOKED_OCSP_LIST = "revoked-ocsp.txt";

    static final String VALID_OCSP_LIST = "valid-ocsp.txt";

    /**
     *  This certified entity was generated to test revocation with OCSP mechanism.
     */
    protected static final String ENTITY_CERTIFIED_REVOCATION_OCSP_SUB_PATH = "entityOcsp1";

    /**
     * The entity4 certificate has a hardcoded ocsp url: http://localhost:1111
     */
    static final int FIXED_OCSP_PORT = 1111;


    static final String DEFAULT_OCSP_RESPONDER= "server";

    private Process process ;

    private final String ocspList ;

    private int ocspPort;

    private String ocspResponder ;


    AbstractHttpOcspRevocationTestCase(String configFile, String ocspList, String certAlias)
    {
        super(configFile, ENTITY_CERTIFIED_REVOCATION_OCSP_SUB_PATH);
        this.ocspList = ocspList;
        initialiseOcspStatus(certAlias);
    }

    private void initialiseOcspStatus (String certAlias)
    {
        if (certAlias != null)
        {
            certAliasSystemProperty = new SystemProperty("certAlias", certAlias);
            dynamicOcspPort = new DynamicPort("dynamicOcspPort");
            ocspPort = dynamicOcspPort.getNumber();
            ocspResponder = certAliasSystemProperty.getValue();
        }
        else
        {
            ocspPort = FIXED_OCSP_PORT ;
            ocspResponder = DEFAULT_OCSP_RESPONDER;
        }
    }


    @Before
    public void setUp() throws Exception
    {
        // Running the ocsp server in a fixed port causes that sometimes the server fails at initialisation.
        assumeFalse("Since openssl ocsp command has a flaky behavior the test will be ignored when the port number is not dynamic.", FIXED_OCSP_PORT == ocspPort);
        process = Runtime.getRuntime().exec(format(RUN_OCSP_SERVER_COMMAND, ocspList, ocspPort, ocspResponder, ocspResponder));
    }

    @After
    public void tearDown() throws IOException
    {
        if(process != null)
        {
            process.destroy();
        }
    }

}
