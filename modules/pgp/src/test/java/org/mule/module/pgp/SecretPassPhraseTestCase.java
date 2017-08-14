/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp;

import static java.lang.String.format;
import static org.mule.module.pgp.DecryptStreamTransformer.INVALID_PASS_PHRASE_ERROR_MESSAGE;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SecretPassPhraseTestCase extends FunctionalTestCase
{

    private static String secretPassPhrase ;

    private static final String VALID_KEY = "56B4312E6168F39C";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private InputStream encryptedData;

    @Override
    protected String getConfigFile()
    {
        return "secret-pass-phrase-config.xml";
    }

    @Before
    public void setUp() throws Exception
    {
        encryptedData = getClass().getClassLoader().getResourceAsStream("test-secret-id.asc");
    }

    @Test
    public void testInvalidSecretPassPhrase() throws Exception
    {
        secretPassPhrase = "WrongPassPhrase";
        expectedException.expectMessage(format(INVALID_PASS_PHRASE_ERROR_MESSAGE, secretPassPhrase, VALID_KEY));
        runFlow("decryptFlow", getTestEvent(encryptedData));
    }

    public static class TestPGPKeyRing extends PGPKeyRingImpl
    {

        @Override
        public String getSecretPassphrase()
        {
            return secretPassPhrase;
        }

    }
}
