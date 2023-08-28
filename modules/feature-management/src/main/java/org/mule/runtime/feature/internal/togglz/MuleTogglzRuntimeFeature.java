/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.feature.internal.togglz;

import org.mule.runtime.api.config.Feature;

/**
 * A {@link org.togglz.core.Feature} based on runtime {@link Feature}.
 * <p>
 * All Runtime Features are registered in Togglz.
 *
 * @since 4.5.0
 */
public class MuleTogglzRuntimeFeature implements org.togglz.core.Feature {

  private final Feature feature;

  public MuleTogglzRuntimeFeature(Feature feature) {
    this.feature = feature;
  }

  public Feature getRuntimeFeature() {
    return this.feature;
  }

  @Override
  public String name() {
    return feature.toString();
  }
}
