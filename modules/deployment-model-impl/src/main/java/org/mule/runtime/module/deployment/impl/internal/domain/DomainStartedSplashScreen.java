/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainLibFolder;

import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactStartedSplashScreen;


/**
 * Splash screen specific for {@link Domain} startup.
 */
public class DomainStartedSplashScreen extends ArtifactStartedSplashScreen<DomainDescriptor> {

  @Override
  public void createMessage(DomainDescriptor descriptor) {
    doBody(String.format("Started domain '%s'", descriptor.getName()));
    if (RUNTIME_VERBOSE_PROPERTY.isEnabled()) {
      listPlugins("Domain", descriptor);
      listLibraries(descriptor);
    }
  }

  private void listLibraries(DomainDescriptor descriptor) {
    listItems(getLibraries(getDomainLibFolder(descriptor.getName())), "Domain libraries: ");
  }
}
