/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.secutiry.tls;

import org.mule.runtime.core.privileged.security.tls.TlsConfiguration;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Move a {@link TlsConfiguration} to and from Properties (typically System Properties). This can be used to store TLS/SSL
 * configuration for a library (eg. Javamail, which reads java.mail properties) or for later retrieval by
 * {@link TlsPropertiesSocketFactory}.
 */
public class TlsPropertiesMapper {

  private static final String TRUST_NAME_SUFFIX = ".ssl.trustStore";
  private static final String TRUST_TYPE_SUFFIX = ".ssl.trustStoreType";
  private static final String TRUST_PASSWORD_SUFFIX = ".ssl.trustStorePassword";
  private static final String TRUST_ALGORITHM_SUFFIX = ".ssl.trustManagerAlgorithm";

  private static final String KEY_NAME_SUFFIX = ".ssl.keyStore";
  private static final String KEY_TYPE_SUFFIX = ".ssl.keyStoreType";
  private static final String KEY_PASSWORD_SUFFIX = ".ssl.keyStorePassword";

  private Logger logger = LoggerFactory.getLogger(getClass());
  private String namespace;

  public TlsPropertiesMapper(String namespace) {
    this.namespace = namespace;
  }

  public void readFromProperties(TlsConfiguration configuration, Properties properties) throws IOException {
    readTrustStoreFromProperties(configuration, properties);
    readKeyStoreFromProperties(configuration, properties);
  }

  private void readTrustStoreFromProperties(TlsConfiguration configuration, Properties properties) throws IOException {
    configuration.setTrustStore(getProperty(properties, TRUST_NAME_SUFFIX, configuration.getTrustStore()));
    configuration.setTrustStoreType(getProperty(properties, TRUST_TYPE_SUFFIX, configuration.getTrustStoreType()));
    configuration.setTrustStorePassword(getProperty(properties, TRUST_PASSWORD_SUFFIX, configuration.getTrustStorePassword()));
    configuration
        .setTrustManagerAlgorithm(getProperty(properties, TRUST_ALGORITHM_SUFFIX, configuration.getTrustManagerAlgorithm()));
  }

  // note the asymmetry here. this preserves the semantics of the original implementation.

  // originally, the "client" keystore data were written to system properties (only) and
  // used implicitly to construct sockets, while "non-client" keystore information was
  // used explicitly.

  // now we construct some of those implicit sockets explicitly (as part of avoiding global
  // configuration for tls across all transports). in these cases we read the data needed
  // from (namespaced) proeprties. if we read that information back into "non-client" keystore
  // data, even though it was written from "client" data, then we can use the same code in
  // TlsConfiguration to generate the sockets in both cases.

  private void readKeyStoreFromProperties(TlsConfiguration configuration, Properties properties) throws IOException {
    configuration.setKeyStore(getProperty(properties, KEY_NAME_SUFFIX, configuration.getKeyStore()));
    configuration.setKeyStoreType(getProperty(properties, KEY_TYPE_SUFFIX, configuration.getKeyStoreType()));
    configuration.setKeyStorePassword(getProperty(properties, KEY_PASSWORD_SUFFIX, configuration.getKeyStorePassword()));
  }

  private String getProperty(Properties properties, String suffix, String deflt) {
    String value = properties.getProperty(namespace + suffix);
    if (null == value) {
      value = deflt;
    }
    if (logger.isDebugEnabled()) {
      logger.debug(namespace + suffix + " -> " + value);
    }
    return value;
  }

}


