/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

/**
 * A model property which gives access to an {@link ArtifactLifecycleListener}.
 *
 * @since 4.6
 */
public class ArtifactLifecycleListenerModelProperty implements ModelProperty {

  private final ArtifactLifecycleListener artifactLifecycleListener;

  public ArtifactLifecycleListenerModelProperty(ArtifactLifecycleListener artifactLifecycleListener) {
    this.artifactLifecycleListener = artifactLifecycleListener;
  }

  @Override
  public String getName() {
    return "artifactLifecycleListenerModelProperty";
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  /**
   * @return the extension's {@link ArtifactLifecycleListener}.
   */
  public ArtifactLifecycleListener getArtifactLifecycleListener() {
    return artifactLifecycleListener;
  }
}
