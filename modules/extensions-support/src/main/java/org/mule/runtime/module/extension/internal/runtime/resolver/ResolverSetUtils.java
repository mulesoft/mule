/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.PAYLOAD;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.metadata.MediaType.parse;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.util.FunctionalUtils.withNullEvent;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getExpressionSupport;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.NameUtils.getAliasName;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ComponentParameterizationUtils.createComponentParameterization;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver.fromValues;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isExpression;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;

import static com.google.common.collect.ImmutableBiMap.of;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.ast.api.MetadataTypeAdapter;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.declaration.type.annotation.ExpressionSupportAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.TypeDslAnnotation;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.resolver.ValueResolverFactory;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Utils class to create {@link ResolverSet}s.
 *
 * @since 4.5
 */
public class ResolverSetUtils {

  private static final String PAYLOAD_EXPRESSION = "#[payload]";
  private static final String APPLICATION_JAVA = "application/java";

  private ResolverSetUtils() {}

  /**
   * Creates a {@link ResolverSet} of a {@link ParameterizedModel} based on static values of its parameters. Keep in mind that
   * this method will not work if the given {@link ParameterizedModel} contains parameter that share the same name, since the map
   * representation falls short in that case.
   *
   * @param parameters         static parameter values.
   * @param parameterizedModel the parameterized group.
   * @param muleContext        the mule context.
   * @param disableValidations whether validations should be disabled or not.
   * @param reflectionCache    a reflection cache.
   * @param expressionManager  the expression manager.
   * @param parametersOwner    the owner of the parameters from the parameters resolver.
   * @param artifactEncoding   the encoding used in the application
   * @return the corresponding {@link ResolverSet}
   * @throws MuleException
   */
  public static ResolverSet getResolverSetFromStaticValues(ParameterizedModel parameterizedModel,
                                                           Map<String, Object> parameters,
                                                           MuleContext muleContext,
                                                           boolean disableValidations,
                                                           ReflectionCache reflectionCache,
                                                           ExpressionManager expressionManager,
                                                           String parametersOwner, ArtifactEncoding artifactEncoding)
      throws MuleException {
    return getResolverSetFromComponentParameterization(createComponentParameterization(parameterizedModel, parameters),
                                                       muleContext,
                                                       disableValidations,
                                                       reflectionCache,
                                                       expressionManager, parametersOwner, artifactEncoding);
  }

  /**
   * Creates a {@link ResolverSet} of a {@link ParameterizedModel} based on static values of its parameters.
   *
   * @param componentParameterization the componentParameterization that describes the model parameter values.
   * @param muleContext               the mule context.
   * @param disableValidations        whether validations should be disabled or not.
   * @param reflectionCache           a reflection cache.
   * @param expressionManager         the expression manager.
   * @param parametersOwner           the owner of the parameters from the parameters resolver.
   * @param artifactEncoding          the encoding used in the application
   * @return the corresponding {@link ResolverSet}
   * @throws MuleException
   */
  public static ResolverSet getResolverSetFromComponentParameterization(ComponentParameterization<?> componentParameterization,
                                                                        MuleContext muleContext,
                                                                        boolean disableValidations,
                                                                        ReflectionCache reflectionCache,
                                                                        ExpressionManager expressionManager,
                                                                        String parametersOwner, ArtifactEncoding artifactEncoding)
      throws MuleException {
    return getResolverSetFromParameters(componentParameterization.getModel(),
                                        componentParameterization::getParameter,
                                        muleContext,
                                        disableValidations,
                                        reflectionCache,
                                        expressionManager,
                                        parametersOwner,
                                        new ValueResolverFactory(),
                                        artifactEncoding);
  }

  /**
   * Creates a {@link ResolverSet} from a set of parameters obtained through the {@code params} function.
   *
   * @param model                the {@link ParameterizedModel} being configured
   * @param params               a {@link BiFunction} to obtain the value of a parameter in a specific group. The group
   *                             <b>MUST</b> be obtained through the {@code model}
   * @param muleContext          the mule context.
   * @param disableValidations   whether validations should be disabled or not.
   * @param reflectionCache      a reflection cache.
   * @param expressionManager    the expression manager.
   * @param parametersOwner      the owner of the parameters from the parameters resolver.
   * @param valueResolverFactory the {@link ValueResolverFactory} to be used
   * @param artifactEncoding     the encoding used in the application
   * @return the corresponding {@link ResolverSet}
   * @throws MuleException
   * @since 4.5.0
   */
  public static ResolverSet getResolverSetFromParameters(ParameterizedModel model,
                                                         BiFunction<ParameterGroupModel, ParameterModel, Object> params,
                                                         MuleContext muleContext,
                                                         boolean disableValidations,
                                                         ReflectionCache reflectionCache,
                                                         ExpressionManager expressionManager,
                                                         String parametersOwner,
                                                         ValueResolverFactory valueResolverFactory,
                                                         ArtifactEncoding artifactEncoding)
      throws MuleException {
    Map<String, ValueResolver> resolvers = new HashMap<>();

    for (ParameterGroupModel parameterGroupModel : model.getParameterGroupModels()) {
      resolvers.putAll(
                       getParameterGroupValueResolvers(params, parameterGroupModel, reflectionCache, muleContext,
                                                       valueResolverFactory, artifactEncoding));
    }

    return fromValues(resolvers,
                      muleContext,
                      disableValidations,
                      reflectionCache,
                      expressionManager,
                      parametersOwner)
                          .getParametersAsResolverSet(model, muleContext);
  }

  private static Map<String, ValueResolver> getParameterGroupValueResolvers(BiFunction<ParameterGroupModel, ParameterModel, Object> params,
                                                                            ParameterGroupModel parameterGroupModel,
                                                                            ReflectionCache reflectionCache,
                                                                            MuleContext muleContext,
                                                                            ValueResolverFactory valueResolverFactory,
                                                                            ArtifactEncoding artifactEncoding) {
    Map<String, ValueResolver> parameterGroupParametersValueResolvers =
        getParameterGroupParametersValueResolvers(parameterGroupModel, params, reflectionCache, muleContext,
                                                  valueResolverFactory, artifactEncoding);

    if (parameterGroupModel.isShowInDsl() && !parameterGroupParametersValueResolvers.isEmpty()) {
      Optional<ParameterGroupModelProperty> parameterGroupModelProperty =
          parameterGroupModel.getModelProperty(ParameterGroupModelProperty.class);

      if (parameterGroupModelProperty.isPresent()) {
        Optional<Class<?>> parameterGroupDeclaringClass = parameterGroupModelProperty.get()
            .getDescriptor().getType().getDeclaringClass();
        if (parameterGroupDeclaringClass.isPresent()) {
          DefaultObjectBuilder defaultObjectBuilder =
              new DefaultObjectBuilder<>(parameterGroupDeclaringClass.get(), reflectionCache);
          defaultObjectBuilder.setEncoding(artifactEncoding.getDefaultEncoding().displayName());

          for (Map.Entry<String, ValueResolver> stringValueResolverEntry : parameterGroupParametersValueResolvers.entrySet()) {
            defaultObjectBuilder.addPropertyResolver(stringValueResolverEntry.getKey(),
                                                     stringValueResolverEntry.getValue());

          }
          ValueResolver objectBuilderValuerResolver = new ObjectBuilderValueResolver<>(defaultObjectBuilder, muleContext);

          return of(getGroupName(parameterGroupModelProperty.orElseThrow()), objectBuilderValuerResolver);
        }
      }
    }
    return parameterGroupParametersValueResolvers;
  }

  private static String getGroupName(ParameterGroupModelProperty groupModelProperty) {
    Object container = groupModelProperty.getDescriptor().getContainer();
    if (container instanceof Field f) {
      return f.getName();
    } else if (container instanceof Parameter p) {
      return p.getName();
    } else {
      throw new IllegalArgumentException("Group container of unexpected type: " + container);
    }
  }


  private static Map<String, ValueResolver> getParameterGroupParametersValueResolvers(ParameterGroupModel parameterGroupModel,
                                                                                      BiFunction<ParameterGroupModel, ParameterModel, Object> params,
                                                                                      ReflectionCache reflectionCache,
                                                                                      MuleContext muleContext,
                                                                                      ValueResolverFactory valueResolverFactory,
                                                                                      ArtifactEncoding artifactEncoding) {
    Map<String, ValueResolver> parameterGroupParametersValueResolvers = new HashMap<>();
    for (ParameterModel parameterModel : parameterGroupModel.getParameterModels()) {
      valueResolverFactory.ofNullableParameter(params, parameterGroupModel, parameterModel,
                                               value -> getParameterValueResolver(parameterModel.getName(),
                                                                                  parameterModel.getType(),
                                                                                  parameterModel.getExpressionSupport(),
                                                                                  value,
                                                                                  parameterModel.getModelProperties(),
                                                                                  reflectionCache,
                                                                                  muleContext,
                                                                                  valueResolverFactory, artifactEncoding,
                                                                                  acceptsReferences(parameterModel)))
          .ifPresent(resolver -> parameterGroupParametersValueResolvers.put(parameterModel.getName(), resolver));
    }
    return parameterGroupParametersValueResolvers;
  }

  private static <T> ValueResolver<T> getParameterValueResolver(String parameterName, MetadataType type,
                                                                ExpressionSupport expressionSupport,
                                                                T value,
                                                                Set<ModelProperty> modelProperties,
                                                                ReflectionCache reflectionCache,
                                                                MuleContext muleContext,
                                                                ValueResolverFactory valueResolverFactory,
                                                                ArtifactEncoding artifactEncoding,
                                                                boolean acceptsReferences)
      throws InitialisationException {
    ValueResolver<T> resolverReference;

    if (type.getMetadataFormat().equals(JAVA)) {
      resolverReference = getJavaParameterValueResolver(parameterName, type, expressionSupport, value, modelProperties,
                                                        reflectionCache, muleContext,
                                                        valueResolverFactory, artifactEncoding, acceptsReferences);
    } else {
      Object convertedValue =
          convertValueWithExpressionLanguage(value, type, expressionSupport, muleContext.getExpressionManager());
      resolverReference = getValueResolverFor(parameterName, type, convertedValue, null, NOT_SUPPORTED, false, modelProperties,
                                              false, valueResolverFactory);
    }
    initialiseIfNeeded(resolverReference, muleContext);
    return resolverReference;
  }

  private static <T> ValueResolver<T> getJavaParameterValueResolver(String parameterName, MetadataType type,
                                                                    ExpressionSupport expressionSupport,
                                                                    T value, Set<ModelProperty> modelProperties,
                                                                    ReflectionCache reflectionCache, MuleContext muleContext,
                                                                    ValueResolverFactory valueResolverFactory,
                                                                    ArtifactEncoding artifactEncoding,
                                                                    boolean acceptsReferences) {
    Reference<ValueResolver<T>> resolverReference = new Reference<>();
    type.accept(new MetadataTypeVisitor() {

      @Override
      public void visitArrayType(ArrayType arrayType) {
        boolean effectivelyAcceptReferences = acceptsReferences;
        ValueResolver<T> resolver;
        try {
          if (value instanceof Collection collection) {
            resolver = getParameterValueResolverForCollection(parameterName, arrayType, expressionSupport,
                                                              collection,
                                                              reflectionCache, muleContext,
                                                              valueResolverFactory, artifactEncoding);
            effectivelyAcceptReferences = false;
          } else {
            resolver = new StaticValueResolver<>(value);
          }
          resolverReference.set(valueResolverFor(parameterName, arrayType, resolveAndInjectIfStatic(resolver, muleContext),
                                                 modelProperties, valueResolverFactory, effectivelyAcceptReferences));
        } catch (MuleException e) {
          throw new MuleRuntimeException(e);
        }
      }

      @Override
      public void visitObject(ObjectType objectType) {
        boolean effectivelyAcceptReferences = acceptsReferences;
        ValueResolver<T> resolver;
        try {
          if (isMap(objectType)) {
            if (value instanceof Map map) {
              resolver =
                  getParameterValueResolverForMap(parameterName, objectType, expressionSupport, map, reflectionCache,
                                                  muleContext, valueResolverFactory, artifactEncoding);
              effectivelyAcceptReferences = false;
            } else {
              resolver = new StaticValueResolver<>(value);
            }
          } else {
            Optional<ValueResolver<T>> pojoResolver =
                getPojoParameterValueResolver(parameterName, objectType, expressionSupport, value, reflectionCache,
                                              muleContext, valueResolverFactory, artifactEncoding);
            if (pojoResolver.isPresent()) {
              resolver = pojoResolver.get();
              effectivelyAcceptReferences = false;
            } else {
              resolver = new StaticValueResolver<>(value);
            }
          }
          resolverReference
              .set(valueResolverFor(parameterName, objectType, resolveAndInjectIfStatic(resolver, muleContext), modelProperties,
                                    valueResolverFactory, effectivelyAcceptReferences));
        } catch (MuleException e) {
          throw new MuleRuntimeException(e);
        }
      }

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        resolverReference
            .set(valueResolverFor(parameterName, metadataType, value, modelProperties, valueResolverFactory, acceptsReferences));
      }

      private ValueResolver<T> valueResolverFor(String parameterName, MetadataType metadataType, final Object value,
                                                Set<ModelProperty> modelProperties, ValueResolverFactory valueResolverFactory,
                                                final boolean acceptsReferences) {
        return getValueResolverFor(parameterName, metadataType, value, getDefaultValue(metadataType),
                                   getExpressionSupport(metadataType), false, modelProperties,
                                   acceptsReferences, valueResolverFactory);
      }
    });

    return resolverReference.get();
  }

  private static Object convertValueWithExpressionLanguage(Object value, MetadataType type, ExpressionSupport expressionSupport,
                                                           ExpressionManager expressionManager) {

    final var mimeType = getFirstValidMimeType(type);
    final var expectedOutputType = DataType.builder().type(getType(type).orElse(Object.class))
        .mediaType(mimeType)
        .build();

    if (!NOT_SUPPORTED.equals(expressionSupport)
        && isExpression(value)) {
      if (PAYLOAD_EXPRESSION.equals(value)) {
        // @Content parameters default to `#[payload]`
        return null;
      } else {
        return expressionManager.evaluate(value.toString(),
                                          expectedOutputType,
                                          NULL_BINDING_CONTEXT)
            .getValue();
      }
    } else {
      TypedValue<?> typedValue = value instanceof TypedValue tv
          ? tv
          : new TypedValue<>(value, DataType.builder().type(value.getClass())
              .mediaType(mimeType)
              .build());

      return expressionManager.evaluate(PAYLOAD_EXPRESSION,
                                        expectedOutputType,
                                        BindingContext.builder().addBinding(PAYLOAD, typedValue).build())
          .getValue();
    }
  }

  private static MediaType getFirstValidMimeType(MetadataType type) {
    Collection<String> validMimeTypes = type.getMetadataFormat().getValidMimeTypes();
    String mimeType;
    if (validMimeTypes.isEmpty()) {
      mimeType = APPLICATION_JAVA;
    } else {
      mimeType = validMimeTypes.iterator().next();
    }
    return parse(mimeType);
  }

  private static <T> ValueResolver<T> getValueResolverFor(String parameterName, MetadataType metadataType, Object value,
                                                          Object defaultValue, ExpressionSupport expressionSupport,
                                                          boolean required,
                                                          Set<ModelProperty> modelProperties, boolean acceptsReferences,
                                                          ValueResolverFactory valueResolverFactory) {
    return valueResolverFactory.of(parameterName, metadataType, value, defaultValue,
                                   expressionSupport, required, modelProperties, acceptsReferences);
  }

  private static Object resolveAndInjectIfStatic(ValueResolver valueResolver, MuleContext muleContext) {
    if (valueResolver.isDynamic()) {
      return valueResolver;
    }

    return withNullEvent(event -> {
      try (
          ValueResolvingContext ctx = ValueResolvingContext.builder(event, muleContext.getExpressionManager()).build()) {
        Object staticProduct = valueResolver.resolve(ctx);
        if (staticProduct != null) {
          muleContext.getInjector().inject(staticProduct);
        }
        return staticProduct;
      }
    });
  }

  private static <T> Optional<ValueResolver<T>> getPojoParameterValueResolver(String parameterName, ObjectType objectType,
                                                                              ExpressionSupport expressionSupport,
                                                                              Object value,
                                                                              ReflectionCache reflectionCache,
                                                                              MuleContext muleContext,
                                                                              ValueResolverFactory valueResolverFactory,
                                                                              ArtifactEncoding artifactEncoding)
      throws InitialisationException {

    Optional<Class<Object>> pojoClass = getType(objectType);

    if (pojoClass.isPresent()) {
      if (value instanceof Map valuesMap) {
        DefaultObjectBuilder objectBuilder = new DefaultObjectBuilder<>(pojoClass.get(), reflectionCache);
        objectBuilder.setEncoding(artifactEncoding.getDefaultEncoding().displayName());
        for (ObjectFieldType objectFieldType : objectType.getFields()) {
          if (valuesMap.containsKey(objectFieldType.getKey().getName().toString())) {
            objectBuilder.addPropertyResolver(objectFieldType.getKey().getName().toString(),
                                              getParameterValueResolver(parameterName, objectFieldType.getValue(),
                                                                        expressionSupport,
                                                                        valuesMap
                                                                            .get(objectFieldType.getKey().getName().toString()),
                                                                        emptySet(), reflectionCache,
                                                                        muleContext, valueResolverFactory, artifactEncoding,
                                                                        false));
          }
        }

        return Optional.of(new ObjectBuilderValueResolver<>(objectBuilder, muleContext));
      } else if (value instanceof ComponentParameterization valuesParameterization) {
        DefaultObjectBuilder objectBuilder = null;
        if (isInstantiableType(objectType)) {
          objectBuilder = new DefaultObjectBuilder<>(pojoClass.get(), reflectionCache);
        } else if (valuesParameterization.getModel() instanceof MetadataTypeAdapter metadataTypeAdapter) {
          Optional<Class<Object>> parameterizedType = getType(metadataTypeAdapter.getType());
          if (parameterizedType.isPresent()) {
            objectBuilder = new DefaultObjectBuilder<>(parameterizedType.get(), reflectionCache);
            objectBuilder.setEncoding(artifactEncoding.getDefaultEncoding().displayName());
            objectType = (ObjectType) metadataTypeAdapter.getType();
          }
        }
        if (objectBuilder == null) {
          throw new IllegalArgumentException(format("Class %s cannot be instantiated.", pojoClass.get()));
        }
        String aliasName = getAliasName(objectType);
        for (ObjectFieldType objectFieldType : objectType.getFields()) {
          Object paramValue = valuesParameterization.getParameter(aliasName, objectFieldType.getKey().getName().getLocalPart());
          if (paramValue != null) {
            objectBuilder.addPropertyResolver(objectFieldType.getKey().getName().toString(),
                                              getParameterValueResolver(parameterName, objectFieldType.getValue(),
                                                                        retrieveExpressionSupport(objectFieldType),
                                                                        paramValue,
                                                                        emptySet(), reflectionCache,
                                                                        muleContext, valueResolverFactory, artifactEncoding,
                                                                        acceptsReferences(objectFieldType.getValue())));
          }
        }

        return Optional.of(new ObjectBuilderValueResolver(objectBuilder, muleContext));
      } else {
        return empty();
      }
    } else {
      return empty();
    }
  }

  private static boolean acceptsReferences(MetadataType type) {
    return type.getAnnotation(TypeDslAnnotation.class).map(TypeDslAnnotation::allowsTopLevelDefinition).orElse(false);
  }

  private static ExpressionSupport retrieveExpressionSupport(ObjectFieldType objectFieldType) {
    return objectFieldType
        .getAnnotation(ExpressionSupportAnnotation.class)
        .map(ExpressionSupportAnnotation::getExpressionSupport)
        .orElse(NOT_SUPPORTED);
  }

  private static boolean isInstantiableType(MetadataType type) {
    return type.getAnnotation(ClassInformationAnnotation.class).map(ClassInformationAnnotation::isInstantiable).orElse(false);
  }

  private static ValueResolver getParameterValueResolverForCollection(String parameterName, ArrayType arrayType,
                                                                      ExpressionSupport expressionSupport,
                                                                      Collection collection,
                                                                      ReflectionCache reflectionCache,
                                                                      MuleContext muleContext,
                                                                      ValueResolverFactory valueResolverFactory,
                                                                      ArtifactEncoding artifactEncoding)
      throws MuleException {
    Optional<Class<Object>> expectedType = getType(arrayType);
    if (expectedType.isPresent()) {
      Class type = expectedType.get();
      List<ValueResolver<Object>> itemsResolvers = new ArrayList<>();
      for (Object collectionItem : collection) {
        itemsResolvers
            .add(getParameterValueResolver(parameterName, arrayType.getType(), expressionSupport,
                                           collectionItem, emptySet(), reflectionCache,
                                           muleContext, valueResolverFactory, artifactEncoding, false));
      }
      return CollectionValueResolver.of(type, itemsResolvers);
    } else {
      return new StaticValueResolver<>(collection);
    }
  }

  private static ValueResolver getParameterValueResolverForMap(String parameterName, ObjectType type,
                                                               ExpressionSupport expressionSupport,
                                                               Map<Object, Object> map,
                                                               ReflectionCache reflectionCache,
                                                               MuleContext muleContext, ValueResolverFactory valueResolverFactory,
                                                               ArtifactEncoding artifactEncoding)
      throws MuleException {
    Class mapClass = getType(type).orElseThrow();

    MetadataType valueType = type.getOpenRestriction().orElse(null);
    Function<Object, ValueResolver<Object>> valueValueResolverFunction;
    if (valueType != null) {
      valueValueResolverFunction = value -> {
        try {
          return getParameterValueResolver(parameterName, valueType, expressionSupport,
                                           value, emptySet(), reflectionCache,
                                           muleContext, valueResolverFactory, artifactEncoding, false);
        } catch (MuleException e) {
          throw new MuleRuntimeException(e);
        }
      };
    } else {
      valueValueResolverFunction = value -> new StaticValueResolver<>(value);
    }

    List<ValueResolver<Object>> keyResolvers = new ArrayList<>();
    List<ValueResolver<Object>> valueResolvers = new ArrayList<>();
    for (Map.Entry<Object, Object> mapEntry : map.entrySet()) {
      keyResolvers.add(new StaticValueResolver<>(mapEntry.getKey().toString()));
      valueResolvers.add(valueValueResolverFunction.apply(mapEntry.getValue()));
    }

    return MapValueResolver.of(mapClass, keyResolvers, valueResolvers, reflectionCache, muleContext);
  }

  private static boolean acceptsReferences(ParameterModel parameterModel) {
    return parameterModel.getDslConfiguration().allowsReferences();
  }

  /**
   * Evaluates the given {@code resolverSet} in the context of a {@code configurationInstance} and {@code event}
   *
   * @param resolverSet           the set to be evaluated
   * @param configurationInstance an optional {@link ConfigurationInstance}
   * @param event                 the context event
   * @return a {@link Map} with the evaluation result
   *
   * @since 4.5.0
   */
  public static Map<String, Object> evaluate(ResolverSet resolverSet,
                                             Optional<ConfigurationInstance> configurationInstance,
                                             CoreEvent event) {
    ValueResolvingContext.Builder ctxBuilder = ValueResolvingContext.builder(event);
    configurationInstance.ifPresent(ctxBuilder::withConfig);

    try (ValueResolvingContext ctx = ctxBuilder.build()) {
      return resolverSet.resolve(ctx).asMap();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Exception found while evaluating parameters:" + e.getMessage()), e);
    }
  }
}
