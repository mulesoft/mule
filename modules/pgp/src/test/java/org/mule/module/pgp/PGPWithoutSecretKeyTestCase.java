/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */ 
package org.mule.module.pgp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.DefaultMuleMessage;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.security.CryptoFailureException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.ExceptionUtils;

import org.junit.Test;

public class PGPWithoutSecretKeyTestCase extends FunctionalTestCase
{
    private static final String NO_SECRET_KEY_RING_FILENAME_DEFINED = "No secret key ring filename defined";

    public String testFolder;

    private static Throwable exceptionFromFlow = null;

    public PGPWithoutSecretKeyTestCase()
    {
        setStartContext(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "file-decryption-config-no-secret-key.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        testFolder = getFileInsideWorkingDirectory("testData").getAbsolutePath();
    }

    @Test
    public void whenNoSecretKeyFilenameProvidedAndEncryptThenNoError() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.send("vm://inEncrypt", new DefaultMuleMessage("plainText", muleContext));
        MuleMessage message = client.request("vm://outEncrypt", 5000);
        assertThat(message, is(notNullValue()));

    }

    @Test
    public void whenNoSecretKeyFilenameProvidedAndDecryptThenFlowError() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.send("vm://inDecrypt", new DefaultMuleMessage("encryptedtext", muleContext));
        MuleMessage message = client.request("vm://outDecrypt", 5000);
        assertThat(message, is(nullValue()));
        CryptoFailureException cryptoFailureException =
                ExceptionUtils.getDeepestOccurenceOfType(exceptionFromFlow, CryptoFailureException.class);
        assertThat(cryptoFailureException.getCause().getMessage(), equalTo(NO_SECRET_KEY_RING_FILENAME_DEFINED));
    }

    public static class ExceptionSaver implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            ExceptionPayload exceptionPayload = event.getMessage().getExceptionPayload();
            exceptionFromFlow = exceptionPayload.getException();

            return null;
        }
    }

}
