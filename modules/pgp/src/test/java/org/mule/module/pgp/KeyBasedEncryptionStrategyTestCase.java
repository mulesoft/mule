/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mule.module.pgp.i18n.PGPMessages.noPublicKeyForPrincipal;
import static org.mule.module.pgp.i18n.PGPMessages.noSecretPassPhrase;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.CredentialsAccessor;
import org.mule.api.security.CryptoFailureException;
import org.mule.util.IOUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import org.bouncycastle.openpgp.PGPException;
import org.junit.Test;

public class KeyBasedEncryptionStrategyTestCase extends AbstractEncryptionStrategyTestCase
{

    @Test
    public void testDecryptCompressedSigned() throws Exception
    {
        testDecrypt("encrypted-compressed-signed.asc");
    }

    @Test
    public void testDecryptSignedCompressed() throws Exception
    {
        testDecrypt("encrypted-signed-compressed.asc");
    }

    @Test
    public void testDecryptCompressedSignedDifferentKeys() throws Exception
    {
        testDecrypt("encrypted-compressed-signed-different-keys.asc");
    }


    private void testDecrypt(String file) throws Exception
    {
        URL url = Thread.currentThread().getContextClassLoader().getResource(file);

        FileInputStream in = new FileInputStream(url.getFile());
        byte[] msg = IOUtils.toByteArray(in);
        in.close();

        PGPCryptInfo cryptInfo = new PGPCryptInfo(kbStrategy.getKeyManager().getPublicKey(
                "Mule client <mule_client@mule.com>"), false);

        kbStrategy.initialise();
        String result = new String(kbStrategy.decrypt(msg, cryptInfo));
        assertEquals("This is a test message.\r\nThis is another line.\r\n", result);
    }

    @Test
    public void testEncryptWithCustomAlgorithm() throws Exception
    {
        String msg = "Test Message";
        PGPCryptInfo cryptInfo = new PGPCryptInfo(kbStrategy.getKeyManager().getPublicKey(
                "Mule client <mule_client@mule.com>"), false);

        kbStrategy.setEncryptionAlgorithm(EncryptionAlgorithm.AES_256.toString());
        kbStrategy.initialise();
        String result = new String(kbStrategy.encrypt(msg.getBytes(), cryptInfo));
        assertNotNull(result);
    }

    @Test
    public void testEncryptWithDefaultAlgorithm() throws Exception
    {
        String msg = "Test Message";
        PGPCryptInfo cryptInfo = new PGPCryptInfo(kbStrategy.getKeyManager().getPublicKey(
                "Mule client <mule_client@mule.com>"), false);

        kbStrategy.initialise();
        String result = new String(kbStrategy.encrypt(msg.getBytes(), cryptInfo));
        assertNotNull(result);
    }

    @Test(expected = RuntimeException.class)
    public void testEncryptWithInvalidAlgorithm() throws Exception
    {
        String msg = "Test Message";
        PGPCryptInfo cryptInfo = new PGPCryptInfo(kbStrategy.getKeyManager().getPublicKey(
                "Mule client <mule_client@mule.com>"), false);

        kbStrategy.setEncryptionAlgorithm("invalid algorithm");
        kbStrategy.initialise();
        String result = new String(kbStrategy.encrypt(msg.getBytes(), cryptInfo));
    }

    @Test
    public void testInvalidPrincipal()
    {
        InputStream inputStream = mock(InputStream.class);
        FakeCredentialAccessor credentialAccessor = new FakeCredentialAccessor("Invalid Principle <invalidPrinciple@mule.com>");
        kbStrategy.setCredentialsAccessor(credentialAccessor);
        try
        {
            kbStrategy.encrypt(inputStream, null);
            fail("CryptoFailureException should be triggered because principal is wrong");
        }
        catch (CryptoFailureException cryptoFailureException)
        {
            assertThat(cryptoFailureException.getMessage(), containsString(noPublicKeyForPrincipal(credentialAccessor.getCredentials()).getMessage()));
        }
    }

    @Test
    public void testEncryptAndSignWithAllCredentialsWorksCorrectly() throws Exception
    {
        String msg = "Test Message";

        PGPEncryptAndSignInfo encryptAndSignInfo = new PGPEncryptAndSignInfo("Mule server <mule_server@mule.com>");
        kbStrategy.setCredentialsAccessor(new FakeCredentialAccessor("Mule client <mule_client@mule.com>"));
        kbStrategy.initialise();
        String result = new String(kbStrategy.encrypt(msg.getBytes(), encryptAndSignInfo));
        assertNotNull(result);
    }

    @Test
    public void testEncryptAndSignWithoutSignerSecretKey() throws Exception
    {
        String msg = "Test Message";

        PGPEncryptAndSignInfo encryptAndSignInfo = new PGPEncryptAndSignInfo("Robert Plant <robert@cambridge.edu.uk>");
        kbStrategy.setCredentialsAccessor(new FakeCredentialAccessor("Mule client <mule_client@mule.com>"));
        kbStrategy.initialise();
        try
        {
            kbStrategy.encrypt(msg.getBytes(), encryptAndSignInfo);
            fail("CryptoFailureException should be triggered because signer private key is not present");
        }
        catch (CryptoFailureException e)
        {
            assertThat(e.getMessage(), startsWith("Crypto Failure: Signer private key not found for principal:"));
        }
    }

    @Test
    public void testNoDefinedSecretPassPhrase() throws Exception
    {
        InputStream inputStream = mock(InputStream.class);
        PGPCryptInfo pgpCryptInfo = mock(PGPCryptInfo.class);
        PGPKeyRing keyManager = mock(PGPKeyRing.class);
        CredentialsAccessor credentialsAccessor = mock(CredentialsAccessor.class);
        kbStrategy.setCredentialsAccessor(credentialsAccessor);
        kbStrategy.setKeyManager(keyManager);
        try
        {
            kbStrategy.decrypt(inputStream, pgpCryptInfo);
            fail("CryptoFailureException should be triggered because secretPassPhrase is not defined");
        }
        catch (CryptoFailureException cryptoFailureException)
        {
            assertThat(cryptoFailureException.getMessage(), containsString(noSecretPassPhrase().getMessage()));
        }
    }

}
