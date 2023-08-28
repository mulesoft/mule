/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.api.descriptor;

import org.mule.runtime.module.artifact.activation.internal.descriptor.XmlMuleConfigurationsFilter;

import java.io.File;

/**
 * Determines whether the given file is a configuration of a deployable project.
 */
public interface MuleConfigurationsFilter {

  static MuleConfigurationsFilter defaultMuleConfigurationsFilter() {
    return new XmlMuleConfigurationsFilter();
  }

  /**
   * @param candidateConfig file to determine if it's a configuration of a deployable project.
   *
   * @return whether the given file is a configuration of a deployable project.
   */
  boolean filter(File candidateConfig);

}
