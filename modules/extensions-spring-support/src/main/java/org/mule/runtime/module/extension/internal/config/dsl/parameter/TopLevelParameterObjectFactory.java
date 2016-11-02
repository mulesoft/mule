/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.parameter;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.util.NameUtils.getAliasName;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.lang.reflect.Field;

/**
 * An {@link AbstractExtensionObjectFactory} to resolve extension objects that can be defined as named top level elements and be
 * placed in the mule registry.
 * <p>
 * The objects are parsed as a {@link ValueResolver}. If that resolver is not static, then a value is obtained using a default
 * {@link Event} and that value is returned. Otherwise, the dynamic {@link ValueResolver} is returned instead.
 *
 * @since 4.0
 */
public class TopLevelParameterObjectFactory extends AbstractExtensionObjectFactory<Object> {

  private ObjectBuilder builder;
  private Class<Object> objectClass;
  private final ClassLoader classLoader;

  public TopLevelParameterObjectFactory(ObjectType type, ClassLoader classLoader, MuleContext muleContext) {
    super(muleContext);
    this.classLoader = classLoader;
    withContextClassLoader(classLoader, () -> {
      objectClass = getType(type);
      builder = new DefaultObjectBuilder(objectClass);
    });
  }

  @Override
  public Object getObject() throws Exception {
    return withContextClassLoader(classLoader, () -> {
      resolveParameters(objectClass, builder);
      resolveParameterGroups(objectClass, builder);

      ValueResolver<Object> resolver = new ObjectBuilderValueResolver<>(builder);
      return resolver.isDynamic() ? resolver : resolver.resolve(getInitialiserEvent(muleContext));
    }, Exception.class, exception -> {
      throw exception;
    });
  }

  private void resolveParameterGroups(Class<?> objectClass, ObjectBuilder builder) {
    for (Field groupField : getAllFields(objectClass, withAnnotation(ParameterGroup.class))) {
      final Class<?> groupType = groupField.getType();
      ObjectBuilder groupBuilder = new DefaultObjectBuilder(groupType);
      builder.addPropertyResolver(groupField, new ObjectBuilderValueResolver<>(groupBuilder));

      resolveParameters(groupType, groupBuilder);
      resolveParameterGroups(groupType, groupBuilder);
    }
  }

  private void resolveParameters(Class<?> objectClass, ObjectBuilder builder) {
    // TODO: MULE-9453 this needs to not depend on fields exclusively
    for (Field field : getAllFields(objectClass)) {
      String key = getAliasName(field);
      if (getParameters().containsKey(key)) {
        builder.addPropertyResolver(field, toValueResolver(getParameters().get(key)));
      }
    }
  }
}
