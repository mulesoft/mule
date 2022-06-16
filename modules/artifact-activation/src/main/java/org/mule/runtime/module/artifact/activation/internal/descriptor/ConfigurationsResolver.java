/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.descriptor;

import org.mule.runtime.api.deployment.meta.Product;

import java.io.File;
import java.util.List;

/**
 * Resolves the given files to configurations of a deployable project.
 */
public interface ConfigurationsResolver {

  /**
   * @param candidateConfigs {@link List} of files from which configurations will be resolved.
   * 
   * @return a {@link List} with the project configurations.
   */
  List<DeployableConfiguration> resolve(List<File> candidateConfigs);

  class DeployableConfiguration {

    private final String name;
    private final Product requiredProduct;

    public DeployableConfiguration(String name, Product requiredProduct) {
      this.name = name;
      this.requiredProduct = requiredProduct;
    }

    public String getName() {
      return name;
    }

    public Product getRequiredProduct() {
      return requiredProduct;
    }
  }

}
