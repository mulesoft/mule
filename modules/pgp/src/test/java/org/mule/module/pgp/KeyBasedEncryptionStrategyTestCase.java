/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import org.mule.util.IOUtils;

import java.io.FileInputStream;
import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class KeyBasedEncryptionStrategyTestCase extends AbstractEncryptionStrategyTestCase
{

    @Test
    public void testDecrypt() throws Exception
    {
        URL url = Thread.currentThread().getContextClassLoader().getResource("./encrypted-signed.asc");

        FileInputStream in = new FileInputStream(url.getFile());
        byte[] msg = IOUtils.toByteArray(in);
        in.close();

        PGPCryptInfo cryptInfo = new PGPCryptInfo(kbStrategy.getKeyManager().getPublicKey(
            "Mule client <mule_client@mule.com>"), true);

        String result = new String(kbStrategy.decrypt(msg, cryptInfo));
        assertEquals("This is a test message.\r\nThis is another line.\r\n", result);
    }

    @Test
    public void testEncrypt() throws Exception
    {
        String msg = "Test Message";
        PGPCryptInfo cryptInfo = new PGPCryptInfo(kbStrategy.getKeyManager().getPublicKey(
            "Mule client <mule_client@mule.com>"), true);

        String result = new String(kbStrategy.encrypt(msg.getBytes(), cryptInfo));
        assertNotNull(result);
    }
}
