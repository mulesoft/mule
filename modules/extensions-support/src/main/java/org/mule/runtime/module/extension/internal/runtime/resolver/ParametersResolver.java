/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.intersection;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isFlattenedParameterGroup;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.extension.api.util.NameUtils.getModelName;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.getExpressionBasedValueResolver;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.getFieldDefaultValueValueResolver;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getContainerName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldByNameOrAlias;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMetadataType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isParameterResolver;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isTypedValue;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isNullSafe;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.ConfigOverrideTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.DefaultEncodingAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.NullSafeTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.DefaultEncodingModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;

import com.google.common.base.Joiner;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Contains behavior to obtain a ResolverSet for a set of parameters values and a {@link ParameterizedModel}.
 *
 * @since 4.0
 */
public final class ParametersResolver implements ObjectTypeParametersResolver {

  private final MuleContext muleContext;
  private final Map<String, ?> parameters;

  private ParametersResolver(MuleContext muleContext, Map<String, ?> parameters) {
    this.muleContext = muleContext;
    this.parameters = parameters;
  }

  public static ParametersResolver fromValues(Map<String, ?> parameters, MuleContext muleContext) {
    return new ParametersResolver(muleContext, parameters);
  }

  public static ParametersResolver fromDefaultValues(ParameterizedModel parameterizedModel,
                                                     MuleContext muleContext) {
    Map<String, Object> parameterValues = new HashMap<>();
    for (ParameterModel model : parameterizedModel.getAllParameterModels()) {
      parameterValues.put(model.getName(), model.getDefaultValue());
    }

    return new ParametersResolver(muleContext, parameterValues);
  }

  /**
   * Constructs a {@link ResolverSet} from the parameters, using {@link #toValueResolver(Object, Set)} to process the values.
   *
   * @return a {@link ResolverSet}
   */
  public ResolverSet getParametersAsResolverSet(ParameterizedModel model, MuleContext muleContext) throws ConfigurationException {

    List<ParameterGroupModel> inlineGroups = getInlineGroups(model);
    ResolverSet resolverSet =
        getParametersAsResolverSet(model, getFlatParameters(inlineGroups, model.getAllParameterModels()), muleContext);
    for (ParameterGroupModel group : inlineGroups) {

      getInlineGroupResolver(group, resolverSet, muleContext);
    }
    return resolverSet;
  }

  /**
   * Constructs a {@link ResolverSet} from the parameters, using {@link #toValueResolver(Object, Set)} to process the values.
   *
   * @return a {@link ResolverSet}
   */
  public ResolverSet getParametersAsHashedResolverSet(ParameterizedModel model, MuleContext muleContext)
      throws ConfigurationException {

    List<ParameterGroupModel> inlineGroups = getInlineGroups(model);
    ResolverSet resolverSet =
        getParametersAsHashedResolverSet(model, getFlatParameters(inlineGroups, model.getAllParameterModels()), muleContext);
    for (ParameterGroupModel group : inlineGroups) {

      getInlineGroupResolver(group, resolverSet, muleContext);
    }
    return resolverSet;
  }

  private void getInlineGroupResolver(ParameterGroupModel group, ResolverSet resolverSet, MuleContext muleContext) {
    Optional<ParameterGroupDescriptor> descriptor = group.getModelProperty(ParameterGroupModelProperty.class)
        .map(ParameterGroupModelProperty::getDescriptor);

    String groupKey = descriptor
        .map(d -> getContainerName(d.getContainer()))
        .orElseGet(group::getName);

    if (parameters.containsKey(groupKey)) {
      resolverSet.add(groupKey, toValueResolver(parameters.get(groupKey), group.getModelProperties()));
    } else if (descriptor.isPresent()) {
      resolverSet.add(groupKey,
                      NullSafeValueResolverWrapper.of(new StaticValueResolver<>(null), descriptor.get().getMetadataType(),
                                                      muleContext, this));
    }
  }

  public ResolverSet getParametersAsResolverSet(ParameterizedModel model, List<ParameterModel> parameterModels,
                                                MuleContext muleContext)
      throws ConfigurationException {
    ResolverSet resolverSet = new ResolverSet(muleContext);
    return getResolverSet(model, parameterModels, muleContext, resolverSet);
  }

  public ResolverSet getParametersAsHashedResolverSet(ParameterizedModel model, List<ParameterModel> parameterModels,
                                                      MuleContext muleContext)
      throws ConfigurationException {
    ResolverSet resolverSet = new HashedResolverSet(muleContext);
    return getResolverSet(model, parameterModels, muleContext, resolverSet);
  }

  private ResolverSet getResolverSet(ParameterizedModel model, List<ParameterModel> parameterModels, MuleContext muleContext,
                                     ResolverSet resolverSet)
      throws ConfigurationException {
    parameterModels.forEach(p -> {
      final String parameterName = getMemberName(p, p.getName());
      ValueResolver<?> resolver;
      if (parameters.containsKey(parameterName)) {
        resolver = toValueResolver(parameters.get(parameterName), p.getModelProperties());
      } else {
        resolver = getDefaultValueResolver(p.getModelProperty(DefaultEncodingModelProperty.class).isPresent(), () -> {
          Object defaultValue = p.getDefaultValue();
          if (defaultValue instanceof String) {
            return getExpressionBasedValueResolver((String) defaultValue, p, muleContext);
          } else if (defaultValue != null) {
            return new StaticValueResolver<>(defaultValue);
          }
          return null;
        });
      }

      if (isNullSafe(p)) {
        ValueResolver<?> delegate = resolver != null ? resolver : new StaticValueResolver<>(null);
        MetadataType type = p.getModelProperty(NullSafeModelProperty.class).get().defaultType();
        resolver = NullSafeValueResolverWrapper.of(delegate, type, muleContext, this);
      }

      if (p.isOverrideFromConfig()) {
        resolver = ConfigOverrideValueResolverWrapper.of(resolver != null ? resolver : new StaticValueResolver<>(null),
                                                         parameterName, muleContext);
      }

      if (resolver != null) {
        resolverSet.add(parameterName, resolver);
      } else if (p.isRequired()) {
        throw new IllegalStateException(format("Parameter '%s' from the %s '%s' is required but is not set",
                                               parameterName,
                                               getComponentModelTypeName(model),
                                               getModelName(model)));
      }
    });

    checkParameterGroupExclusiveness(model, parameters.keySet());

    return resolverSet;
  }

  private List<ParameterGroupModel> getInlineGroups(ParameterizedModel model) {
    return model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .collect(toList());
  }

  private List<ParameterModel> getFlatParameters(List<ParameterGroupModel> inlineGroups, List<ParameterModel> parameters) {
    return parameters.stream()
        .filter(p -> inlineGroups.stream().noneMatch(g -> g.getParameterModels().contains(p)))
        .collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resolveParameterGroups(ObjectType objectType, DefaultObjectBuilder builder) {
    Class<?> objectClass = getType(objectType);
    objectType.getFields().stream()
        .filter(ExtensionMetadataTypeUtils::isFlattenedParameterGroup)
        .forEach(groupField -> {
          if (!(groupField.getValue() instanceof ObjectType)) {
            return;

          }

          final ObjectType groupType = (ObjectType) groupField.getValue();
          final Field objectField = getField(objectClass, getLocalPart(groupField));
          DefaultObjectBuilder groupBuilder = new DefaultObjectBuilder(getType(groupField.getValue()));
          builder.addPropertyResolver(objectField.getName(), new ObjectBuilderValueResolver<>(groupBuilder, muleContext));

          resolveParameters(groupType, groupBuilder);
          resolveParameterGroups(groupType, groupBuilder);
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resolveParameters(ObjectType objectType, DefaultObjectBuilder builder) {
    final Class<?> objectClass = getType(objectType);
    final boolean isParameterGroup = isFlattenedParameterGroup(objectType);
    objectType.getFields().forEach(field -> {
      final String key = getLocalPart(field);
      ValueResolver<?> valueResolver = null;
      Field objectField = getField(objectClass, key);

      if (parameters.containsKey(key)) {
        valueResolver = toValueResolver(parameters.get(key));
      } else if (!isParameterGroup) {
        valueResolver = getDefaultValueResolver(field.getAnnotation(DefaultEncodingAnnotation.class).isPresent(),
                                                () -> getDefaultValue(field).isPresent()
                                                    ? getFieldDefaultValueValueResolver(field, muleContext)
                                                    : null);
      }

      Optional<NullSafeTypeAnnotation> nullSafe = field.getAnnotation(NullSafeTypeAnnotation.class);
      if (nullSafe.isPresent()) {
        ValueResolver<?> delegate = valueResolver != null ? valueResolver : new StaticValueResolver<>(null);
        MetadataType type =
            getMetadataType(nullSafe.get().getType(), ExtensionsTypeLoaderFactory.getDefault().createTypeLoader());
        valueResolver = NullSafeValueResolverWrapper.of(delegate, type, muleContext, this);
      }

      if (field.getAnnotation(ConfigOverrideTypeAnnotation.class).isPresent()) {
        valueResolver =
            ConfigOverrideValueResolverWrapper.of(valueResolver != null ? valueResolver : new StaticValueResolver<>(null),
                                                  key, muleContext);
      }

      if (valueResolver != null) {
        try {
          initialiseIfNeeded(valueResolver, true, muleContext);
          builder.addPropertyResolver(objectField.getName(), valueResolver);
        } catch (InitialisationException e) {
          throw new MuleRuntimeException(e);
        }
      } else if (field.isRequired() && !isFlattenedParameterGroup(field)) {
        throw new IllegalStateException(format("The object '%s' requires the parameter '%s' but is not set",
                                               objectClass.getSimpleName(), objectField.getName()));
      }
    });
  }

  private Field getField(Class<?> objectClass, String key) {
    return getFieldByNameOrAlias(objectClass, key)
        .orElseThrow(() -> new IllegalModelDefinitionException(format("Class '%s' does not contain field %s",
                                                                      objectClass.getName(),
                                                                      key)));
  }

  public void checkParameterGroupExclusiveness(ParameterizedModel model, Set<String> resolverKeys)
      throws ConfigurationException {

    for (ParameterGroupModel group : model.getParameterGroupModels()) {
      for (ExclusiveParametersModel exclusiveModel : group.getExclusiveParametersModels()) {
        Collection<String> definedExclusiveParameters = intersection(exclusiveModel.getExclusiveParameterNames(), resolverKeys);
        if (definedExclusiveParameters.isEmpty() && exclusiveModel.isOneRequired()) {
          throw new ConfigurationException((createStaticMessage(format(
                                                                       "Parameter group '%s' requires that one of its optional parameters should be set but all of them are missing. "
                                                                           + "One of the following should be set: [%s]",
                                                                       group.getName(),
                                                                       Joiner.on(", ")
                                                                           .join(exclusiveModel.getExclusiveParameterNames())))));
        } else if (definedExclusiveParameters.size() > 1) {
          throw new ConfigurationException(
                                           createStaticMessage(format("In %s '%s', the following parameters cannot be set at the same time: [%s]",
                                                                      getComponentModelTypeName(model), getModelName(model),
                                                                      Joiner.on(", ").join(definedExclusiveParameters))));
        }
      }
    }
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
  private ValueResolver<?> toValueResolver(Object value) {
    return toValueResolver(value, emptySet());
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
   * @param modelProperties of the value's parameter
   * @return a {@link ValueResolver}
   */
  private ValueResolver<?> toValueResolver(Object value, Set<ModelProperty> modelProperties) {
    ValueResolver<?> resolver;
    if (value instanceof ValueResolver) {
      resolver = (ValueResolver<?>) value;
    } else if (value instanceof Collection) {
      resolver = getCollectionResolver((Collection) value);
    } else if (value instanceof Map) {
      resolver = getMapResolver((Map<Object, Object>) value);
    } else if (isParameterResolver(modelProperties)) {
      resolver = new StaticValueResolver<>(new StaticParameterResolver<>(value));
    } else if (isTypedValue(modelProperties)) {
      resolver = new StaticValueResolver<>(new TypedValue<>(value, DataType.fromObject(value)));
    } else {
      resolver = new StaticValueResolver<>(value);
    }
    return resolver;
  }

  private ValueResolver<?> getMapResolver(Map<Object, Object> value) {
    Map<ValueResolver<Object>, ValueResolver<Object>> normalizedMap = new LinkedHashMap<>(value.size());
    value.forEach((key, entryValue) -> normalizedMap.put((ValueResolver<Object>) toValueResolver(key),
                                                         (ValueResolver<Object>) toValueResolver(entryValue)));
    return MapValueResolver.of(value.getClass(), copyOf(normalizedMap.keySet()), copyOf(normalizedMap.values()), muleContext);
  }

  private ValueResolver<?> getCollectionResolver(Collection<?> collection) {
    return CollectionValueResolver.of(collection.getClass(),
                                      collection.stream().map(p -> toValueResolver(p)).collect(new ImmutableListCollector<>()));
  }

  /**
   * Gets a {@link ValueResolver} for the parameter if it has an associated a default value or encoding.
   *
   * @param hasDefaultEncoding whether the parameter has to use runtime's default encoding or not
   * @return {@link Supplier} for obtaining the the proper {@link ValueResolver} for the default value, {@code null} if there is
   *         no default.
   */
  private ValueResolver<?> getDefaultValueResolver(boolean hasDefaultEncoding, Supplier<ValueResolver<?>> supplier) {
    return hasDefaultEncoding ? new StaticValueResolver<>(muleContext.getConfiguration().getDefaultEncoding()) : supplier.get();
  }
}
