/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
