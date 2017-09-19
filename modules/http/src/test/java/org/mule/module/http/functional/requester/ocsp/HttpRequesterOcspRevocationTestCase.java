/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester.ocsp;

import static java.lang.String.format;
import static org.junit.Assert.fail;
import org.mule.module.http.functional.AbstractHttpTlsRevocationTestCase;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class HttpRequesterOcspRevocationTestCase extends AbstractHttpTlsRevocationTestCase
{

    private Process process ;
    private final Latch latch = new Latch();

    public HttpRequesterOcspRevocationTestCase()
    {
        super("http-requester-ocsp-revocation-config.xml", ENTITY_CERTIFIED_REVOCATION_OCSP_SUB_PATH);
    }

    @Before
    public void setUp() throws Exception
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    process = Runtime.getRuntime().exec(format(RUN_OCSP_SERVER_COMMAND, REVOKED_OCSP_LIST));
                    StringWriter output = new StringWriter();
                    int outputInt ;

                    while ((outputInt = process.getErrorStream().read()) != 10)
                    {
                        output.write(outputInt);
                    }
                    if(output.toString().contains("Error"))
                    {
                        System.out.println("ERROR");
                    }
                    latch.countDown();
                }
                catch (IOException e)
                {
                    fail("There was an error trying to start to star the ocsp server.");
                }
            }
        }).start();
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

    @After
    public void tearDown() throws IOException
    {
       process.destroy();
    }
}
