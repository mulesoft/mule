/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.io.internal.dto;

import java.util.UUID;

/**
 * Correlates and integration with it's config name against the ICaaS Config ID for that same config.
 */
public class ConfigurationDTO {

  private String integrationConfigName;
  private UUID icaasConfigId;

  public String getIntegrationConfigName() {
    return integrationConfigName;
  }

  public void setIntegrationConfigName(String integrationConfigName) {
    this.integrationConfigName = integrationConfigName;
  }

  public UUID getIcaasConfigId() {
    return icaasConfigId;
  }

  public void setIcaasConfigId(UUID icaasConfigId) {
    this.icaasConfigId = icaasConfigId;
  }
}
