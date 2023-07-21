/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilder;

/**
 * Provides a way to resolve the parameter values of a given {@link ObjectType}
 *
 * @since 4.0
 */
public interface ObjectTypeParametersResolver {

  /**
   * Adds the required {@link ValueResolver}s for the object parameters into the builder
   *
   * @param objectType the {@link ObjectType} to introspect
   * @param builder    the {@link ObjectBuilder} being used to create the owner object of the parameters
   */
  void resolveParameters(ObjectType objectType, DefaultObjectBuilder builder);

  /**
   * Adds the required {@link ValueResolver}s for the object parameters groups into the builder
   *
   * @param objectType the {@link ObjectType} to introspect
   * @param builder    the {@link ObjectBuilder} being used to create the owner object of the parameters
   */
  void resolveParameterGroups(ObjectType objectType, DefaultObjectBuilder builder);
}
