/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester.crl;

import static org.apache.commons.lang.exception.ExceptionUtils.getRootCause;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.module.http.functional.AbstractHttpTlsRevocationTestCase;

import java.security.cert.CertPathValidatorException;

import org.junit.Test;

public class HttpPreferringCrlTestCase extends AbstractHttpTlsRevocationTestCase
{

    public static String EXPECTED_OCSP_ERROR_MESSAGE = "Certificate does not specify OCSP responder";

    public HttpPreferringCrlTestCase()
    {
        super("http-requester-tls-crl-standard-config.xml", REVOKED_CRL_FILE_PATH, ENTITY_CERTIFIED_REVOCATION_SUB_PATH);
    }

    @Test
    public void testPreferCrlWithFallback() throws Exception
    {
        try
        {
            runFlow("testFlowPreferCrl");
            fail("CertificateRevokedException should have been thrown.");
        }
        catch (Exception e)
        {
            verifyRevocationException(e);
        }
    }

    @Test
    public void testPreferCrlNoFallback() throws Exception
    {
        try
        {
            runFlow("testFlowPreferCrlNoFallback");
            fail("CertificateRevokedException should have been thrown.");
        }
        catch (Exception e)
        {
            verifyRevocationException(e);
        }
    }

    @Test
    public void testNotPreferCrlWithFallback() throws Exception
    {
        try
        {
            runFlow("testFlowNotPreferCrl");
            fail("CertificateRevokedException should have been thrown.");
        }
        catch (Exception e)
        {
            verifyRevocationException(e);
        }
    }

    @Test
    public void testNotPreferCrlNoFallback() throws Exception
    {
        try
        {
            runFlow("testFlowNotPreferCrlNoFallback");
            fail("If No preferring CRL and No fallback, so an exception that asks for an OCSP responder should be thrown " +
                  "since it is absent in the certificate");
        }
        catch (Exception e)
        {
            Throwable rootException = getRootCause(e);
            assertThat(rootException, is(instanceOf(CertPathValidatorException.class)));
            assertThat(rootException.getMessage(), is(EXPECTED_OCSP_ERROR_MESSAGE));
        }
    }

}
