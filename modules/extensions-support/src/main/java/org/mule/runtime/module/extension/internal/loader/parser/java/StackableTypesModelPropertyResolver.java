/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static org.mule.runtime.api.metadata.DataType.fromType;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.runtime.parameter.Literal;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackableType;
import org.mule.runtime.module.extension.internal.loader.java.property.stackabletypes.StackedTypesModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionBasedParameterResolverValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionTypedValueValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterResolverValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticLiteralValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticParameterResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypedValueValueResolverWrapper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves the {@link ModelProperty model properties} used to manage parameters defined as {@link StackableType wrapper types}.
 * <p>
 * This class is based on what used to be the {@code StackableTypesParameterContributor} in the 4.1 to 4.4.x version range.
 *
 * @since 4.5
 */
class StackableTypesModelPropertyResolver {

  public static StackableTypesModelPropertyResolver newInstance() {
    return StackableTypesModelPropertyResolver.builder()
        .addType(StackableType
            .builder(ParameterResolver.class)
            .setStaticResolverFactory(value -> new StaticValueResolver<>(new StaticParameterResolver<>(value)))
            .setDelegateResolverFactory(resolver -> new ParameterResolverValueResolverWrapper(resolver))
            .setExpressionBasedResolverFactory((value,
                                                expectedType) -> new ExpressionBasedParameterResolverValueResolver(value,
                                                                                                                   expectedType,
                                                                                                                   fromType(expectedType)))
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
        .addType(StackableType
            .builder(org.mule.sdk.api.runtime.parameter.ParameterResolver.class)
            .setStaticResolverFactory(value -> new StaticValueResolver<>(new StaticParameterResolver<>(value)))
            .setDelegateResolverFactory(resolver -> new ParameterResolverValueResolverWrapper(resolver))
            .setExpressionBasedResolverFactory((value,
                                                expectedType) -> new ExpressionBasedParameterResolverValueResolver(value,
                                                                                                                   expectedType,
                                                                                                                   fromType(expectedType)))
            .build())
        .addType(StackableType
            .builder(org.mule.sdk.api.runtime.parameter.Literal.class)
            .setExpressionBasedResolverFactory((expression, expectedType) -> new StaticLiteralValueResolver(expression,
                                                                                                            expectedType))
            .setStaticResolverFactory((value) -> new StaticLiteralValueResolver(value.toString(), value.getClass()))
            .build())
        .build();
  }

  private final Map<Type, StackableType> stackableTypes;

  private StackableTypesModelPropertyResolver(Map<Type, StackableType> stackableTypes) {
    this.stackableTypes = stackableTypes;
  }

  /**
   * Contributes to a {@link ParameterDeclarer} if the type of the given parameter is one of the registered as
   * {@link StackableType wrapper types}
   *
   * @param parameter          {@link ExtensionParameter} with introspected information of the Java parameter
   * @param declarationContext context of the parameter to be declared
   */
  public List<ModelProperty> resolveStackableProperties(ExtensionParameter parameter,
                                                        ParameterDeclarationContext declarationContext) {
    List<ModelProperty> properties = new LinkedList<>();
    LazyValue<StackedTypesModelProperty.Builder> stackedTypesModelPropertyBuilder =
        new LazyValue<>(StackedTypesModelProperty::builder);

    doResolve(parameter, declarationContext, parameter.getType(), stackedTypesModelPropertyBuilder);
    stackedTypesModelPropertyBuilder.ifComputed(builder -> properties.add(builder.build()));

    return properties;
  }

  private void doResolve(ExtensionParameter extensionParameter,
                         ParameterDeclarationContext declarationContext,
                         Type resolvableType,
                         LazyValue<StackedTypesModelProperty.Builder> builder) {
    getStackableType(resolvableType)
        .ifPresent(stackableType -> {
          List<TypeGeneric> generics = resolvableType.getGenerics();
          if (!generics.isEmpty()) {
            builder.get().addType(stackableType);
            doResolve(extensionParameter, declarationContext, generics.get(0).getConcreteType(), builder);
          } else {
            throw new IllegalParameterModelDefinitionException(
                                                               format(
                                                                      "The parameter [%s] from the %s [%s] doesn't specify the %s parameterized type",
                                                                      extensionParameter.getName(),
                                                                      declarationContext.getComponentType(),
                                                                      declarationContext.getComponentName(),
                                                                      extensionParameter.getType()));
          }
        });

  }

  private Optional<StackableType> getStackableType(Type type) {
    return stackableTypes.entrySet().stream().filter(entry -> entry.getKey().isSameType(type)).map(Map.Entry::getValue)
        .findFirst();
  }

  private static Builder builder() {
    return new Builder();
  }

  private static class Builder {

    private final Map<Type, StackableType> stackableTypes = new HashMap<>();

    public Builder addType(StackableType stackableType) {
      stackableTypes.put(stackableType.getType(), stackableType);
      return this;
    }

    public StackableTypesModelPropertyResolver build() {
      return new StackableTypesModelPropertyResolver(stackableTypes);
    }
  }

}
