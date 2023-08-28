/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.annotation.param.NullSafe;

/**
 * A {@link ModelProperty} intended to be used on {@link ParameterModel parameters} to signal that if the parameter is resolved to
 * {@code null}, then the runtime should create a default instance, such as described in {@link NullSafe}
 *
 * @since 4.0
 */
public class NullSafeModelProperty implements ModelProperty {

  private final MetadataType defaultType;

  public NullSafeModelProperty(MetadataType defaultType) {
    this.defaultType = defaultType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "nullSafe";
  }

  public MetadataType defaultType() {
    return defaultType;
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
