/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tls.internal.config;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.tls.TlsContextTrustStoreConfiguration;

/**
 * POJO for parsing the TLS trust store before building the actual DefaultTlsContext
 *
 * @since 4.0
 */
public class TrustStoreConfig extends AbstractComponent implements TlsContextTrustStoreConfiguration {

  private String path;
  private String password;
  private String type;
  private String algorithm;
  private boolean insecure;

  @Override
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  @Override
  public boolean isInsecure() {
    return insecure;
  }

  public void setInsecure(boolean insecure) {
    this.insecure = insecure;
  }
}
