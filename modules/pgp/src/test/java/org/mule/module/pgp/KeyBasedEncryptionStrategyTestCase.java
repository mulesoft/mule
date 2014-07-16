/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
