/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.parameter;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.privileged.component.AnnotatedObjectInvocationHandler.addAnnotationsToClass;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext.from;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.dsl.api.component.ObjectTypeProvider;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import org.slf4j.Logger;

/**
 * An {@link AbstractExtensionObjectFactory} to resolve extension objects that can be defined as named top level elements and be
 * placed in the mule registry.
 * <p>
 * The objects are parsed as a {@link ValueResolver}. If that resolver is not static, then a value is obtained using a default
 * {@link CoreEvent} and that value is returned. Otherwise, the dynamic {@link ValueResolver} is returned instead.
 *
 * @since 4.0
 */
public class TopLevelParameterObjectFactory extends AbstractExtensionObjectFactory<Object> implements ObjectTypeProvider {

  private DefaultObjectBuilder builder;
  private Class<?> objectClass;
  private final ObjectType objectType;
  private final ClassLoader classLoader;
  private String name;
  private Object staticProduct = null;

  public TopLevelParameterObjectFactory(ObjectType type, ClassLoader classLoader, MuleContext muleContext) {
    super(muleContext);
    this.classLoader = classLoader;
    this.objectType = type;

    objectClass = withContextClassLoader(classLoader, () -> {
      // We must add the annotations support with a proxy to avoid the SDK user to clutter the POJO definitions in an extension
      // with the annotations stuff.
      return addAnnotationsToClass(getType(type));
    });
    builder = new DefaultObjectBuilder(objectClass);
  }

  @Override
  public Object doGetObject() throws Exception {
    return withContextClassLoader(classLoader, () -> {
      // TODO MULE-10919 - This logic is similar to that of the resolverset object builder and should
      // be generalized

      resolveParameters(objectType, builder);
      resolveParameterGroups(objectType, builder);

      injectFields();

      ValueResolver<Object> resolver = new ObjectBuilderValueResolver<>(builder, muleContext);
      if (resolver.isDynamic()) {
        return resolver;
      }

      staticProduct = resolver.resolve(from(getInitialiserEvent(muleContext)));
      muleContext.getInjector().inject(staticProduct);
      return staticProduct;
    }, Exception.class, exception -> {
      throw exception;
    });
  }

  private void injectFields() {
    builder.setEncoding(muleContext.getConfiguration().getDefaultEncoding());
    if (name != null) {
      builder.setName(name);
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public Class<?> getObjectType() {
    return objectClass;
  }
}
