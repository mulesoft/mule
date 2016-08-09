/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.pgp;

import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.net.URL;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;

public abstract class AbstractEncryptionStrategyTestCase extends AbstractMuleContextTestCase {

  protected KeyBasedEncryptionStrategy kbStrategy;
  protected PGPKeyRing keyManager;

  @Override
  protected void doSetUp() throws Exception {
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
  protected void doTearDown() throws Exception {
    kbStrategy = null;
    keyManager = null;
  }
}


