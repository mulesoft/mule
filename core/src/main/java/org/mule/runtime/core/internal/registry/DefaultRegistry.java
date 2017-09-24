/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.privileged.component.AnnotatedObjectInvocationHandler.removeDynamicAnnotations;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * Default implementation for {@link Registry}.
 *
 * @since 4.0
 */
public class DefaultRegistry implements Registry {

  private static final Function<Object, Object> deAnnotator = s -> s == null ? null : removeDynamicAnnotations(s);

  private MuleContextWithRegistries muleContext;

  public DefaultRegistry(MuleContext muleContext) {
    this.muleContext = (MuleContextWithRegistries) muleContext;
  }

  @Override
  public <T> Optional<T> lookupByType(Class<T> objectType) {
    try {
      return ofNullable(muleContext.getRegistry().lookupObject(objectType));
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public <T> Optional<T> lookupByName(String name) {
    return (Optional<T>) ofNullable(muleContext.getRegistry().lookupObject(name));
  }

  @Override
  public <T> Collection<T> lookupAllByType(Class<T> serviceType) {
    return muleContext.getRegistry().lookupObjects(serviceType);
  }
}
