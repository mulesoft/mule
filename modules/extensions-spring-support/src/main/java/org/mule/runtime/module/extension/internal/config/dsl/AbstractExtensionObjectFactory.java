/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.intersection;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser.CHILD_ELEMENT_KEY_PREFIX;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser.CHILD_ELEMENT_KEY_SUFFIX;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getModelName;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isNullSafe;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.core.util.func.CompositePredicate;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.CollectionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.MapValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.NullSafeValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import com.google.common.base.Joiner;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Base class for {@link ObjectFactory} implementation which create extension components.
 * <p>
 * Contains behavior to obtain and manage components parameters.
 *
 * @param <T> the generic type of the instances to be built
 * @since 4.0
 */
public abstract class AbstractExtensionObjectFactory<T> extends AbstractAnnotatedObjectFactory<T> {

  protected final MuleContext muleContext;
  private Map<String, Object> parameters = new HashMap<>();

  public AbstractExtensionObjectFactory(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * @return the components parameters as {@link Map} which key is the parameter name and the value is the actual thingy
   */
  protected Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = normalize(parameters);
  }

  /**
   * Constructs a {@link ResolverSet} from the output of {@link #getParameters()}, using {@link #toValueResolver(Object)} to
   * process the values.
   *
   * @return a {@link ResolverSet}
   * @throws {@link ConfigurationException} if the exclusiveness condition between parameters is not honored
   */
  protected ResolverSet getParametersAsResolverSet(ParameterizedModel model, Predicate<ParameterModel>... predicates)
      throws ConfigurationException {

    ResolverSet resolverSet = new ResolverSet();
    Map<String, Object> parameters = getParameters();
    model.getAllParameterModels().stream()
        .filter(CompositePredicate.of(predicates))
        .forEach(p -> {
          String parameterName = getMemberName(p, p.getName());
          ValueResolver<?> resolver = null;
          if (parameters.containsKey(parameterName)) {
            resolver = toValueResolver(parameters.get(parameterName));
          } else {
            Object defaultValue = p.getDefaultValue();
            if (defaultValue instanceof String) {
              resolver = new TypeSafeExpressionValueResolver((String) defaultValue, getType(p.getType()), muleContext);
            } else if (defaultValue != null) {
              resolver = new StaticValueResolver<>(defaultValue);
            }
          }

          if (isNullSafe(p)) {
            resolver = resolver != null ? resolver : new StaticValueResolver<>(null);
            resolver = NullSafeValueResolverWrapper.of(resolver, model, p, muleContext);
          }

          if (resolver != null) {
            resolverSet.add(parameterName, resolver);
          }
        });

    checkParameterGroupExclusiveness(model, getParameters().keySet());

    return resolverSet;
  }

  /**
   * Wraps the {@code value} into a {@link ValueResolver} of the proper type. For example, {@link Collection} and {@link Map}
   * instances are exposed as {@link CollectionValueResolver} and {@link MapValueResolver} respectively.
   * <p>
   * If {@code value} is already a {@link ValueResolver} then it's returned as is.
   * <p>
   * Other values (including {@code null}) are wrapped in a {@link StaticValueResolver}.
   *
   * @param value the value to expose
   * @return a {@link ValueResolver}
   */
  protected ValueResolver<?> toValueResolver(Object value) {
    ValueResolver<?> resolver;
    if (value instanceof ValueResolver) {
      resolver = (ValueResolver<?>) value;
    } else if (value instanceof Collection) {
      resolver = CollectionValueResolver
          .of((Class<? extends Collection>) value.getClass(),
              (List) ((Collection) value).stream().map(this::toValueResolver).collect(new ImmutableListCollector()));
    } else if (value instanceof Map) {
      Map<Object, Object> map = (Map<Object, Object>) value;
      Map<ValueResolver<Object>, ValueResolver<Object>> normalizedMap = new LinkedHashMap<>(map.size());
      map.forEach((key, entryValue) -> normalizedMap.put((ValueResolver<Object>) toValueResolver(key),
                                                         (ValueResolver<Object>) toValueResolver(entryValue)));
      resolver = MapValueResolver.of(map.getClass(), copyOf(normalizedMap.keySet()), copyOf(normalizedMap.values()));
    } else {
      resolver = new StaticValueResolver<>(value);
    }
    return resolver;
  }

  private Map<String, Object> normalize(Map<String, Object> parameters) {
    Map<String, Object> normalized = new HashMap<>();

    parameters.forEach((key, value) -> {
      String normalizedKey = key;

      if (isChildKey(key)) {
        normalizedKey = unwrapChildKey(key);
        normalized.put(normalizedKey, value);
      } else {
        if (!normalized.containsKey(normalizedKey)) {
          normalized.put(normalizedKey, value);
        }
      }
    });

    return normalized;
  }

  private boolean isChildKey(String key) {
    return key.startsWith(CHILD_ELEMENT_KEY_PREFIX) && key.endsWith(CHILD_ELEMENT_KEY_SUFFIX);
  }

  private String unwrapChildKey(String key) {
    return key.replaceAll(CHILD_ELEMENT_KEY_PREFIX, "").replaceAll(CHILD_ELEMENT_KEY_SUFFIX, "");
  }

  protected void checkParameterGroupExclusiveness(ParameterizedModel model, Set<String> resolverKeys)
      throws ConfigurationException {

    for (ParameterGroupModel group : model.getParameterGroupModels()) {
      for (ExclusiveParametersModel exclusiveModel : group.getExclusiveParametersModels()) {
        Collection<String> definedExclusiveParameters = intersection(exclusiveModel.getExclusiveParameterNames(), resolverKeys);
        if (definedExclusiveParameters.isEmpty() && exclusiveModel.isOneRequired()) {
          throw new ConfigurationException((createStaticMessage(format(
                                                                       "Parameter group '%s' requires that one of its optional parameters should be set but all of them are missing",
                                                                       group.getName()))));
        } else if (definedExclusiveParameters.size() > 1) {
          throw buildExclusiveParametersException(model, definedExclusiveParameters);
        }
      }
    }
  }

  private ConfigurationException buildExclusiveParametersException(ParameterizedModel model,
                                                                   Collection<String> definedExclusiveParameters) {
    return new ConfigurationException(createStaticMessage(format("In %s '%s', the following parameters cannot be set at the same time: [%s]",
                                                                 getComponentModelTypeName(model), getModelName(model),
                                                                 Joiner.on(", ").join(definedExclusiveParameters))));
  }
}
