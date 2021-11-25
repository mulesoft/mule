/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
