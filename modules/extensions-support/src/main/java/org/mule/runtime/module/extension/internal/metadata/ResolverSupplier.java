/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.getClassName;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.resolving.NamedTypeResolver;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.metadata.internal.NullMetadataResolverSupplier;

import java.util.function.Supplier;

/**
 * {@link Supplier} implementation that returns instances of type {@link T} from a given {@link Class}
 *
 * @since 4.0
 * @param <T> The type of the instances to supply
 */
public final class ResolverSupplier<T extends NamedTypeResolver> implements Supplier<T> {

  private Class<T> clazz;

  private ResolverSupplier(Class<T> clazz) {
    this.clazz = clazz;
  }

  /**
   * Creates a new {@link Supplier} instance, which will return instances of the given {@link Class}.
   * </p>
   * If the given {@link Class} is a {@link NullMetadataResolver}, the supplier will return a
   * {@link NullMetadataResolverSupplier}.
   *
   * @param aClass The from which the {@link Supplier} will create instances
   * @param <T>    The {@link Class} type
   */
  public static <T extends NamedTypeResolver> Supplier<T> of(Class<T> aClass) {
    checkArgument(aClass != null, "The class can't be null");
    if (aClass.equals(NullMetadataResolver.class)) {
      return (Supplier<T>) NullMetadataResolverSupplier.INSTANCE;
    } else {
      return new ResolverSupplier<>(aClass);
    }
  }

  @Override
  public T get() {
    return instantiateResolver(clazz);
  }

  private T instantiateResolver(Class<T> factoryType) {
    try {
      return ClassUtils.instantiateClass(factoryType);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of type "
          + getClassName(factoryType)), e);
    }
  }
}
