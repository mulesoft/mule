/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceFactory;
import org.mule.runtime.core.api.util.ClassUtils;

public final class DefaultSourceFactory implements SourceFactory {

  private final Class<? extends Source> sourceType;

  public DefaultSourceFactory(Class<? extends Source> sourceType) {
    checkInstantiable(sourceType);
    this.sourceType = sourceType;
  }

  @Override
  public Source createSource() {
    try {
      return ClassUtils.instantiateClass(sourceType);
    } catch (Exception e) {
      throw new RuntimeException("Exception found trying to instantiate source type " + sourceType.getName(), e);
    }
  }
}
