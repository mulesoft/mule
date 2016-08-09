/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.pgp;

import static org.mule.runtime.module.pgp.util.BouncyCastleUtil.KEY_FINGERPRINT_CALCULATOR;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.SecurityUtils;
import org.mule.runtime.module.pgp.i18n.PGPMessages;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PGPKeyRingImpl implements PGPKeyRing, Initialisable {

  protected static final Logger logger = LoggerFactory.getLogger(PGPKeyRingImpl.class);

  private String publicKeyRingFileName;

  private HashMap<String, PGPPublicKey> principalsKeyBundleMap;

  private String secretKeyRingFileName;

  private String secretAliasId;

  private PGPSecretKey secretKey;

  private String secretPassphrase;

  public void initialise() throws InitialisationException {
    try {
      if (!SecurityUtils.isFipsSecurityModel()) {
        java.security.Security.addProvider(new BouncyCastleProvider());
      }

      principalsKeyBundleMap = new HashMap<String, PGPPublicKey>();

      readPublicKeyRing();
      readPrivateKeyBundle();
    } catch (Exception e) {
      logger.error("Error in initialise:" + e.getMessage(), e);
      throw new InitialisationException(CoreMessages.failedToCreate("PGPKeyRingImpl"), e, this);
    }
  }

  private void readPublicKeyRing() throws Exception {
    InputStream in = IOUtils.getResourceAsStream(getPublicKeyRingFileName(), getClass());
    PGPPublicKeyRingCollection collection = new PGPPublicKeyRingCollection(in, KEY_FINGERPRINT_CALCULATOR);
    in.close();

    for (Iterator iterator = collection.getKeyRings(); iterator.hasNext();) {
      PGPPublicKeyRing ring = (PGPPublicKeyRing) iterator.next();
      String userID = "";
      for (Iterator iterator2 = ring.getPublicKeys(); iterator2.hasNext();) {
        PGPPublicKey publicKey = (PGPPublicKey) iterator2.next();
        Iterator userIDs = publicKey.getUserIDs();
        if (userIDs.hasNext()) {
          userID = (String) userIDs.next();
        }
        principalsKeyBundleMap.put(userID, publicKey);
      }
    }
  }

  private void readPrivateKeyBundle() throws Exception {
    InputStream in = IOUtils.getResourceAsStream(getSecretKeyRingFileName(), getClass());
    PGPSecretKeyRingCollection collection = new PGPSecretKeyRingCollection(in, KEY_FINGERPRINT_CALCULATOR);
    in.close();
    secretKey = collection.getSecretKey(Long.valueOf(getSecretAliasId()));

    if (secretKey == null) {
      StringBuilder message = new StringBuilder();
      message.append('\n');
      Iterator iterator = collection.getKeyRings();
      while (iterator.hasNext()) {
        PGPSecretKeyRing ring = (PGPSecretKeyRing) iterator.next();
        Iterator secretKeysIterator = ring.getSecretKeys();
        while (secretKeysIterator.hasNext()) {
          PGPSecretKey k = (PGPSecretKey) secretKeysIterator.next();
          message.append("Key: ");
          message.append(k.getKeyID());
          message.append('\n');
        }
      }
      throw new InitialisationException(PGPMessages.noSecretKeyFoundButAvailable(message.toString()), this);
    }
  }

  public String getSecretKeyRingFileName() {
    return secretKeyRingFileName;
  }

  public void setSecretKeyRingFileName(String value) {
    this.secretKeyRingFileName = value;
  }

  public String getSecretAliasId() {
    return secretAliasId;
  }

  public void setSecretAliasId(String value) {
    this.secretAliasId = value;
  }

  public String getSecretPassphrase() {
    return secretPassphrase;
  }

  public void setSecretPassphrase(String value) {
    this.secretPassphrase = value;
  }

  public PGPSecretKey getSecretKey() {
    return secretKey;
  }

  public String getPublicKeyRingFileName() {
    return publicKeyRingFileName;
  }

  public void setPublicKeyRingFileName(String value) {
    this.publicKeyRingFileName = value;
  }

  public PGPPublicKey getPublicKey(String principalId) {
    return principalsKeyBundleMap.get(principalId);
  }
}
