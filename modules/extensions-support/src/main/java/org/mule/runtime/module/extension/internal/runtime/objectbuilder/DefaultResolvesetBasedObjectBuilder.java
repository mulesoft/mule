/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilderUtils.createInstance;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;

import java.util.Optional;

/**
 * Default implementation of {@link ResolverSetBasedObjectBuilder} which uses reflection
 * and a default constructor to instantiate the generated objects
 *
 * @param <T> the generic type of the produced objects
 * @since 4.0
 */
public class DefaultResolvesetBasedObjectBuilder<T> extends ResolverSetBasedObjectBuilder<T> {

  private final Class<T> prototypeClass;

  public DefaultResolvesetBasedObjectBuilder(Class<T> prototypeClass, Optional<ParameterGroupModelProperty> groupModelProperty,
                                             ResolverSet resolverSet) {
    super(prototypeClass, groupModelProperty, resolverSet);
    this.prototypeClass = prototypeClass;
  }

  @Override
  protected T instantiateObject() {
    return createInstance(prototypeClass);
  }
}
