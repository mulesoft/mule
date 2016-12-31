/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.intersection;
import static org.mule.metadata.internal.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.internal.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isParameterGroup;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.extension.api.util.NameUtils.getModelName;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser.CHILD_ELEMENT_KEY_PREFIX;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser.CHILD_ELEMENT_KEY_SUFFIX;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getContainerName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldByNameOrAlias;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMetadataType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isNullSafe;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.DefaultEncodingAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.NullSafeTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.module.extension.internal.loader.java.property.DefaultEncodingModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.CollectionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.MapValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.NullSafeValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectTypeParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

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
 * Base class for {@link ObjectFactory} implementation which create extension components.
 * <p>
 * Contains behavior to obtain and manage components parameters.
 *
 * @param <T> the generic type of the instances to be built
 * @since 4.0
 */
public abstract class AbstractExtensionObjectFactory<T> extends AbstractAnnotatedObjectFactory<T>
    implements ObjectTypeParametersResolver {

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
  protected ResolverSet getParametersAsResolverSet(ParameterizedModel model)
      throws ConfigurationException {

    List<ParameterGroupModel> inlineGroups = getInlineGroups(model);
    ResolverSet resolverSet = getParametersAsResolverSet(model, getFlatParameters(inlineGroups, model.getAllParameterModels()));

    Map<String, Object> parameters = getParameters();
    inlineGroups.forEach(g -> {
      String containerName = getContainerName(
                                              g.getModelProperty(ParameterGroupModelProperty.class)
                                                  .map(mp -> mp.getDescriptor().getContainer())
                                                  .orElseThrow(() -> new IllegalArgumentException("IllegalArgumentExceptionIllegalArgumentExceptionIllegalArgumentExceptionIllegalArgumentException")));

      if (parameters.containsKey(containerName)) {
        resolverSet.add(containerName, toValueResolver(parameters.get(containerName)));
      }
    });

    return resolverSet;
  }

  protected List<ParameterGroupModel> getInlineGroups(ParameterizedModel model) {
    return model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInline)
        .collect(toList());
  }

  protected List<ParameterModel> getFlatParameters(List<ParameterGroupModel> inlineGroups, List<ParameterModel> parameters) {
    return parameters.stream()
        .filter(p -> inlineGroups.stream().noneMatch(g -> g.getParameterModels().contains(p)))
        .collect(toList());
  }

  protected ResolverSet getParametersAsResolverSet(ParameterizedModel model, List<ParameterModel> parameterModels)
      throws ConfigurationException {

    ResolverSet resolverSet = new ResolverSet();
    Map<String, Object> parameters = getParameters();
    parameterModels.forEach(p -> {
      String parameterName = getMemberName(p, p.getName());
      ValueResolver<?> resolver = null;
      if (parameters.containsKey(parameterName)) {
        resolver = toValueResolver(parameters.get(parameterName));
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

  @Override
  public void resolveParameters(ObjectType objectType, DefaultObjectBuilder builder) {
    final Class<?> objectClass = getType(objectType);
    final boolean isParameterGroup = isParameterGroup(objectType);
    final Map<String, Object> parameters = getParameters();
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
                                                                       "Parameter group '%s' requires that one of its optional parameters should be set but all of them are missing. "
                                                                           + "One of the following should be set: [%s]",
                                                                       group.getName(),
                                                                       Joiner.on(", ")
                                                                           .join(exclusiveModel.getExclusiveParameterNames())))));
        } else if (definedExclusiveParameters.size() > 1) {
          throw buildExclusiveParametersException(model, definedExclusiveParameters);
        }
      }
    }
  }

  private ConfigurationException buildExclusiveParametersException(ParameterizedModel model,
                                                                   Collection<String> definedExclusiveParameters) {
    return new ConfigurationException(
                                      createStaticMessage(format("In %s '%s', the following parameters cannot be set at the same time: [%s]",
                                                                 getComponentModelTypeName(model), getModelName(model),
                                                                 Joiner.on(", ").join(definedExclusiveParameters))));
  }

  /**
   * Gets a {@link ValueResolver} for the parameter if it has an associated a default value or encoding.
   *
   * @param hasDefaultEncoding whether the parameter has to use runtime's default encoding or not
   * @return {@link Supplier} for obtaining the the proper {@link ValueResolver} for the default value, {@code null} if there is
   *         no default.
   */
  private ValueResolver<?> getDefaultValueResolver(boolean hasDefaultEncoding,
                                                   Supplier<ValueResolver<?>> defaultValueResolverSupplier) {
    return hasDefaultEncoding ? new StaticValueResolver<>(muleContext.getConfiguration().getDefaultEncoding())
        : defaultValueResolverSupplier.get();
  }

  private Field getField(Class<?> objectClass, String key) {
    return getFieldByNameOrAlias(objectClass, key)
        .orElseThrow(() -> new IllegalModelDefinitionException(format("Class '%s' does not contain field %s",
                                                                      objectClass.getName(),
                                                                      key)));
  }
}
