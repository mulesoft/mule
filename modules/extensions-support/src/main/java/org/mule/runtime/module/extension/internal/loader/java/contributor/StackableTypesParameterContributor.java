/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.contributor;

import static java.lang.String.format;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackableType;
import org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionBasedParameterResolverValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionTypedValueValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterResolverValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticLiteralValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticParameterResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypedValueValueResolverWrapper;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.ResolvableType;

/**
 * {@link ParameterDeclarerContributor} implementation contributes to the parameters of type that are registered as
 * {@link StackableType wrapper types}.
 *
 * @since 4.0
 */
public class StackableTypesParameterContributor implements ParameterDeclarerContributor {

  private final ClassTypeLoader typeLoader;
  private Map<Class, StackableType> stackableTypes;

  private StackableTypesParameterContributor(ClassTypeLoader typeLoader, Map<Class, StackableType> stackableTypes) {
    this.typeLoader = typeLoader;
    this.stackableTypes = stackableTypes;
  }

  /**
   * Contributes to a {@link ParameterDeclarer} if the type of the given parameter is one of the registered
   * as {@link StackableType wrapper types}
   *
   * @param parameter {@link ExtensionParameter} with introspected information of the Java parameter
   * @param declarer declarer to be enriched
   * @param declarationContext context of the parameter to be declared
   */
  @Override
  public void contribute(ExtensionParameter parameter, ParameterDeclarer declarer,
                         ParameterDeclarationContext declarationContext) {
    LazyValue<StackedTypesModelProperty.Builder> stackedTypesModelPropertyBuilder =
        new LazyValue<>(StackedTypesModelProperty::builder);
    ResolvableType resolvableType = ResolvableType.forType(parameter.getJavaType());

    doContribute(parameter, declarationContext, resolvableType, stackedTypesModelPropertyBuilder);
    declarer.ofType(typeLoader.load(resolvableType.getType()));
    stackedTypesModelPropertyBuilder.ifComputed(builder -> declarer.withModelProperty(builder.build()));
  }

  private void doContribute(ExtensionParameter extensionParameter, ParameterDeclarationContext declarationContext,
                            ResolvableType resolvableType, LazyValue<StackedTypesModelProperty.Builder> builder) {
    if (stackableTypes.containsKey(resolvableType.getRawClass())) {
      ResolvableType[] generics = resolvableType.getGenerics();
      if (generics.length > 0) {
        builder.get().addType(stackableTypes.get(resolvableType.getRawClass()));
        doContribute(extensionParameter, declarationContext, generics[0], builder);
      } else {
        throw new IllegalParameterModelDefinitionException(
                                                           format(
                                                                  "The parameter [%s] from the %s [%s] doesn't specify the %s parameterized type",
                                                                  extensionParameter.getName(),
                                                                  declarationContext.getComponentType(),
                                                                  declarationContext.getName(), extensionParameter.getType()));
      }
    }
  }

  public static Builder builder(ClassTypeLoader typeLoader) {
    return new Builder(typeLoader);
  }

  public static class Builder {

    private Map<Class, StackableType> stackableTypes = new HashMap<>();
    private ClassTypeLoader typeLoader;

    public Builder(ClassTypeLoader typeLoader) {
      this.typeLoader = typeLoader;
    }

    public Builder addType(StackableType stackableType) {
      stackableTypes.put(stackableType.getType(), stackableType);
      return this;
    }

    public StackableTypesParameterContributor build() {
      return new StackableTypesParameterContributor(typeLoader, stackableTypes);
    }
  }

  public static StackableTypesParameterContributor defaultContributor(ClassTypeLoader typeLoader) {
    return StackableTypesParameterContributor.builder(typeLoader)
        .addType(StackableType
            .builder(ParameterResolver.class)
            .setStaticResolverFactory(value -> new StaticValueResolver<>(new StaticParameterResolver<>(value)))
            .setDelegateResolverFactory(resolver -> new ParameterResolverValueResolverWrapper(resolver))
            .setExpressionBasedResolverFactory((value, expectedType) -> new ExpressionBasedParameterResolverValueResolver(value,
                                                                                                                          typeLoader
                                                                                                                              .load(expectedType)))
            .build())
        .addType(StackableType
            .builder(TypedValue.class)
            .setStaticResolverFactory(value -> new StaticValueResolver<>(new TypedValue<>(value, DataType.fromObject(value))))
            .setDelegateResolverFactory(valueResolver -> new TypedValueValueResolverWrapper(valueResolver))
            .setExpressionBasedResolverFactory((expression, expectedType) -> new ExpressionTypedValueValueResolver(expression,
                                                                                                                   expectedType))
            .build())
        .addType(StackableType
            .builder(Literal.class)
            .setExpressionBasedResolverFactory((expression, expectedType) -> new StaticLiteralValueResolver(expression,
                                                                                                            expectedType))
            .setStaticResolverFactory((value) -> new StaticLiteralValueResolver(value.toString(), value.getClass()))
            .build())
        .build();
  }
}
