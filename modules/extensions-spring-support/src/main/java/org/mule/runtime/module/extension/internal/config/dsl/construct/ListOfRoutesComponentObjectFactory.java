/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.construct;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.func.Once.of;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.func.Once.RunOnce;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.CollectionValueResolver;

import java.util.List;

/**
 * An {@link AbstractExtensionObjectFactory} to resolve {@link NestedRouteModel} elements with
 * {@link org.mule.runtime.extension.api.property.ListOfRoutesModelProperty}.
 * <p>
 * The objects are parsed as a {@link ValueResolver}.
 *
 * @since 4.10
 */
public class ListOfRoutesComponentObjectFactory<T> extends AbstractExtensionObjectFactory<CollectionValueResolver<T>> {

  private final NestedRouteModel model;
  private final ObjectType objectType;
  private final ClassLoader classLoader;
  private final List<ValueResolver<T>> nestedRoutes;
  private final RunOnce initialiser;
  private DefaultObjectBuilder<T> builder;
  private Class<Object> objectClass;

  public ListOfRoutesComponentObjectFactory(NestedRouteModel model, ObjectType objectType, ClassLoader classLoader,
                                            MuleContext muleContext, List<ValueResolver<T>> nestedRoutes) {
    super(muleContext);
    this.classLoader = classLoader;
    this.model = model;
    this.objectType = objectType;
    this.nestedRoutes = nestedRoutes;
    initialiser = of(() -> {
      objectClass = getType(objectType);
      builder = new DefaultObjectBuilder(objectClass, reflectionCache);
    });
  }

  @Override
  public CollectionValueResolver<T> doGetObject() throws Exception {
    return withContextClassLoader(classLoader, () -> {
      initialiser.runOnce();

      resolveParameters(objectType, builder);
      resolveParameterGroups(objectType, builder);

      return CollectionValueResolver.of(List.class, nestedRoutes);
    }, Exception.class, exception -> {
      throw exception;
    });
  }

}
