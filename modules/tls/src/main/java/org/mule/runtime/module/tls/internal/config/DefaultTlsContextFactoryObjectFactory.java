/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tls.internal.config;

import org.mule.runtime.api.tls.TlsContextKeyStoreConfiguration;
import org.mule.runtime.api.tls.TlsContextTrustStoreConfiguration;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;

/**
 * {@link ObjectFactory} for TLS context factory
 *
 * @since 4.0
 */
public class DefaultTlsContextFactoryObjectFactory extends AbstractComponentFactory<DefaultTlsContextFactory> {

  private String name;
  private TlsContextKeyStoreConfiguration keyStore;
  private TlsContextTrustStoreConfiguration trustStore;
  private String enabledProtocols;
  private String enabledCipherSuites;

  public void setName(String name) {
    this.name = name;
  }

  public void setKeyStore(TlsContextKeyStoreConfiguration keyStore) {
    this.keyStore = keyStore;
  }

  public void setTrustStore(TlsContextTrustStoreConfiguration trustStore) {
    this.trustStore = trustStore;
  }

  public void setEnabledProtocols(String enabledProtocols) {
    this.enabledProtocols = enabledProtocols;
  }

  public void setEnabledCipherSuites(String enabledCipherSuites) {
    this.enabledCipherSuites = enabledCipherSuites;
  }

  @Override
  public DefaultTlsContextFactory doGetObject() throws Exception {
    DefaultTlsContextFactory tlsContextFactory = new DefaultTlsContextFactory(getAnnotations());

    tlsContextFactory.setName(name);
    tlsContextFactory.setEnabledProtocols(enabledProtocols);
    tlsContextFactory.setEnabledCipherSuites(enabledCipherSuites);

    if (keyStore != null) {
      tlsContextFactory.setKeyAlias(keyStore.getAlias());
      tlsContextFactory.setKeyPassword(keyStore.getKeyPassword());
      if (keyStore.getPath() != null) {
        tlsContextFactory.setKeyStorePath(keyStore.getPath());
      }
      tlsContextFactory.setKeyStorePassword(keyStore.getPassword());
      if (keyStore.getType() != null) {
        tlsContextFactory.setKeyStoreType(keyStore.getType());
      }
      if (keyStore.getAlgorithm() != null) {
        tlsContextFactory.setKeyManagerAlgorithm(keyStore.getAlgorithm());
      }
    }

    if (trustStore != null) {
      if (trustStore.getPath() != null) {
        tlsContextFactory.setTrustStorePath(trustStore.getPath());
      }
      tlsContextFactory.setTrustStorePassword(trustStore.getPassword());
      if (trustStore.getType() != null) {
        tlsContextFactory.setTrustStoreType(trustStore.getType());
      }
      if (trustStore.getAlgorithm() != null) {
        tlsContextFactory.setTrustManagerAlgorithm(trustStore.getAlgorithm());
      }
      tlsContextFactory.setTrustStoreInsecure(trustStore.isInsecure());
    }

    return tlsContextFactory;
  }
}
