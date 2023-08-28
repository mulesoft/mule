/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.runtime.source.SdkSourceFactory;
import org.mule.runtime.extension.api.runtime.source.SourceFactory;

/**
 * A {@link ModelProperty} meant to be used on {@link SourceModel source models}, which provides access to a {@link SourceFactory}
 * used to create a message source
 *
 * @since 4.0
 */
public final class SdkSourceFactoryModelProperty implements ModelProperty {

  private final SdkSourceFactory sdkSourceFactory;

  /**
   * Creates a new instance
   *
   * @param sdkSourceFactory a {@link SourceFactory}
   */
  public SdkSourceFactoryModelProperty(SdkSourceFactory sdkSourceFactory) {
    this.sdkSourceFactory = sdkSourceFactory;
  }

  /**
   * @return a {@link SourceFactory}
   */
  public SdkSourceFactory getMessageSourceFactory() {
    return sdkSourceFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "sdkSourceFactory";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
