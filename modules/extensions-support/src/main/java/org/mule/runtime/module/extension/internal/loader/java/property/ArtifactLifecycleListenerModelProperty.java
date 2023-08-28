/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.internal.lifecycle.ArtifactLifecycleListenerFactory;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

/**
 * A model property which gives access to an {@link ArtifactLifecycleListenerFactory} that can be used to create instances of a
 * given {@link ArtifactLifecycleListener} class.
 *
 * @since 4.5.0
 */
public class ArtifactLifecycleListenerModelProperty implements ModelProperty {

  private final ArtifactLifecycleListenerFactory artifactLifecycleListenerFactory;

  public ArtifactLifecycleListenerModelProperty(Class<? extends ArtifactLifecycleListener> artifactLifecycleListenerClass) {
    this.artifactLifecycleListenerFactory = new ArtifactLifecycleListenerFactory(artifactLifecycleListenerClass);
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
   * @return the extension's {@link ArtifactLifecycleListenerFactory}.
   */
  public ArtifactLifecycleListenerFactory getArtifactLifecycleListenerFactory() {
    return artifactLifecycleListenerFactory;
  }
}
