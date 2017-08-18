/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.privileged.component.AnnotatedObjectInvocationHandler.addAnnotationsToClass;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;

/**
 * Implementation of {@link ConfigurationFactory} which creates instances based on a given {@link Class} which is assumed to have
 * a default and public constructor.
 *
 * @since 3.7.0
 */
public final class TypeAwareConfigurationFactory implements ConfigurationFactory {

  private final Class<?> configurationType;
  private final ClassLoader extensionClassLoader;

  /**
   * Creates an instance of a given {@code configurationType} on each invocation to {@link #newInstance()}.
   *
   * @param configurationType the type to be instantiated. Must be not {@code null}, and have a public default constructor
   * @param extensionClassLoader the {@link ClassLoader} on which the extension is loaded
   * @throws IllegalArgumentException if the type is {@code null} or doesn't have a default public constructor
   */
  public TypeAwareConfigurationFactory(Class<?> configurationType, ClassLoader extensionClassLoader) {
    checkArgument(configurationType != null, "configuration type cannot be null");
    checkArgument(extensionClassLoader != null, "extensionClassLoader type cannot be null");
    checkInstantiable(configurationType);

    this.configurationType = withContextClassLoader(extensionClassLoader, () -> {
      // We must add the annotations support with a proxy to avoid the SDK user to clutter the POJO definitions in an extension
      // with the annotations stuff.
      return addAnnotationsToClass(configurationType);
    });
    this.extensionClassLoader = extensionClassLoader;
  }

  /**
   * Returns a new instance on each invocation {@inheritDoc}
   */
  @Override
  public Object newInstance() {
    try {
      return withContextClassLoader(extensionClassLoader, configurationType::newInstance);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not instantiate configuration of type "
          + configurationType.getName()), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<?> getObjectType() {
    return configurationType;
  }
}
