/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.parameter;

import static java.lang.String.format;
import static org.mule.metadata.internal.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldByNameOrAlias;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.declaration.type.annotation.FlattenedTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.NullSafeTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.NullSafeValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

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

  private DefaultObjectBuilder builder;
  private Class<Object> objectClass;
  private final ObjectType objectType;
  private final ClassLoader classLoader;

  public TopLevelParameterObjectFactory(ObjectType type, ClassLoader classLoader, MuleContext muleContext) {
    super(muleContext);
    this.classLoader = classLoader;
    this.objectType = type;
    withContextClassLoader(classLoader, () -> {
      objectClass = getType(type);
      builder = new DefaultObjectBuilder(objectClass);
    });
  }

  @Override
  public Object doGetObject() throws Exception {
    return withContextClassLoader(classLoader, () -> {
      //TODO MULE-10919 - This logic is similar to that of the resolverset object builder and should
      // be generalized

      resolveParameters(objectType, builder);
      resolveParameterGroups(objectType, builder);

      ValueResolver<Object> resolver = new ObjectBuilderValueResolver<>(builder);
      return resolver.isDynamic() ? resolver : resolver.resolve(getInitialiserEvent(muleContext));
    }, Exception.class, exception -> {
      throw exception;
    });
  }

  private void resolveParameterGroups(ObjectType objectType, DefaultObjectBuilder builder) {
    Class<?> objectClass = getType(objectType);
    objectType.getFields().stream()
        .filter(f -> f.getAnnotation(FlattenedTypeAnnotation.class).isPresent())
        .forEach(groupField -> {
          if (!(groupField.getValue() instanceof ObjectType)) {
            return;
          }

          final ObjectType groupType = (ObjectType) groupField.getValue();
          final Field objectField = getField(objectClass, getFieldKey(groupField));
          DefaultObjectBuilder groupBuilder = new DefaultObjectBuilder(getType(groupField.getValue()));
          builder.addPropertyResolver(objectField.getName(), new ObjectBuilderValueResolver<>(groupBuilder));

          resolveParameters(groupType, groupBuilder);
          resolveParameterGroups(groupType, groupBuilder);
        });
  }

  private void resolveParameters(ObjectType objectType, DefaultObjectBuilder builder) {
    final Class<?> objectClass = getType(objectType);
    final boolean isParameterGroup = objectType.getAnnotation(FlattenedTypeAnnotation.class).isPresent();
    final Map<String, Object> parameters = getParameters();
    objectType.getFields().forEach(field -> {
      final String key = getFieldKey(field);

      ValueResolver<?> valueResolver = null;
      Field objectField = getField(objectClass, key);
      if (parameters.containsKey(key)) {
        valueResolver = toValueResolver(parameters.get(key));
      } else if (!isParameterGroup) {
        valueResolver = getDefaultValue(field)
            .map(value -> new TypeSafeExpressionValueResolver<>(value, objectField.getType(), muleContext))
            .orElse(null);
      }

      Optional<NullSafeTypeAnnotation> nullSafe = field.getAnnotation(NullSafeTypeAnnotation.class);
      if (nullSafe.isPresent()) {
        ValueResolver<?> delegate = valueResolver != null ? valueResolver : new StaticValueResolver<>(null);
        valueResolver = NullSafeValueResolverWrapper.of(delegate, nullSafe.get().getType(), muleContext);
      }

      if (valueResolver != null) {
        builder.addPropertyResolver(objectField.getName(), valueResolver);
      }
    });
  }

  private Field getField(Class<?> objectClass, String key) {
    return getFieldByNameOrAlias(objectClass, key)
        .orElseThrow(() -> new IllegalModelDefinitionException(format("Class '%s' does not contain field %s",
                                                                      objectClass.getName(),
                                                                      key)));
  }

  private String getFieldKey(ObjectFieldType field) {
    return field.getKey().getName().getLocalPart();
  }
}
