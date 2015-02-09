/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;

import java.security.GeneralSecurityException;

import org.junit.Test;

public class HttpRequestValidateCertificateTestCase extends AbstractHttpRequestTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "http-request-validate-certificate-config.xml";
    }

    @Override
    protected boolean enableHttps()
    {
        return true;
    }

    @Test
    public void rejectsMissingCertificate() throws Exception
    {
        try
        {
            runFlow("missingCertFlow", TEST_MESSAGE);
            fail("Failure expected as no valid certificate was provided for client");
        }
        catch (MessagingException e)
        {
            assertThat(e, is(instanceOf(MessagingException.class)));
            assertThat(e.getCauseException(), is(instanceOf(GeneralSecurityException.class)));
        }
    }

    @Test
    public void acceptsValidCertificate() throws Exception
    {
        MuleEvent result = runFlow("validCertFlow", TEST_MESSAGE);
        assertThat(result.getMessage().getPayloadAsString(), equalTo(DEFAULT_RESPONSE));
    }
}
