/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.secutiry.tls;


import static org.mule.runtime.core.api.util.PropertiesUtils.loadProperties;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.api.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TlsProperties {

  private static final Logger logger = LoggerFactory.getLogger(TlsProperties.class);

  private String[] enabledCipherSuites;
  private String[] enabledProtocols;
  private String defaultProtocol;

  public String[] getEnabledCipherSuites() {
    return enabledCipherSuites;
  }

  public String[] getEnabledProtocols() {
    return enabledProtocols;
  }

  public String getDefaultProtocol() {
    return defaultProtocol;
  }

  public void load(String fileName) {
    try {
      InputStream config = IOUtils.getResourceAsStream(fileName, TlsProperties.class);

      if (config == null) {
        logger.warn(String.format("File %s not found, using default configuration.", fileName));
      } else {
        logger.info(String.format("Loading configuration file: %s", fileName));
        Properties properties = loadProperties(config);

        String enabledCipherSuitesProperty = properties.getProperty("enabledCipherSuites");
        String enabledProtocolsProperty = properties.getProperty("enabledProtocols");
        String defaultProtocolProperty = properties.getProperty("defaultProtocol");

        if (enabledCipherSuitesProperty != null) {
          enabledCipherSuites = StringUtils.splitAndTrim(enabledCipherSuitesProperty, ",");

        }
        if (enabledProtocolsProperty != null) {
          enabledProtocols = StringUtils.splitAndTrim(enabledProtocolsProperty, ",");
        }
        if (defaultProtocolProperty != null) {
          defaultProtocol = defaultProtocolProperty.trim();
        }
      }
    } catch (IOException e) {
      logger.warn(String.format("Cannot read file %s, using default configuration", fileName), e);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TlsProperties that = (TlsProperties) o;

    if (!Arrays.equals(enabledCipherSuites, that.enabledCipherSuites)) {
      return false;
    }
    if (!Arrays.equals(enabledProtocols, that.enabledProtocols)) {
      return false;
    }

    return defaultProtocol != null ? defaultProtocol.equals(that.defaultProtocol) : that.defaultProtocol == null;
  }

  @Override
  public int hashCode() {
    int result = enabledCipherSuites != null ? Arrays.hashCode(enabledCipherSuites) : 0;
    result = 31 * result + (enabledProtocols != null ? Arrays.hashCode(enabledProtocols) : 0);
    result = 31 * result + (defaultProtocol != null ? defaultProtocol.hashCode() : 0);
    return result;
  }
}
