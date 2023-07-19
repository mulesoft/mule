/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.api.artifact;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.deployment.meta.MuleServiceContractModel;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Describes how to create a {@link Service} instance.
 */
public class ServiceDescriptor extends ArtifactDescriptor {

  private List<MuleServiceContractModel> contractModels = emptyList();

  /**
   * Creates a new service descriptor
   *
   * @param name service name. Non empty.
   */
  public ServiceDescriptor(String name) {
    super(name);
  }

  /**
   * @return the {@link MuleServiceContractModel} that are fulfilled by the service artifact.
   */
  public List<MuleServiceContractModel> getContractModels() {
    return contractModels;
  }

  /**
   * Sets the {@link MuleServiceContractModel} that are fulfilled by the service artifact.
   *
   * @param contractModels the {@link MuleServiceContractModel} that are fulfilled by the service artifact.
   */
  public void setContractModels(List<MuleServiceContractModel> contractModels) {
    checkArgument(!contractModels.isEmpty(), "contractModels cannot be empty");
    this.contractModels = contractModels;
  }
}
