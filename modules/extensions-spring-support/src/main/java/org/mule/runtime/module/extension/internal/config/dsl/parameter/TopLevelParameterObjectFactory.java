/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.parameter;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldByAlias;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.ObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.lang.reflect.Field;

/**
 * An {@link AbstractExtensionObjectFactory} to resolve extension objects that can be defined as named top level elements and be
 * placed in the mule registry.
 * <p>
 * The objects are parsed as a {@link ValueResolver}. If that resolver is not static, then a value is obtained using a default
 * {@link MuleEvent} and that value is returned. Otherwise, the dynamic {@link ValueResolver} is returned instead.
 *
 * @since 4.0
 */
public class TopLevelParameterObjectFactory extends AbstractExtensionObjectFactory<Object> implements MuleContextAware {

  private ObjectBuilder builder;
  private Class<Object> objectClass;
  private final ClassLoader classLoader;
  private MuleContext muleContext;

  public TopLevelParameterObjectFactory(ObjectType type, ClassLoader classLoader) {
    this.classLoader = classLoader;
    withContextClassLoader(classLoader, () -> {
      objectClass = getType(type);
      builder = new DefaultObjectBuilder(objectClass);
    });
  }

  @Override
  public Object getObject() throws Exception {
    return withContextClassLoader(classLoader, () -> {
      getParameters().forEach((key, value) -> {
        Field field = getFieldByAlias(objectClass, key);
        if (field != null) {
          builder.addPropertyResolver(field, toValueResolver(value));
        }
      });

      ValueResolver<Object> resolver = new ObjectBuilderValueResolver<>(builder);
      return resolver.isDynamic() ? resolver : resolver.resolve(getInitialiserEvent(muleContext));
    }, Exception.class, exception -> {
      throw exception;
    });
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
