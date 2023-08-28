/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.lifecycle;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

/**
 * Factory class for creating {@link ArtifactLifecycleListener} instances.
 *
 * @since 4.5.0
 */
public class ArtifactLifecycleListenerFactory {

  private final Class<? extends ArtifactLifecycleListener> clazz;

  /**
   * Creates a factory for the given {@code artifactLifecycleListenerClass}.
   *
   * @param artifactLifecycleListenerClass the corresponding {@link ArtifactLifecycleListener} class that should be instantiated
   *                                       by this factory.
   */
  public ArtifactLifecycleListenerFactory(Class<? extends ArtifactLifecycleListener> artifactLifecycleListenerClass) {
    checkArgument(artifactLifecycleListenerClass != null, "ArtifactLifecycleListener type cannot be null");
    this.clazz = artifactLifecycleListenerClass;
  }

  /**
   * @return the {@link ArtifactLifecycleListener} instance of the corresponding class.
   */
  public ArtifactLifecycleListener createArtifactLifecycleListener() {
    try {
      return ClassUtils.instantiateClass(clazz);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create ArtifactLifecycleListener of type %s",
                                                         clazz.getName()),
                                     e);
    }
  }
}
