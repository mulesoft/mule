/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.descriptor;

import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.util.Optional;
import java.util.Properties;

/**
 * Creates an instance of a deployable artifact descriptor.
 *
 * @param <D> the concrete type of deployable artifact descriptor (application or domain) to resolve.
 */
public interface DeployableArtifactDescriptorCreator<D extends DeployableArtifactDescriptor> {

  /**
   * @return a default creator for {@link ApplicationDescriptor}s.
   */
  static DeployableArtifactDescriptorCreator<ApplicationDescriptor> applicationDescriptorCreator() {
    return new DeployableArtifactDescriptorCreator<ApplicationDescriptor>() {

      @Override
      public ApplicationDescriptor create(String name) {
        return new ApplicationDescriptor(name);
      }

      @Override
      public ApplicationDescriptor create(String name, Optional<Properties> deploymentProperties) {
        return new ApplicationDescriptor(name, deploymentProperties);
      }

    };
  }

  /**
   * @return a default creator for {@link DomainDescriptor}s.
   */
  static DeployableArtifactDescriptorCreator<DomainDescriptor> domainDescriptorCreator() {
    return new DeployableArtifactDescriptorCreator<DomainDescriptor>() {

      @Override
      public DomainDescriptor create(String name) {
        return new DomainDescriptor(name);
      }

      @Override
      public DomainDescriptor create(String name, Optional<Properties> deploymentProperties) {
        return new DomainDescriptor(name, deploymentProperties);
      }

    };
  }

  /**
   * Creates a deployable artifact descriptor with the given name.
   *
   * @param name artifact name. Non-empty.
   * @return a deployable artifact descriptor.
   */
  D create(String name);

  /**
   * Creates a deployable artifact descriptor with the given name and deployment properties.
   *
   * @param name                 artifact name. Non-empty.
   * @param deploymentProperties deployment properties.
   * @return a deployable artifact descriptor.
   */
  D create(String name, Optional<Properties> deploymentProperties);

}
