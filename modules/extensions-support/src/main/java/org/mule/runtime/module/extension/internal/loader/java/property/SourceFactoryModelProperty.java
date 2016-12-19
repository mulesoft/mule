/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.runtime.source.SourceFactory;

/**
 * A {@link ModelProperty} meant to be used on {@link SourceModel source models},
 * which provides access to a {@link SourceFactory} used to create a message source
 *
 * @since 4.0
 */
public final class SourceFactoryModelProperty implements ModelProperty {

  private final SourceFactory sourceFactory;

  /**
   * Creates a new instance
   *
   * @param sourceFactory a {@link SourceFactory}
   */
  public SourceFactoryModelProperty(SourceFactory sourceFactory) {
    this.sourceFactory = sourceFactory;
  }

  /**
   * @return a {@link SourceFactory}
   */
  public SourceFactory getSourceFactory() {
    return sourceFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "sourceFactory";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
