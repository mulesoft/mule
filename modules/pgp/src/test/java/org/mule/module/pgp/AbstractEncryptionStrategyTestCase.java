/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.net.URL;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

public abstract class AbstractEncryptionStrategyTestCase extends AbstractMuleContextTestCase
{
    protected KeyBasedEncryptionStrategy kbStrategy;
    protected PGPKeyRing keyManager;

    protected static boolean isCryptographyExtensionInstalled()
    {
        // see MULE-3671
        try
        {
            int maxKeyLength = Cipher.getMaxAllowedKeyLength("DES/CBC/PKCS5Padding");
            // if JCE is not installed, maxKeyLength will be 64
            return maxKeyLength == Integer.MAX_VALUE;
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new AssertionError(e);
        }
    }

    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        return (isCryptographyExtensionInstalled() == false);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        PGPKeyRingImpl keyM = new PGPKeyRingImpl();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        URL url = loader.getResource("./serverPublic.gpg");
        keyM.setPublicKeyRingFileName(url.getFile());

        url = loader.getResource("./serverPrivate.gpg");
        keyM.setSecretKeyRingFileName(url.getFile());

        keyM.setSecretAliasId("6247672658342245276");
        keyM.setSecretPassphrase("TestingPassphrase");
        keyM.initialise();

        kbStrategy = new KeyBasedEncryptionStrategy();
        kbStrategy.setKeyManager(keyM);
        kbStrategy.setCredentialsAccessor(new FakeCredentialAccessor("Mule server <mule_server@mule.com>"));
        kbStrategy.initialise();

        keyManager = keyM;
    }

    @Override
    protected void doTearDown() throws Exception
    {
        kbStrategy = null;
        keyManager = null;
    }
}


