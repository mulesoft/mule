/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static com.google.common.collect.ImmutableBiMap.of;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver.fromValues;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isExpression;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.resolver.ValueResolverFactory;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.sdk.api.runtime.parameter.Literal;
import org.mule.sdk.api.runtime.parameter.ParameterResolver;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utils class to create {@link ResolverSet}s.
 *
 * @since 4.5
 */
public class ResolverSetUtils {

  /**
   * Creates a {@link ResolverSet} of a {@link ParameterizedModel} based on static values of its parameters.
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
                                                           String parametersOwner,
                                                           DslSyntaxResolver dslSyntaxResolver)
      throws MuleException {
    Map<String, ValueResolver> resolvers = new HashMap<>();
    ValueResolverFactory valueResolverFactory = new ValueResolverFactory(dslSyntaxResolver);

    for (ParameterGroupModel parameterGroupModel : parameterizedModel.getParameterGroupModels()) {
      resolvers.putAll(getParameterGroupValueResolvers(parameterGroupModel, parameters, reflectionCache, muleContext,
                                                       valueResolverFactory));
    }

    return fromValues(resolvers,
                      muleContext,
                      disableValidations,
                      reflectionCache,
                      expressionManager,
                      parametersOwner).getParametersAsResolverSet(parameterizedModel, muleContext);
  }

  private static Map<String, ValueResolver> getParameterGroupValueResolvers(ParameterGroupModel parameterGroupModel,
                                                                            Map<String, Object> parameters,
                                                                            ReflectionCache reflectionCache,
                                                                            MuleContext muleContext,
                                                                            ValueResolverFactory valueResolverFactory)
      throws MuleException {
    Map<String, ValueResolver> parameterGroupParametersValueResolvers =
        getParameterGroupParametersValueResolvers(parameterGroupModel,
                                                  parameters, reflectionCache, muleContext, valueResolverFactory);

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
                                                                                      Map<String, Object> parameters,
                                                                                      ReflectionCache reflectionCache,
                                                                                      MuleContext muleContext,
                                                                                      ValueResolverFactory valueResolverFactory)
      throws MuleException {
    Map<String, ValueResolver> parameterGroupParametersValueResolvers = new HashMap<>();
    for (ParameterModel parameterModel : parameterGroupModel.getParameterModels()) {
      if (parameters.containsKey(parameterModel.getName())) {
        parameterGroupParametersValueResolvers.put(parameterModel.getName(), getParameterValueResolver(parameterModel,
                                                                                                       parameters
                                                                                                           .get(parameterModel
                                                                                                               .getName()),
                                                                                                       reflectionCache,
                                                                                                       muleContext,
                                                                                                       valueResolverFactory));
      }
    }
    return parameterGroupParametersValueResolvers;
  }

  private static ValueResolver getParameterValueResolver(ParameterModel parameterModel, Object value,
                                                         ReflectionCache reflectionCache, MuleContext muleContext,
                                                         ValueResolverFactory valueResolverFactory)
      throws MuleException {

    return valueResolverFactory.of(parameterModel.getName(), parameterModel.getType(), value, parameterModel.getDefaultValue(),
                                   parameterModel.getExpressionSupport(), parameterModel.isRequired(),
                                   parameterModel.getModelProperties(), false);

    // Optional<ExtensionParameterDescriptorModelProperty> extensionParameterDescriptorModelProperty =
    // parameterModel.getModelProperty(ExtensionParameterDescriptorModelProperty.class);
    //
    // if (!extensionParameterDescriptorModelProperty.isPresent()) {
    // return new StaticValueResolver(value);
    // }
    //
    // Optional<StackedTypesModelProperty> stackedTypesModelProperty =
    // parameterModel.getModelProperty(StackedTypesModelProperty.class);
    //
    // Type parameterType = extensionParameterDescriptorModelProperty.get().getExtensionParameter().getType();
    //
    // if (stackedTypesModelProperty.isPresent()) {
    // if (isExpression(value)) {
    // return stackedTypesModelProperty.get().getValueResolverFactory()
    // .getExpressionBasedValueResolver((String) value,
    // getNonSpecialType(parameterType).getDeclaringClass().orElse(Object.class));
    // } else {
    // // CHECK OPTIONAL!!!
    // return stackedTypesModelProperty.get().getValueResolverFactory()
    // .getStaticValueResolver(resolveStaticValue(getNonSpecialType(parameterType), value,
    // reflectionCache, muleContext),
    // /* check optional */ parameterType.getDeclaringClass().get())
    // .get();
    // }
    // }
    //
    // return getParameterValueResolver(parameterType, value, reflectionCache, muleContext);
  }

  private static Object resolveStaticValue(Type type, Object value, ReflectionCache reflectionCache,
                                           MuleContext muleContext)
      throws MuleException {
    ValueResolver resolver = getParameterValueResolver(type, value, reflectionCache, muleContext);
    if (!resolver.isDynamic()) {
      return resolver.resolve(ValueResolvingContext.builder(getNullEvent(muleContext)).build());
    }
    return value;
  }

  private static Type getNonSpecialType(Type parameterType) {
    Class expectedType = parameterType.getDeclaringClass().orElse(null);
    if (TypedValue.class.isAssignableFrom(expectedType)) {
      List<Type> parameterTypeGenerics = parameterType.getSuperTypeGenerics(TypedValue.class);
      if (parameterTypeGenerics.size() == 1) {
        return parameterTypeGenerics.get(0);
      }
    } else if (ParameterResolver.class.isAssignableFrom(expectedType)) {
      List<Type> parameterTypeGenerics = parameterType.getSuperTypeGenerics(ParameterResolver.class);
      if (parameterTypeGenerics.size() == 1) {
        return parameterTypeGenerics.get(0);
      }
    } else if (Literal.class.isAssignableFrom(expectedType)) {
      List<Type> parameterTypeGenerics = parameterType.getSuperTypeGenerics(Literal.class);
      if (parameterTypeGenerics.size() == 1) {
        return parameterTypeGenerics.get(0);
      }
    }
    return parameterType;
  }

  // TODO W-10992158: Already existing value resolving processing must be reviewed to avoid code duplication.
  private static ValueResolver getParameterValueResolver(Type parameterType, Object value, ReflectionCache reflectionCache,
                                                         MuleContext muleContext)
      throws MuleException {
    ValueResolver valueResolver;
    // TODO W-10992158: Value resolution must take into account stackable types, utilizing the StackableTypesValueResolverFactory
    Class expectedType = parameterType.getDeclaringClass().orElse(null);

    if (expectedType == null) {
      return new StaticValueResolver(value);
    }

    if (value instanceof Collection && Collection.class.isAssignableFrom(expectedType)) {
      valueResolver = getParameterValueResolverForCollection(parameterType, (Collection) value, reflectionCache, muleContext);
    } else if (value instanceof Map && Map.class.isAssignableFrom(expectedType)) {
      valueResolver = getParameterValueResolverForMap(parameterType, (Map) value, reflectionCache, muleContext);
    } else {
      // TODO W-10992158: Resolution must take into account the way date type are converted from DSL in
      // org.mule.runtime.module.extension.internal.config.dsl.resolver.ValueResolverFactoryTypeVisitor.doParseDate
      valueResolver = new ExpressionLanguageTransformationValueResolver(new StaticValueResolver(value), expectedType,
                                                                        muleContext.getExpressionManager());
    }

    valueResolver = new TypeSafeValueResolverWrapper(valueResolver, expectedType);

    muleContext.getInjector().inject(valueResolver);
    initialiseIfNeeded(valueResolver);

    return valueResolver;
  }

  private static ValueResolver getParameterValueResolverForCollection(Type parameterType, Collection collection,
                                                                      ReflectionCache reflectionCache,
                                                                      MuleContext muleContext)
      throws MuleException {
    Class expectedType = parameterType.getDeclaringClass().orElse(Collection.class);
    List<Type> parameterTypeCollectionGenerics = parameterType.getSuperTypeGenerics(Collection.class);
    if (parameterTypeCollectionGenerics.size() == 1) {
      List<ValueResolver<Object>> itemsResolvers = new ArrayList<>();
      for (Object collectionItem : collection) {
        itemsResolvers
            .add(getParameterValueResolver(parameterTypeCollectionGenerics.get(0), collectionItem, reflectionCache, muleContext));
      }

      return CollectionValueResolver.of(expectedType, itemsResolvers);
    } else {
      return new StaticValueResolver(collection);
    }
  }

  private static ValueResolver getParameterValueResolverForMap(Type parameterType, Map<Object, Object> map,
                                                               ReflectionCache reflectionCache,
                                                               MuleContext muleContext)
      throws MuleException {
    Class expectedType = parameterType.getDeclaringClass().orElse(Map.class);
    List<Type> parameterTypeCollectionGenerics = parameterType.getSuperTypeGenerics(Map.class);
    if (parameterTypeCollectionGenerics.size() == 2) {
      List<ValueResolver<Object>> keyResolvers = new ArrayList<>();
      List<ValueResolver<Object>> valueResolvers = new ArrayList<>();
      for (Map.Entry<Object, Object> mapEntry : map.entrySet()) {
        keyResolvers.add(getParameterValueResolver(parameterTypeCollectionGenerics.get(0), mapEntry.getKey(), reflectionCache,
                                                   muleContext));
        valueResolvers.add(getParameterValueResolver(parameterTypeCollectionGenerics.get(1), mapEntry.getValue(), reflectionCache,
                                                     muleContext));
      }

      return MapValueResolver.of(expectedType, keyResolvers, valueResolvers, reflectionCache, muleContext);
    } else {
      return new StaticValueResolver(map);
    }
  }

}
