/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.api.config.MuleProperties.MULE_PGP_ENCRYPTION_ALGORITHM;
import org.mule.util.IOUtils;

import java.io.FileInputStream;
import java.net.URL;

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
                "Mule client <mule_client@mule.com>"), true);

        kbStrategy.initialise();
        String result = new String(kbStrategy.decrypt(msg, cryptInfo));
        assertEquals("This is a test message.\r\nThis is another line.\r\n", result);
    }

    @Test
    public void testEncryptWithCustomAlgorithm() throws Exception
    {
        String msg = "Test Message";
        PGPCryptInfo cryptInfo = new PGPCryptInfo(kbStrategy.getKeyManager().getPublicKey(
            "Mule client <mule_client@mule.com>"), true);

        try
        {
            setProperty(MULE_PGP_ENCRYPTION_ALGORITHM, EncryptionAlgorithm.AES_256.toString());
            kbStrategy.initialise();
            String result = new String(kbStrategy.encrypt(msg.getBytes(), cryptInfo));
            assertNotNull(result);
        }
        finally
        {
            clearProperty(MULE_PGP_ENCRYPTION_ALGORITHM);
        }
    }

    @Test
    public void testEncryptWithDefaultAlgorithm() throws Exception
    {
        String msg = "Test Message";
        PGPCryptInfo cryptInfo = new PGPCryptInfo(kbStrategy.getKeyManager().getPublicKey(
                "Mule client <mule_client@mule.com>"), true);

        kbStrategy.initialise();
        String result = new String(kbStrategy.encrypt(msg.getBytes(), cryptInfo));
        assertNotNull(result);
    }

    @Test(expected = RuntimeException.class)
    public void testEncryptWithInvalidAlgorithm() throws Exception
    {
        String msg = "Test Message";
        PGPCryptInfo cryptInfo = new PGPCryptInfo(kbStrategy.getKeyManager().getPublicKey(
                "Mule client <mule_client@mule.com>"), true);

        try
        {
            setProperty(MULE_PGP_ENCRYPTION_ALGORITHM, "invalid algorithm");
            kbStrategy.initialise();
            String result = new String(kbStrategy.encrypt(msg.getBytes(), cryptInfo));
            assertNotNull(result);
        }
        finally
        {
            clearProperty(MULE_PGP_ENCRYPTION_ALGORITHM);
        }
    }
}
