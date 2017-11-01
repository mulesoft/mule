/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;

import com.google.common.collect.ImmutableSet;

import java.io.File;

/**
 * Encapsulates defaults when no explicit descriptor provided with an app.
 */
public class EmptyApplicationDescriptor extends ApplicationDescriptor {

  /**
   * Creates an {@link ApplicationDescriptor} with the default values.
   *
   * @param appLocation the directory where the application content is stored.
   */
  public EmptyApplicationDescriptor(File appLocation) {
    super(appLocation.getName());
    String configLocation = DEFAULT_CONFIGURATION_RESOURCE;
    setConfigResources(ImmutableSet.<String>builder().add(configLocation).build());
    setArtifactLocation(appLocation);
    setRootFolder(appLocation.getParentFile());
  }
}
