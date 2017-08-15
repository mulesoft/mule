/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.module.pgp.DecryptStreamTransformer.INVALID_KEY_ERROR_MESSAGE;
import static org.mule.module.pgp.i18n.PGPMessages.noKeyIdFound;

import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SecretAliasIdTestCase extends FunctionalTestCase
{

    private static String secretAliasId ;

    private static final String VALID_KEY = "56B4312E6168F39C";

    private static final String INVALID_KEY = "35D86EAA0D5E353E";

    private static final String NONEXISTENT_KEY = "FFFFFFFFFFFFFFFF";

    private static final String PAYLOAD = "Testing secretAliasId";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private InputStream encryptedData;

    @Override
    protected String getConfigFile()
    {
        return "secret-alias-id-config.xml";
    }

    @Before
    public void setUp() throws Exception
    {
        encryptedData = getClass().getClassLoader().getResourceAsStream("test-secret-id.asc");
    }

    @Test
    public void getSecretKeyFromEncryptedMessage() throws Exception
    {
        secretAliasId = null;
        MuleEvent result = runFlow("decryptFlow", getTestEvent(encryptedData));
        assertThat(result.getMessage().getPayloadAsString(), is(PAYLOAD));
    }

    @Test
    public void getSecretKeyFromValidConfiguredSecretAliasId() throws Exception
    {
        secretAliasId = VALID_KEY;
        MuleEvent result = runFlow("decryptFlow", getTestEvent(encryptedData));
        assertThat(result.getMessage().getPayloadAsString(), is(PAYLOAD));
    }

    @Test
    public void getSecretKeyFromInvalidConfiguredSecretAliasId() throws Exception
    {
        secretAliasId = INVALID_KEY;
        expectedException.expectMessage(format(INVALID_KEY_ERROR_MESSAGE, INVALID_KEY, VALID_KEY));
        runFlow("decryptFlow", getTestEvent(encryptedData));
    }

    @Test
    public void getSecretKeyFromNonexistentSecretAliasId() throws Exception
    {
        secretAliasId = NONEXISTENT_KEY;
        expectedException.expectMessage(noKeyIdFound(NONEXISTENT_KEY).getMessage());
        runFlow("decryptFlow", getTestEvent(encryptedData));
    }

    public static class TestPGPKeyRing extends PGPKeyRingImpl
    {
        @Override
        public String getSecretAliasId()
        {
            return secretAliasId;
        }
    }
}
