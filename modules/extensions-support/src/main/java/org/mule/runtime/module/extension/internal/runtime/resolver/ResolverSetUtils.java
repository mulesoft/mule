/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static com.google.common.collect.ImmutableBiMap.of;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getDefaultValue;
import static org.mule.runtime.api.el.BindingContextUtils.PAYLOAD;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.metadata.MediaType.parse;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.util.FunctionalUtils.withNullEvent;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getExpressionSupport;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ComponentParameterizationUtils.createComponentParameterization;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver.fromValues;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.component.ComponentParameterization;
import org.mule.runtime.extension.internal.component.value.HasObjectType;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.resolver.ValueResolverFactory;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
   * @return the corresponding {@link ResolverSet}
   * @throws MuleException
   */
  public static ResolverSet getResolverSetFromStaticValues(ParameterizedModel parameterizedModel,
                                                           Map<String, Object> parameters,
                                                           MuleContext muleContext,
                                                           boolean disableValidations,
                                                           ReflectionCache reflectionCache,
                                                           ExpressionManager expressionManager,
                                                           String parametersOwner)
      throws MuleException {
    return getResolverSetFromComponentParameterization(createComponentParameterization(parameterizedModel, parameters),
                                                       muleContext,
                                                       disableValidations,
                                                       reflectionCache,
                                                       expressionManager, parametersOwner);
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
   * @return the corresponding {@link ResolverSet}
   * @throws MuleException
   */
  public static ResolverSet getResolverSetFromComponentParameterization(ComponentParameterization<?> componentParameterization,
                                                                        MuleContext muleContext,
                                                                        boolean disableValidations,
                                                                        ReflectionCache reflectionCache,
                                                                        ExpressionManager expressionManager,
                                                                        String parametersOwner)
      throws MuleException {
    Map<String, ValueResolver> resolvers = new HashMap<>();
    ValueResolverFactory valueResolverFactory = new ValueResolverFactory();

    for (ParameterGroupModel parameterGroupModel : componentParameterization.getModel().getParameterGroupModels()) {
      resolvers
          .putAll(getParameterGroupValueResolvers(componentParameterization, parameterGroupModel, reflectionCache, muleContext,
                                                  valueResolverFactory));
    }

    return fromValues(resolvers,
                      muleContext,
                      disableValidations,
                      reflectionCache,
                      expressionManager,
                      parametersOwner).getParametersAsResolverSet(componentParameterization.getModel(), muleContext);
  }

  private static Map<String, ValueResolver> getParameterGroupValueResolvers(ComponentParameterization componentParameterization,
                                                                            ParameterGroupModel parameterGroupModel,
                                                                            ReflectionCache reflectionCache,
                                                                            MuleContext muleContext,
                                                                            ValueResolverFactory valueResolverFactory)
      throws MuleException {
    Map<String, ValueResolver> parameterGroupParametersValueResolvers =
        getParameterGroupParametersValueResolvers(parameterGroupModel,
                                                  componentParameterization, reflectionCache, muleContext, valueResolverFactory);

    if (parameterGroupModel.isShowInDsl() && !parameterGroupParametersValueResolvers.isEmpty()) {
      Optional<ParameterGroupModelProperty> parameterGroupModelProperty =
          parameterGroupModel.getModelProperty(ParameterGroupModelProperty.class);

      if (parameterGroupModelProperty.isPresent()) {
        Optional<Class<?>> parameterGroupDeclaringClass = parameterGroupModelProperty.get()
            .getDescriptor().getType().getDeclaringClass();
        if (parameterGroupDeclaringClass.isPresent()) {
          DefaultObjectBuilder defaultObjectBuilder =
              new DefaultObjectBuilder(parameterGroupDeclaringClass.get(), reflectionCache);

          for (Map.Entry<String, ValueResolver> stringValueResolverEntry : parameterGroupParametersValueResolvers.entrySet()) {
            defaultObjectBuilder.addPropertyResolver(stringValueResolverEntry.getKey(),
                                                     stringValueResolverEntry.getValue());

          }
          ValueResolver objectBuilderValuerResolver = new ObjectBuilderValueResolver<>(defaultObjectBuilder, muleContext);
          return of(((Field) parameterGroupModelProperty.get().getDescriptor()
              .getContainer()).getName(), objectBuilderValuerResolver);
        }
      }
    }
    return parameterGroupParametersValueResolvers;
  }

  private static Map<String, ValueResolver> getParameterGroupParametersValueResolvers(ParameterGroupModel parameterGroupModel,
                                                                                      ComponentParameterization componentParameterization,
                                                                                      ReflectionCache reflectionCache,
                                                                                      MuleContext muleContext,
                                                                                      ValueResolverFactory valueResolverFactory)
      throws MuleException {
    Map<String, ValueResolver> parameterGroupParametersValueResolvers = new HashMap<>();
    for (ParameterModel parameterModel : parameterGroupModel.getParameterModels()) {
      Object value = componentParameterization.getParameter(parameterGroupModel, parameterModel);
      if (value != null) {
        parameterGroupParametersValueResolvers.put(parameterModel
            .getName(), getParameterValueResolver(parameterModel.getName(), parameterModel.getType(),
                                                  value,
                                                  parameterModel.getModelProperties(),
                                                  reflectionCache,
                                                  muleContext,
                                                  valueResolverFactory, acceptsReferences(parameterModel)));
      }
    }
    return parameterGroupParametersValueResolvers;
  }

  private static ValueResolver getParameterValueResolver(String parameterName, MetadataType type, Object value,
                                                         Set<ModelProperty> modelProperties, ReflectionCache reflectionCache,
                                                         MuleContext muleContext, ValueResolverFactory valueResolverFactory,
                                                         boolean acceptsReferences)
      throws MuleException {
    Reference<ValueResolver> resolverReference = new Reference<>();

    if (type.getMetadataFormat().equals(MetadataFormat.JAVA)) {
      type.accept(new MetadataTypeVisitor() {

        @Override
        public void visitArrayType(ArrayType arrayType) {
          boolean effectivelyAcceptReferences = acceptsReferences;
          ValueResolver resolver;
          try {
            if (value instanceof Collection) {

              resolver = getParameterValueResolverForCollection(parameterName, arrayType, (Collection) value,
                                                                reflectionCache, muleContext,
                                                                valueResolverFactory);
              effectivelyAcceptReferences = false;
            } else {
              resolver = new StaticValueResolver(value);
            }
            resolverReference
                .set(getValueResolverFor(parameterName, arrayType, resolveAndInjectIfStatic(resolver, muleContext),
                                         getDefaultValue(type),
                                         getExpressionSupport(arrayType), false, modelProperties, effectivelyAcceptReferences,
                                         valueResolverFactory));
          } catch (MuleException e) {
            throw new MuleRuntimeException(e);
          }
        }

        @Override
        public void visitObject(ObjectType objectType) {
          boolean effectivelyAcceptReferences = acceptsReferences;
          ValueResolver resolver;
          try {
            if (isMap(objectType)) {
              if (value instanceof Map) {
                resolver = getParameterValueResolverForMap(parameterName, objectType, (Map) value, reflectionCache,
                                                           muleContext, valueResolverFactory);
                effectivelyAcceptReferences = false;
              } else {
                resolver = new StaticValueResolver(value);
              }
            } else {
              Optional<ValueResolver> pojoResolver =
                  getPojoParameterValueResolver(parameterName, objectType, value, reflectionCache,
                                                muleContext, valueResolverFactory);
              if (pojoResolver.isPresent()) {
                resolver = pojoResolver.get();
                effectivelyAcceptReferences = false;
              } else {
                resolver = new StaticValueResolver(value);
              }
            }
            resolverReference
                .set(getValueResolverFor(parameterName, objectType, resolveAndInjectIfStatic(resolver, muleContext),
                                         getDefaultValue(type),
                                         getExpressionSupport(objectType), false, modelProperties, effectivelyAcceptReferences,
                                         valueResolverFactory));
          } catch (MuleException e) {
            throw new MuleRuntimeException(e);
          }
        }

        @Override
        protected void defaultVisit(MetadataType metadataType) {
          resolverReference.set(getValueResolverFor(parameterName, metadataType, value, getDefaultValue(type),
                                                    getExpressionSupport(metadataType), false, modelProperties,
                                                    acceptsReferences, valueResolverFactory));
        }



      });
    } else {
      Object convertedValue = convertValueWithExpressionLanguage(value, type, muleContext.getExpressionManager());
      resolverReference.set(getValueResolverFor(parameterName, type, convertedValue, null, NOT_SUPPORTED, false, modelProperties,
                                                false, valueResolverFactory));
    }
    initialiseIfNeeded(resolverReference.get(), muleContext);
    return resolverReference.get();
  }

  private static Object convertValueWithExpressionLanguage(Object value, MetadataType type, ExpressionManager expressionManager) {
    TypedValue typedValue = value instanceof TypedValue ? (TypedValue) value : TypedValue.of(value);
    return expressionManager.evaluate(PAYLOAD_EXPRESSION,
                                      DataType.builder().type(getType(type).orElse(Object.class))
                                          .mediaType(getFirstValidMimeType(type))
                                          .build(),
                                      BindingContext.builder().addBinding(PAYLOAD, typedValue).build())
        .getValue();
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

  private static ValueResolver getValueResolverFor(String parameterName, MetadataType metadataType, Object value,
                                                   Object defaultValue, ExpressionSupport expressionSupport, boolean required,
                                                   Set<ModelProperty> modelProperties, boolean acceptsReferences,
                                                   ValueResolverFactory valueResolverFactory) {
    return valueResolverFactory.of(parameterName, metadataType, value, defaultValue,
                                   expressionSupport, required, modelProperties, acceptsReferences);
  }

  private static Object resolveAndInjectIfStatic(ValueResolver valueResolver, MuleContext muleContext) throws MuleException {
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

  private static Optional<ValueResolver> getPojoParameterValueResolver(String parameterName, ObjectType objectType, Object value,
                                                                       ReflectionCache reflectionCache, MuleContext muleContext,
                                                                       ValueResolverFactory valueResolverFactory)
      throws MuleException {

    Optional<Class<Object>> pojoClass = getType(objectType);

    if (pojoClass.isPresent() && value instanceof Map) {
      if (value instanceof HasObjectType) {
        objectType = ((HasObjectType) value).getObjectType();
        pojoClass = getType(objectType);
      }
      DefaultObjectBuilder objectBuilder = new DefaultObjectBuilder(pojoClass.get(), reflectionCache);
      for (ObjectFieldType objectFieldType : objectType.getFields()) {
        Map valuesMap = (Map) value;
        if (valuesMap.containsKey(objectFieldType.getKey().getName().toString())) {
          objectBuilder.addPropertyResolver(objectFieldType.getKey().getName().toString(),
                                            getParameterValueResolver(parameterName, objectFieldType.getValue(),
                                                                      valuesMap
                                                                          .get(objectFieldType.getKey().getName().toString()),
                                                                      emptySet(), reflectionCache,
                                                                      muleContext, valueResolverFactory, false));
        }
      }

      ObjectBuilderValueResolver objectBuilderValueResolver = new ObjectBuilderValueResolver(objectBuilder, muleContext);
      return Optional.of(objectBuilderValueResolver);
    } else {
      return empty();
    }
  }

  private static ValueResolver getParameterValueResolverForCollection(String parameterName, ArrayType arrayType,
                                                                      Collection collection,
                                                                      ReflectionCache reflectionCache,
                                                                      MuleContext muleContext,
                                                                      ValueResolverFactory valueResolverFactory)
      throws MuleException {
    Optional<Class<Object>> expectedType = getType(arrayType);
    if (expectedType.isPresent()) {
      Class type = expectedType.get();
      List<ValueResolver<Object>> itemsResolvers = new ArrayList<>();
      for (Object collectionItem : collection) {
        itemsResolvers
            .add(getParameterValueResolver(parameterName, arrayType.getType(), collectionItem, emptySet(), reflectionCache,
                                           muleContext, valueResolverFactory, false));
      }
      return CollectionValueResolver.of(type, itemsResolvers);
    } else {
      return new StaticValueResolver(collection);
    }
  }

  private static ValueResolver getParameterValueResolverForMap(String parameterName, ObjectType type, Map<Object, Object> map,
                                                               ReflectionCache reflectionCache,
                                                               MuleContext muleContext, ValueResolverFactory valueResolverFactory)
      throws MuleException {
    Optional<Class<Object>> mapClassOptional = getType(type);
    Class mapClass = mapClassOptional.get();

    MetadataType valueType = type.getOpenRestriction().orElse(null);
    Function<Object, ValueResolver<Object>> valueValueResolverFunction;
    if (valueType != null) {
      valueValueResolverFunction = value -> {
        try {
          return getParameterValueResolver(parameterName, valueType, value, emptySet(), reflectionCache,
                                           muleContext, valueResolverFactory, false);
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

  public static boolean acceptsReferences(ParameterModel parameterModel) {
    return parameterModel.getDslConfiguration().allowsReferences();
  }
}
