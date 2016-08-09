/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.domain;

import static org.mule.runtime.module.launcher.MuleFoldersUtil.getDomainLibFolder;
import org.mule.runtime.module.launcher.artifact.ArtifactStartedSplashScreen;
import org.mule.runtime.module.launcher.descriptor.DomainDescriptor;

/**
 * Splash screen specific for {@link Domain} startup.
 */
public class DomainStartedSplashScreen extends ArtifactStartedSplashScreen<DomainDescriptor> {

  @Override
  protected void createMessage(DomainDescriptor descriptor) {
    doBody(String.format("Started domain '%s'", descriptor.getName()));
    if (RUNTIME_VERBOSE_PROPERTY.isEnabled()) {
      listLibraries(descriptor);
    }
  }

  private void listLibraries(DomainDescriptor descriptor) {
    listItems(getLibraries(getDomainLibFolder(descriptor.getName())), "Domain libraries: ");
  }
}
