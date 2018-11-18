/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static com.google.common.base.Joiner.on;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.intersection;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.declaration.type.TypeUtils.getAlias;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.declaration.type.annotation.ExclusiveOptionalsTypeAnnotation;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionBasedValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RequiredParameterValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * {@link DefaultObjectBuilder} extension that validates that the built object complies with
 * the rules specified in {@link org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals}
 *
 * @since 4.0
 */
public final class ExclusiveParameterGroupObjectBuilder<T> extends DefaultObjectBuilder<T> {

  private final ExclusiveOptionalsTypeAnnotation exclusiveOptionalsTypeAnnotation;
  private final boolean lazyInitEnabled;

  /**
   * Creates a new instance that will build instances of {@code prototypeClass}.
   *
   * @param prototypeClass a {@link Class} which needs to have a public defualt constructor
   */
  public ExclusiveParameterGroupObjectBuilder(Class<T> prototypeClass,
                                              ExclusiveOptionalsTypeAnnotation exclusiveOptionalsTypeAnnotation,
                                              boolean lazyInitEnabled,
                                              ReflectionCache reflectionCache) {
    super(prototypeClass, reflectionCache);
    this.exclusiveOptionalsTypeAnnotation = exclusiveOptionalsTypeAnnotation;
    this.lazyInitEnabled = lazyInitEnabled;
  }

  @Override
  public ObjectBuilder<T> addPropertyResolver(String propertyName, ValueResolver<? extends Object> resolver) {
    checkArgument(!isBlank(propertyName), "property name cannot be blank");
    checkArgument(resolver != null, "resolver cannot be null");

    return super.addPropertyResolver(propertyName, wrapResolver(propertyName, resolver));
  }

  @Override
  public ObjectBuilder<T> addPropertyResolver(Field field, ValueResolver<? extends Object> resolver) {
    checkArgument(resolver != null, "resolver cannot be null");

    return super.addPropertyResolver(field, wrapResolver(field.getName(), resolver));
  }

  private ValueResolver<? extends Object> wrapResolver(String propertyName, ValueResolver<? extends Object> resolver) {
    if (exclusiveOptionalsTypeAnnotation.isOneRequired() && resolver.isDynamic()
        && exclusiveOptionalsTypeAnnotation.getExclusiveParameterNames().contains(propertyName)) {
      if (resolver instanceof ExpressionBasedValueResolver) {
        resolver = new RequiredParameterValueResolverWrapper<>(resolver, propertyName,
                                                               ((ExpressionBasedValueResolver) resolver).getExpression());
      } else {
        resolver = new RequiredParameterValueResolverWrapper<>(resolver, propertyName);
      }
    }
    return resolver;
  }

  @Override
  public T build(ValueResolvingContext context) throws MuleException {
    if (!lazyInitEnabled) {
      Collection<String> definedExclusiveParameters =
          intersection(exclusiveOptionalsTypeAnnotation.getExclusiveParameterNames(),
                       resolvers.keySet().stream().map(fs -> getAlias(fs.getField())).collect(toSet()));
      if (definedExclusiveParameters.isEmpty() && exclusiveOptionalsTypeAnnotation.isOneRequired()) {
        throw new ConfigurationException((createStaticMessage(format(
                                                                     "Parameter group of type '%s' requires that one of its optional parameters should be set but all of them are missing. "
                                                                         + "One of the following should be set: [%s]",
                                                                     prototypeClass.getName(),
                                                                     on(", ").join(exclusiveOptionalsTypeAnnotation
                                                                         .getExclusiveParameterNames())))));
      } else if (definedExclusiveParameters.size() > 1) {
        throw new ConfigurationException(
                                         createStaticMessage(format("In Parameter group of type '%s', the following parameters cannot be set at the same time: [%s]",
                                                                    prototypeClass.getName(),
                                                                    on(", ").join(definedExclusiveParameters))));
      }
    }
    return super.build(context);
  }

}
