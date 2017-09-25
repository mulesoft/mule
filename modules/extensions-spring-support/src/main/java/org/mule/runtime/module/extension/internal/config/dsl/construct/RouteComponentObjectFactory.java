/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.construct;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ProcessorChainValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.List;

/**
 * An {@link AbstractExtensionObjectFactory} to resolve {@link NestedRouteModel} elements.
 * <p>
 * The objects are parsed as a {@link ValueResolver}. If that resolver is not static, then a value is obtained using a default
 * {@link CoreEvent} and that value is returned. Otherwise, the dynamic {@link ValueResolver} is returned instead.
 *
 * @since 4.0
 */
public class RouteComponentObjectFactory extends AbstractExtensionObjectFactory<Object> {

  private DefaultObjectBuilder builder;
  private NestedRouteModel model;
  private Class<Object> objectClass;
  private final ObjectType objectType;
  private final ClassLoader classLoader;
  private final List<Processor> nestedProcessors;

  public RouteComponentObjectFactory(NestedRouteModel model, ObjectType objectType, ClassLoader classLoader,
                                     MuleContext muleContext, List<Processor> nestedProcessors) {
    super(muleContext);
    this.classLoader = classLoader;
    this.model = model;
    this.objectType = objectType;
    this.nestedProcessors = nestedProcessors;
    withContextClassLoader(classLoader, () -> {
      objectClass = getType(objectType);
      builder = new DefaultObjectBuilder(objectClass);
    });
  }

  @Override
  public Object doGetObject() throws Exception {
    return withContextClassLoader(classLoader, () -> {

      if (nestedProcessors != null) {
        model.getNestedComponents().stream()
            .filter(component -> component instanceof NestedChainModel)
            .findFirst()
            .ifPresent(chain -> parameters.put(chain.getName(), new ProcessorChainValueResolver(nestedProcessors)));
      }

      resolveParameters(objectType, builder);
      resolveParameterGroups(objectType, builder);

      return new ObjectBuilderValueResolver<>(builder, muleContext);
    }, Exception.class, exception -> {
      throw exception;
    });
  }
}
