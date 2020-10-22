/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;

import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.extension.api.runtime.source.SdkSourceFactory;
import org.mule.runtime.module.extension.internal.runtime.source.legacy.LegacySourceAdapter;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.sdk.api.runtime.source.Source;

public final class DefaultSdkSourceFactory implements SdkSourceFactory {

  private final Class<?> sourceType;

  public DefaultSdkSourceFactory(Class<?> sourceType) {
    checkInstantiable(sourceType, new ReflectionCache());
    if (sourceType.isAssignableFrom(org.mule.runtime.extension.api.runtime.source.Source.class) || sourceType.isAssignableFrom(Source.class)) {
      this.sourceType = sourceType;
    } else {
      throw new IllegalSourceModelDefinitionException(format("Source type %s must extend either %s or %s classes",
              sourceType.getName(),
              Source.class.getName(),
              org.mule.runtime.extension.api.runtime.source.Source.class.getName()));
    }
  }

  @Override
  public Source createMessageSource() {
    try {
      Object source = ClassUtils.instantiateClass(sourceType);
      if (source instanceof org.mule.runtime.extension.api.runtime.source.Source) {
        source = LegacySourceAdapter.from((org.mule.runtime.extension.api.runtime.source.Source) source);
      }
      return (Source) source;
    } catch (Exception e) {
      throw new RuntimeException("Exception found trying to instantiate source type " + sourceType.getName(), e);
    }
  }
}
