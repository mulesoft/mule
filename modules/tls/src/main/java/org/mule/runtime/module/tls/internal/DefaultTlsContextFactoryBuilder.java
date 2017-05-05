/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.internal;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.lifecycle.CreateException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tls.TlsContextFactoryBuilder;

import java.io.IOException;

public class DefaultTlsContextFactoryBuilder implements TlsContextFactoryBuilder {

  private TlsContextFactory defaultTlsContextFactory;
  private DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory();

  private String trustStorePath;
  private String keyStorePath;

  public DefaultTlsContextFactoryBuilder(TlsContextFactory defaultTlsContextFactory) {
    this.defaultTlsContextFactory = defaultTlsContextFactory;
  }

  @Override
  public TlsContextFactoryBuilder setName(String name) {
    tlsContextFactory.setName(name);
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setEnabledProtocols(String protocols) {
    tlsContextFactory.setEnabledProtocols(protocols);
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setEnabledCipherSuites(String cipherSuites) {
    tlsContextFactory.setEnabledCipherSuites(cipherSuites);
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setTrustStorePath(String path) {
    trustStorePath = path;
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setTrustStorePassword(String password) {
    tlsContextFactory.setTrustStorePassword(password);
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setTrustStoreType(String type) {
    tlsContextFactory.setTrustStoreType(type);
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setInsecureTrustStore(boolean insecure) {
    tlsContextFactory.setTrustStoreInsecure(insecure);
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setTrustStoreAlgorithm(String algorithm) {
    tlsContextFactory.setTrustManagerAlgorithm(algorithm);
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setKeyStorePath(String path) {
    keyStorePath = path;
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setKeyStorePassword(String password) {
    tlsContextFactory.setKeyStorePassword(password);
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setKeyAlias(String alias) {
    tlsContextFactory.setKeyAlias(alias);
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setKeyPassword(String password) {
    tlsContextFactory.setKeyPassword(password);
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setKeyStoreType(String type) {
    tlsContextFactory.setKeyStoreType(type);
    return this;
  }

  @Override
  public TlsContextFactoryBuilder setKeyStoreAlgorithm(String algorithm) {
    tlsContextFactory.setKeyManagerAlgorithm(algorithm);
    return this;
  }

  @Override
  public TlsContextFactory build() throws CreateException {
    try {
      if (trustStorePath != null) {
        tlsContextFactory.setTrustStorePath(trustStorePath);
      }
      if (keyStorePath != null) {
        tlsContextFactory.setKeyStorePath(keyStorePath);
      }
      tlsContextFactory.initialise();
    } catch (IOException e) {
      throw new CreateException(createStaticMessage(e.getMessage()), e, tlsContextFactory);
    } catch (InitialisationException e) {
      throw new CreateException(e.getI18nMessage(), e, tlsContextFactory);
    }
    return tlsContextFactory;
  }

  @Override
  public TlsContextFactory buildDefault() {
    return defaultTlsContextFactory;
  }

}
