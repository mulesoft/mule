/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.privileged.component.AnnotatedObjectInvocationHandler.addAnnotationsToClass;
import static org.mule.runtime.module.artifact.activation.internal.ExecutionEnvironment.isMuleFramework;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.checkInstantiable;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.runtime.config.ConfigurationFactory;
import org.mule.runtime.module.artifact.internal.classloader.WithAttachedClassLoaders;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

/**
 * Implementation of {@link ConfigurationFactory} which creates instances based on a given {@link Class} which is assumed to have
 * a default and public constructor.
 *
 * @since 3.7.0
 */
public final class TypeAwareConfigurationFactory implements ConfigurationFactory {

  private final LazyValue<Class<?>> configurationType;
  private final ClassLoader extensionClassLoader;

  /**
   * Creates an instance of a given {@code configurationType} on each invocation to {@link #newInstance()}.
   *
   * @param configurationType    the type to be instantiated. Must be not {@code null}, and have a public default constructor
   * @param extensionClassLoader the {@link ClassLoader} on which the extension is loaded
   * @throws IllegalArgumentException if the type is {@code null} or doesn't have a default public constructor
   */
  public TypeAwareConfigurationFactory(Class<?> configurationType, ClassLoader extensionClassLoader) {
    checkArgument(configurationType != null, "configuration type cannot be null");
    checkArgument(extensionClassLoader != null, "extensionClassLoader type cannot be null");
    checkInstantiable(configurationType, new ReflectionCache());

    this.extensionClassLoader = extensionClassLoader;

    this.configurationType = new LazyValue<>(() -> {
      if (isMuleFramework()) {
        return configurationType;
      } else {
        return withContextClassLoader(this.extensionClassLoader, () -> {
          // We must add the annotations support with a proxy to avoid the SDK user to clutter the POJO definitions in an
          // extension with the annotations stuff.
          Class<?> annotated = addAnnotationsToClass(configurationType);
          if (extensionClassLoader instanceof WithAttachedClassLoaders) {
            // The annotated class is added to a spring cache that has to be cleared later, and for that we'll need the
            // classloader it was loaded with (or a classloader in the parents' hierarchy).
            // For the cleaning part, see MuleArtifactContext code.
            ((WithAttachedClassLoaders) extensionClassLoader).attachClassLoader(annotated.getClassLoader().getParent());
          }
          return annotated;
        });
      }
    });
  }

  /**
   * Returns a new instance on each invocation {@inheritDoc}
   */
  @Override
  public Object newInstance() {
    try {
      return withContextClassLoader(extensionClassLoader, configurationType.get()::newInstance);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not instantiate configuration of type "
          + configurationType.get().getName()), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<?> getObjectType() {
    return configurationType.get();
  }
}
