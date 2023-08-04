/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.module.io.internal.dto;

import java.util.Collections;
import java.util.List;

public class IntegrationConfigDTO {

  private String config;
  private List<ConfigurationDTO> configurations = Collections.emptyList();

  public String getConfig() {
    return config;
  }

  public void setConfig(String config) {
    this.config = config;
  }

  public List<ConfigurationDTO> getConfigurations() {
    return configurations;
  }

  public void setConfigurations(List<ConfigurationDTO> configurations) {
    this.configurations = configurations;
  }
}
