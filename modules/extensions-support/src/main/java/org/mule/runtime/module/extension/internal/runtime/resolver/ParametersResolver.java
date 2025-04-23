/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.util.collection.SmallMap.forSize;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isFlattenedParameterGroup;
import static org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty.getStackedTypesModelProperty;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.getDefaultValueResolver;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverUtils.getFieldDefaultValueValueResolver;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getContainerName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldByNameOrAlias;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMemberName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMetadataType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isNullSafe;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.primitives.Primitives.wrap;

import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.ConfigOverrideTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.ExclusiveOptionalsTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.NullSafeTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.NullSafeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ExclusiveParameterGroupObjectBuilder;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contains behavior to obtain a ResolverSet for a set of parameters values and a {@link ParameterizedModel}.
 *
 * @since 4.0
 */
public class ParametersResolver implements ObjectTypeParametersResolver {

  private final MuleContext muleContext;
  private final Injector injector;
  private final Map<String, ?> parameters;
  private final ReflectionCache reflectionCache;
  private final ExtendedExpressionManager expressionManager;
  private final String parameterOwner;

  protected ParametersResolver(MuleContext muleContext,
                               Injector injector,
                               Map<String, ?> parameters,
                               ReflectionCache reflectionCache,
                               ExtendedExpressionManager expressionManager,
                               String parameterOwner) {
    this.muleContext = muleContext;
    this.injector = injector;
    this.parameters = parameters;
    this.reflectionCache = reflectionCache;
    this.expressionManager = expressionManager;
    this.parameterOwner = parameterOwner;
  }

  public static ParametersResolver fromValues(Map<String, ?> parameters,
                                              MuleContext muleContext,
                                              Injector injector,
                                              ReflectionCache reflectionCache,
                                              ExtendedExpressionManager expressionManager,
                                              String parameterOwner) {
    return fromValues(parameters, muleContext, injector, true, reflectionCache, expressionManager, parameterOwner);
  }

  public static ParametersResolver fromValues(Map<String, ?> parameters,
                                              MuleContext muleContext,
                                              Injector injector,
                                              boolean disableValidations,
                                              ReflectionCache reflectionCache,
                                              ExtendedExpressionManager expressionManager,
                                              String parameterOwner) {
    if (disableValidations) {
      return new ParametersResolver(muleContext, injector, parameters, reflectionCache, expressionManager, parameterOwner);
    } else {
      return new ValidatingParametersResolver(muleContext, injector, parameters, reflectionCache, expressionManager,
                                              parameterOwner);
    }
  }

  public static ParametersResolver fromDefaultValues(ParameterizedModel parameterizedModel,
                                                     MuleContext muleContext,
                                                     Injector injector,
                                                     ReflectionCache reflectionCache,
                                                     ExtendedExpressionManager expressionManager) {
    List<ParameterModel> allParameterModels = parameterizedModel.getAllParameterModels();
    Map<String, Object> parameterValues = forSize(allParameterModels.size());
    for (ParameterModel model : allParameterModels) {
      parameterValues.put(model.getName(), model.getDefaultValue());
    }

    return new ParametersResolver(muleContext, injector, parameterValues, reflectionCache, expressionManager,
                                  parameterizedModel.getName());
  }

  /**
   * Constructs a {@link ResolverSet} from the parameters, using {@link #toValueResolver(Object, Set)} to process the values.
   *
   * @return a {@link ResolverSet}
   */
  public ResolverSet getParametersAsResolverSet(ParameterizedModel model, MuleContext muleContext) throws ConfigurationException {
    List<ParameterGroupModel> inlineGroups = getInlineGroups(model.getParameterGroupModels());
    List<ParameterModel> flatParameters = getFlatParameters(inlineGroups, model.getAllParameterModels());
    ResolverSet resolverSet = getParametersAsResolverSet(model, flatParameters);
    for (ParameterGroupModel group : inlineGroups) {
      getInlineGroupResolver(group, resolverSet, muleContext);
    }
    return resolverSet;
  }

  public ResolverSet getNestedComponentsAsResolverSet(ComponentModel model) {
    List<? extends NestableElementModel> nestedComponents = model.getNestedComponents();
    ResolverSet resolverSet = new ResolverSet(injector);
    nestedComponents.forEach(nc -> resolverSet.add(nc.getName(), toValueResolver(parameters.get(nc.getName()))));
    return resolverSet;
  }

  /**
   * Constructs a {@link ResolverSet} from the parameters groups, using {@link #toValueResolver(Object, Set)} to process the
   * values.
   *
   * @return a {@link ResolverSet}
   */
  public ResolverSet getParametersAsResolverSet(MuleContext context, ParameterizedModel model, List<ParameterGroupModel> groups)
      throws ConfigurationException {
    List<ParameterGroupModel> inlineGroups = getInlineGroups(groups);
    List<ParameterModel> allParameters = groups.stream().flatMap(g -> g.getParameterModels().stream()).collect(toList());
    ResolverSet resolverSet = getParametersAsResolverSet(model, getFlatParameters(inlineGroups, allParameters));
    for (ParameterGroupModel group : inlineGroups) {
      getInlineGroupResolver(group, resolverSet, context);
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
      resolverSet.add(groupKey, toValueResolver(parameters.get(groupKey), group));
    } else if (descriptor.isPresent()) {
      resolverSet.add(groupKey,
                      NullSafeValueResolverWrapper.of(new StaticValueResolver<>(null), descriptor.get().getMetadataType(),
                                                      reflectionCache,
                                                      muleContext.getTransformationService(),
                                                      expressionManager, muleContext, injector, this));
    } else {
      List<ValueResolver<Object>> keyResolvers = new LinkedList<>();
      List<ValueResolver<Object>> valueResolvers = new LinkedList<>();

      group.getParameterModels().forEach(param -> {
        ValueResolver<Object> parameterValueResolver = getParameterValueResolver(param);
        if (parameterValueResolver != null) {
          keyResolvers.add(new StaticValueResolver<>(param.getName()));
          valueResolvers.add(parameterValueResolver);
        }
      });

      resolverSet.add(groupKey, MapValueResolver.of(HashMap.class, keyResolvers, valueResolvers, reflectionCache, injector));
    }
  }

  public ResolverSet getParametersAsResolverSet(ParameterizedModel model, List<ParameterModel> parameters)
      throws ConfigurationException {
    ResolverSet resolverSet = new ResolverSet(injector);
    return getResolverSet(Optional.of(model), model.getParameterGroupModels(), parameters, resolverSet);
  }

  public ResolverSet getParametersAsResolverSet(List<ParameterGroupModel> groups, List<ParameterModel> parameterModels)
      throws ConfigurationException {
    ResolverSet resolverSet = new ResolverSet(injector);
    return getResolverSet(empty(), groups, parameterModels, resolverSet);
  }

  protected ResolverSet getResolverSet(Optional<ParameterizedModel> model, List<ParameterGroupModel> groups,
                                       List<ParameterModel> parameterModels, ResolverSet resolverSet)
      throws ConfigurationException {
    parameterModels
        .stream()
        .filter(p -> !p.isComponentId()
            // This model property exists only for non synthetic parameters, in which case the value resolver has to be created,
            // regardless of the parameter being the componentId
            || p.getModelProperty(ExtensionParameterDescriptorModelProperty.class).isPresent())
        .forEach(p -> addToResolverSet(p, resolverSet, getParameterValueResolver(p)));

    return resolverSet;
  }

  protected void addToResolverSet(ParameterModel paramModel, final ResolverSet resolverSet, ValueResolver<?> resolver) {
    if (resolver != null) {
      resolverSet.add(getMemberName(paramModel, paramModel.getName()), resolver);
    }
  }

  private ValueResolver<Object> getParameterValueResolver(ParameterModel parameter) {
    ValueResolver<?> resolver;
    String parameterName = parameter.getName();
    if (parameters.containsKey(parameterName)) {
      resolver = toValueResolver(parameters.get(parameterName), parameter);
    } else {
      // TODO MULE-13066 Extract ParameterResolver logic into a centralized resolver
      resolver = getDefaultValueResolver(parameter, muleContext.getTransformationService(), expressionManager, injector);
    }

    if (isNullSafe(parameter)) {
      ValueResolver<?> delegate = resolver != null ? resolver : new StaticValueResolver<>(null);
      Optional<MetadataType> type =
          parameter.getModelProperty(NullSafeModelProperty.class).map(NullSafeModelProperty::defaultType);
      if (type.isPresent()) {
        resolver =
            NullSafeValueResolverWrapper.of(delegate, type.get(), reflectionCache,
                                            muleContext.getTransformationService(),
                                            expressionManager, this.muleContext, injector,
                                            this);
      }
    }

    if (parameter.isOverrideFromConfig()) {
      AtomicReference<Class<?>> simpleTypeClass = new AtomicReference<>();

      parameter.getType().accept(new MetadataTypeVisitor() {

        @Override
        public void visitString(StringType stringType) {
          // enum values are represented as strings
          simpleTypeClass.set(ExtensionMetadataTypeUtils.<String>getType(stringType).orElse(String.class));
        }

        @Override
        public void visitBoolean(BooleanType booleanType) {
          simpleTypeClass.set(Boolean.class);
        }

        @Override
        public void visitNumber(NumberType numberType) {
          simpleTypeClass.set(ExtensionMetadataTypeUtils.<Number>getType(numberType).orElse(Number.class));
        }

      });

      if (simpleTypeClass.get() != null) {
        resolver = ConfigOverrideValueResolverWrapper.of(resolver != null ? resolver : new StaticValueResolver<>(null),
                                                         parameterName, wrap(simpleTypeClass.get()),
                                                         reflectionCache, this.muleContext, parameterOwner);
      } else {
        resolver = ConfigOverrideValueResolverWrapper.of(resolver != null ? resolver : new StaticValueResolver<>(null),
                                                         parameterName, parameter.getType(),
                                                         reflectionCache, this.muleContext, parameterOwner);
      }

    }
    return (ValueResolver<Object>) resolver;
  }

  private List<ParameterGroupModel> getInlineGroups(List<ParameterGroupModel> groups) {
    return groups.stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .collect(toList());
  }

  private List<ParameterModel> getFlatParameters(List<ParameterGroupModel> inlineGroups, List<ParameterModel> parameters) {
    return parameters.stream()
        .filter(p -> inlineGroups.stream().noneMatch(g -> g.getParameterModels().contains(p)))
        .collect(toList());
  }

  private DefaultObjectBuilder getParameterGroupObjectBuilder(ObjectFieldType groupField) {
    Class<Object> type = getType(groupField.getValue());
    if (groupField.getAnnotation(ExclusiveOptionalsTypeAnnotation.class).isPresent()) {
      return new ExclusiveParameterGroupObjectBuilder(type,
                                                      groupField.getAnnotation(ExclusiveOptionalsTypeAnnotation.class).get(),
                                                      reflectionCache);
    }

    return new DefaultObjectBuilder(type, reflectionCache);
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
          DefaultObjectBuilder groupBuilder = getParameterGroupObjectBuilder(groupField);
          builder.addPropertyResolver(objectField, new ObjectBuilderValueResolver<>(groupBuilder, muleContext));

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
      Field objectField = getField(objectClass, key);
      ValueResolver<?> valueResolver = resolveObjectFieldParameter(objectClass, objectField, field, key, isParameterGroup);

      addPropertyResolver(builder, valueResolver, field, objectField);
    });
  }

  private ValueResolver<?> resolveObjectFieldParameter(final Class<?> objectClass, Field objectField, ObjectFieldType field,
                                                       final String key, final boolean isParameterGroup) {
    ValueResolver<?> valueResolver = null;

    if (parameters.containsKey(key)) {
      valueResolver = toValueResolver(parameters.get(key));
    } else if (!isParameterGroup) {
      valueResolver = getDefaultValue(field).isPresent()
          ? getFieldDefaultValueValueResolver(field, muleContext.getTransformationService(), expressionManager, injector)
          : null;
    }

    Optional<NullSafeTypeAnnotation> nullSafe = field.getAnnotation(NullSafeTypeAnnotation.class);
    if (nullSafe.isPresent()) {
      ValueResolver<?> delegate = valueResolver != null ? valueResolver : new StaticValueResolver<>(null);
      MetadataType type =
          getMetadataType(nullSafe.get().getType(), ExtensionsTypeLoaderFactory.getDefault().createTypeLoader());
      valueResolver =
          NullSafeValueResolverWrapper.of(delegate, type, reflectionCache,
                                          muleContext.getTransformationService(),
                                          expressionManager, muleContext, injector, this);
    }

    if (field.getAnnotation(ConfigOverrideTypeAnnotation.class).isPresent()) {
      valueResolver =
          ConfigOverrideValueResolverWrapper.of(valueResolver != null ? valueResolver : new StaticValueResolver<>(null),
                                                key, objectField.getType(), reflectionCache, muleContext,
                                                objectClass.getName());
    }
    return valueResolver;
  }

  protected void addPropertyResolver(DefaultObjectBuilder builder, ValueResolver<?> valueResolver, ObjectFieldType field,
                                     Field objectField) {
    if (valueResolver != null) {
      try {
        initialiseIfNeeded(valueResolver, injector);
        builder.addPropertyResolver(objectField, valueResolver);
      } catch (InitialisationException e) {
        throw new MuleRuntimeException(e);
      }
    }
  }

  private Field getField(Class<?> objectClass, String key) {
    return getFieldByNameOrAlias(objectClass, key, reflectionCache)
        .orElseThrow(() -> new IllegalModelDefinitionException(format("Class '%s' does not contain field %s",
                                                                      objectClass.getName(),
                                                                      key)));
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
  private <T> ValueResolver<T> toValueResolver(Object value) {
    return (ValueResolver<T>) toValueResolver(value, null);
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
  private ValueResolver<?> toValueResolver(Object value, EnrichableModel withModelPropertiesModel) {
    ValueResolver<?> resolver;
    if (value instanceof ValueResolver) {
      resolver = (ValueResolver<?>) value;
    } else if (value instanceof Collection) {
      resolver = getCollectionResolver((Collection) value);
    } else if (value instanceof Map) {
      resolver = getMapResolver((Map<Object, Object>) value);
    } else {
      final Optional<StackedTypesModelProperty> stackedTypesModelProperty = withModelPropertiesModel == null
          ? empty()
          : getStackedTypesModelProperty(withModelPropertiesModel);
      if (stackedTypesModelProperty.isPresent()) {
        resolver = stackedTypesModelProperty.get().getValueResolverFactory().getStaticValueResolver(value);
      } else if (value instanceof ConfigurationProvider) {
        resolver = new ConfigurationValueResolver<>((ConfigurationProvider) value);
      } else {
        resolver = new StaticValueResolver<>(value);
        if (value instanceof ObjectStore) {
          resolver = new LifecycleInitialiserValueResolverWrapper<>(resolver, muleContext);
        }
      }
    }
    return resolver;
  }

  private ValueResolver<?> getMapResolver(Map<Object, Object> value) {
    Map<ValueResolver<Object>, ValueResolver<Object>> normalizedMap = new LinkedHashMap<>(value.size());
    value.forEach((key, entryValue) -> normalizedMap.put((ValueResolver<Object>) toValueResolver(key),
                                                         (ValueResolver<Object>) toValueResolver(entryValue)));
    return MapValueResolver.of(value.getClass(), copyOf(normalizedMap.keySet()), copyOf(normalizedMap.values()), reflectionCache,
                               injector);
  }

  private ValueResolver<?> getCollectionResolver(Collection<?> collection) {
    return CollectionValueResolver.of(collection.getClass(),
                                      collection.stream().map(this::toValueResolver).toList());
  }

  protected Map<String, ?> getParameters() {
    return parameters;
  }
}
