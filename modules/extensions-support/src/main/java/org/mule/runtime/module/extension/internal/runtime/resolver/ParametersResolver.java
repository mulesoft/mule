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
import static org.mule.metadata.internal.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.internal.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isParameterGroup;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.extension.api.util.NameUtils.getModelName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getContainerName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldByNameOrAlias;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMetadataType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isNullSafe;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
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
import org.mule.runtime.extension.api.declaration.type.annotation.DefaultEncodingAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.NullSafeTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.module.extension.internal.loader.java.ParameterResolverTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.TypedValueTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.DefaultEncodingModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;

import com.google.common.base.Joiner;

import java.lang.reflect.Field;
import java.util.Collection;
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

  public ParametersResolver(MuleContext muleContext, Map<String, ?> parameters) {
    this.muleContext = muleContext;
    this.parameters = parameters;
  }

  /**
   * Constructs a {@link ResolverSet} from the parameters, using {@link #toValueResolver(Object, Set)} to
   * process the values.
   *
   * @return a {@link ResolverSet}
   */
  public ResolverSet getParametersAsResolverSet(ParameterizedModel model) throws ConfigurationException {

    List<ParameterGroupModel> inlineGroups = getInlineGroups(model);
    ResolverSet resolverSet = getParametersAsResolverSet(model, getFlatParameters(inlineGroups, model.getAllParameterModels()));
    for (ParameterGroupModel group : inlineGroups) {
      String containerName = getContainerName(group.getModelProperty(ParameterGroupModelProperty.class)
          .map(mp -> mp.getDescriptor().getContainer())
          .orElseThrow(() -> new IllegalArgumentException(
                                                          format("Missing ParameterGroup information for group '%s'",
                                                                 group.getName()))));

      if (parameters.containsKey(containerName)) {
        resolverSet.add(containerName, toValueResolver(parameters.get(containerName), group.getModelProperties()));
      }
    }
    return resolverSet;
  }

  public ResolverSet getParametersAsResolverSet(ParameterizedModel model, List<ParameterModel> parameterModels)
      throws ConfigurationException {
    ResolverSet resolverSet = new ResolverSet();
    parameterModels.forEach(p -> {
      String parameterName = getMemberName(p, p.getName());
      ValueResolver<?> resolver;
      if (parameters.containsKey(parameterName)) {
        resolver = toValueResolver(parameters.get(parameterName), p.getModelProperties());
      } else {
        resolver = getDefaultValueResolver(p.getModelProperty(DefaultEncodingModelProperty.class).isPresent(), () -> {
          Object defaultValue = p.getDefaultValue();
          if (defaultValue instanceof String) {
            return new TypeSafeExpressionValueResolver((String) defaultValue, getType(p.getType()), muleContext);
          } else if (defaultValue != null) {
            return new StaticValueResolver<>(defaultValue);
          }
          return null;
        });
      }

      if (isNullSafe(p)) {
        MetadataType type = p.getModelProperty(NullSafeModelProperty.class).get().defaultType();
        ValueResolver<?> delegate = resolver != null ? resolver : new StaticValueResolver<>(null);
        resolver = NullSafeValueResolverWrapper.of(delegate, type, muleContext, this);
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
        .filter(ExtensionMetadataTypeUtils::isParameterGroup)
        .forEach(groupField -> {
          if (!(groupField.getValue() instanceof ObjectType)) {
            return;
          }

          final ObjectType groupType = (ObjectType) groupField.getValue();
          final Field objectField = getField(objectClass, getLocalPart(groupField));
          DefaultObjectBuilder groupBuilder = new DefaultObjectBuilder(getType(groupField.getValue()));
          builder.addPropertyResolver(objectField.getName(), new ObjectBuilderValueResolver<>(groupBuilder));

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
    final boolean isParameterGroup = isParameterGroup(objectType);
    objectType.getFields().forEach(field -> {
      final String key = getLocalPart(field);
      ValueResolver<?> valueResolver = null;
      Field objectField = getField(objectClass, key);

      if (parameters.containsKey(key)) {
        valueResolver = toValueResolver(parameters.get(key));
      } else if (!isParameterGroup) {
        valueResolver = getDefaultValueResolver(field.getAnnotation(DefaultEncodingAnnotation.class).isPresent(),
                                                () -> getDefaultValue(field).isPresent()
                                                    ? new TypeSafeExpressionValueResolver<>(getDefaultValue(field).get(),
                                                                                            objectField.getType(), muleContext)
                                                    : null);
      }

      Optional<NullSafeTypeAnnotation> nullSafe = field.getAnnotation(NullSafeTypeAnnotation.class);
      if (nullSafe.isPresent()) {
        ValueResolver<?> delegate = valueResolver != null ? valueResolver : new StaticValueResolver<>(null);
        MetadataType type =
            getMetadataType(nullSafe.get().getType(), ExtensionsTypeLoaderFactory.getDefault().createTypeLoader());
        valueResolver = NullSafeValueResolverWrapper.of(delegate, type, muleContext, this);
      }

      if (valueResolver != null) {
        builder.addPropertyResolver(objectField.getName(), valueResolver);
      } else if (field.isRequired() && !isParameterGroup(field)) {
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
   * @param value           the value to expose
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
    return MapValueResolver.of(value.getClass(), copyOf(normalizedMap.keySet()), copyOf(normalizedMap.values()));
  }

  private ValueResolver<?> getCollectionResolver(Collection<?> collection) {
    return CollectionValueResolver.of(collection.getClass(),
                                      collection.stream().map(p -> toValueResolver(p)).collect(new ImmutableListCollector<>()));
  }

  private boolean isTypedValue(Set<ModelProperty> modelProperties) {
    return modelProperties.stream().anyMatch(modelProperty -> modelProperty instanceof TypedValueTypeModelProperty);
  }

  private boolean isParameterResolver(Set<ModelProperty> modelProperties) {
    return modelProperties.stream().anyMatch(modelProperty -> modelProperty instanceof ParameterResolverTypeModelProperty);
  }

  /**
   * Gets a {@link ValueResolver} for the parameter if it has an associated a default value or encoding.
   *
   * @param hasDefaultEncoding whether the parameter has to use runtime's default encoding or not
   * @return {@link Supplier} for obtaining the the proper {@link ValueResolver} for the default value, {@code null} if there is
   * no default.
   */
  private ValueResolver<?> getDefaultValueResolver(boolean hasDefaultEncoding, Supplier<ValueResolver<?>> supplier) {
    return hasDefaultEncoding ? new StaticValueResolver<>(muleContext.getConfiguration().getDefaultEncoding()) : supplier.get();
  }
}
