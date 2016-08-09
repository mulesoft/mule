/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.pgp;

import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.security.CredentialsAccessor;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.security.AbstractNamedEncryptionStrategy;
import org.mule.runtime.core.util.SecurityUtils;
import org.mule.runtime.module.pgp.i18n.PGPMessages;

import java.io.InputStream;
import java.security.Provider;
import java.util.Calendar;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyBasedEncryptionStrategy extends AbstractNamedEncryptionStrategy {

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(KeyBasedEncryptionStrategy.class);

  private PGPKeyRing keyManager;
  private CredentialsAccessor credentialsAccessor;
  private boolean checkKeyExpirity = false;
  private Provider provider;

  public void initialise() throws InitialisationException {
    if (!SecurityUtils.isFipsSecurityModel()) {
      java.security.Security.addProvider(new BouncyCastleProvider());
    }
    provider = SecurityUtils.getDefaultSecurityProvider();
  }

  public InputStream encrypt(InputStream data, Object cryptInfo) throws CryptoFailureException {
    try {
      PGPCryptInfo pgpCryptInfo = this.safeGetCryptInfo(cryptInfo);
      PGPPublicKey publicKey = pgpCryptInfo.getPublicKey();
      StreamTransformer transformer = new EncryptStreamTransformer(data, publicKey, provider);
      return new LazyTransformedInputStream(new TransformContinuouslyPolicy(), transformer);
    } catch (Exception e) {
      throw new CryptoFailureException(this, e);
    }
  }

  public InputStream decrypt(InputStream data, Object cryptInfo) throws CryptoFailureException {
    try {
      PGPCryptInfo pgpCryptInfo = this.safeGetCryptInfo(cryptInfo);
      PGPPublicKey publicKey = pgpCryptInfo.getPublicKey();
      StreamTransformer transformer = new DecryptStreamTransformer(data, publicKey, this.keyManager.getSecretKey(),
                                                                   this.keyManager.getSecretPassphrase(), provider);
      return new LazyTransformedInputStream(new TransformContinuouslyPolicy(), transformer);
    } catch (Exception e) {
      throw new CryptoFailureException(this, e);
    }
  }

  private PGPCryptInfo safeGetCryptInfo(Object cryptInfo) {
    if (cryptInfo == null) {
      MuleEvent event = RequestContext.getEvent();
      PGPPublicKey publicKey = keyManager.getPublicKey((String) this.getCredentialsAccessor().getCredentials(event));
      this.checkKeyExpirity(publicKey);
      return new PGPCryptInfo(publicKey, false);
    } else {
      PGPCryptInfo info = (PGPCryptInfo) cryptInfo;
      this.checkKeyExpirity(info.getPublicKey());
      return info;
    }
  }

  private void checkKeyExpirity(PGPPublicKey publicKey) {
    if (this.isCheckKeyExpirity() && publicKey.getValidDays() != 0) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(publicKey.getCreationTime());
      calendar.add(Calendar.DATE, publicKey.getValidDays());

      if (!calendar.getTime().after(Calendar.getInstance().getTime())) {
        throw new InvalidPublicKeyException(PGPMessages.pgpPublicKeyExpired());
      }
    }
  }

  public PGPKeyRing getKeyManager() {
    return keyManager;
  }

  public void setKeyManager(PGPKeyRing keyManager) {
    this.keyManager = keyManager;
  }

  public CredentialsAccessor getCredentialsAccessor() {
    return credentialsAccessor;
  }

  public void setCredentialsAccessor(CredentialsAccessor credentialsAccessor) {
    this.credentialsAccessor = credentialsAccessor;
  }

  public boolean isCheckKeyExpirity() {
    return checkKeyExpirity;
  }

  public void setCheckKeyExpirity(boolean checkKeyExpirity) {
    this.checkKeyExpirity = checkKeyExpirity;
  }
}
