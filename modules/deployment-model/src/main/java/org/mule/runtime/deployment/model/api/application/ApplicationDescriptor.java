/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.application;

import static java.util.Optional.empty;

import java.util.Optional;
import java.util.Properties;

/**
 * @deprecated since 4.5 use org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor instead.
 */
@Deprecated
public class ApplicationDescriptor extends org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor {

  /**
   * Creates a new application descriptor
   *
   * @param name application name. Non empty.
   */
  public ApplicationDescriptor(String name) {
    super(name, empty());
  }

  public ApplicationDescriptor(String name, Optional<Properties> properties) {
    super(name, properties);
  }

}
