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
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;

import static java.util.Optional.empty;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.util.func.Once.RunOnce;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ProcessorChainValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RouteBuilderValueResolver;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.List;

import jakarta.inject.Inject;

/**
 * An {@link AbstractExtensionObjectFactory} to resolve {@link NestedRouteModel} elements.
 * <p>
 * The objects are parsed as a {@link ValueResolver}. If that resolver is not static, then a value is obtained using a default
 * {@link CoreEvent} and that value is returned. Otherwise, the dynamic {@link ValueResolver} is returned instead.
 *
 * @since 4.0
 */
public class RouteComponentObjectFactory extends AbstractExtensionObjectFactory<RouteBuilderValueResolver> {

  private final NestedRouteModel model;
  private final ObjectType objectType;
  private final ClassLoader classLoader;
  private final List<Processor> nestedProcessors;
  private final RunOnce initialiser;
  private DefaultObjectBuilder<RouteBuilderValueResolver> builder;
  private Class<RouteBuilderValueResolver> objectClass;
  @Inject
  private ReflectionCache reflectionCache;

  public RouteComponentObjectFactory(NestedRouteModel model, ObjectType objectType, ClassLoader classLoader,
                                     MuleContext muleContext, List<Processor> nestedProcessors) {
    super(muleContext);
    this.classLoader = classLoader;
    this.model = model;
    this.objectType = objectType;
    this.nestedProcessors = nestedProcessors;
    initialiser = of(() -> {
      objectClass = getType(objectType);
      builder = new DefaultObjectBuilder<>(objectClass, reflectionCache);
    });
  }

  @Override
  public RouteBuilderValueResolver doGetObject() throws Exception {

    return withContextClassLoader(classLoader, () -> {
      initialiser.runOnce();

      final MessageProcessorChain chain;

      if (nestedProcessors != null) {
        chain = newChain(empty(), nestedProcessors);
        final StreamingManager streamingManager =
            ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(StreamingManager.class);
        model.getNestedComponents().stream()
            .filter(component -> component instanceof NestedChainModel)
            .findFirst()
            .ifPresent(chainModel -> parameters.put(chainModel.getName(),
                                                    new ProcessorChainValueResolver(model, chain, streamingManager)));
      } else {
        chain = null;
      }

      resolveParameters(objectType, builder);
      resolveParameterGroups(objectType, builder);

      return new RouteBuilderValueResolver(builder, muleContext, chain);

    }, Exception.class, exception -> {
      throw exception;
    });
  }

}
