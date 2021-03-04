/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.contributor;

import static java.lang.String.format;
import static org.mule.runtime.api.metadata.DataType.fromType;

import org.mule.metadata.api.ClassTypeLoader;
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
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionBasedParameterResolverValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionTypedValueValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterResolverValueResolverWrapper;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticLiteralValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticParameterResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypedValueValueResolverWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ParameterDeclarerContributor} implementation contributes to the parameters of type that are registered as
 * {@link StackableType wrapper types}.
 *
 * @since 4.0
 */
public class StackableTypesParameterContributor implements ParameterDeclarerContributor {

  private final Map<Type, StackableType> stackableTypes;

  private StackableTypesParameterContributor(Map<Type, StackableType> stackableTypes) {
    this.stackableTypes = stackableTypes;
  }

  /**
   * Contributes to a {@link ParameterDeclarer} if the type of the given parameter is one of the registered as
   * {@link StackableType wrapper types}
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

    doContribute(parameter, declarationContext, parameter.getType(), stackedTypesModelPropertyBuilder);
    declarer.ofType(parameter.getType().asMetadataType());
    stackedTypesModelPropertyBuilder.ifComputed(builder -> declarer.withModelProperty(builder.build()));
  }

  private void doContribute(ExtensionParameter extensionParameter, ParameterDeclarationContext declarationContext,
                            Type resolvableType, LazyValue<StackedTypesModelProperty.Builder> builder) {
    getStackableType(resolvableType)
        .ifPresent(stackableType -> {
          List<TypeGeneric> generics = resolvableType.getGenerics();
          if (!generics.isEmpty()) {
            builder.get().addType(stackableType);
            doContribute(extensionParameter, declarationContext, generics.get(0).getConcreteType(), builder);
          } else {
            throw new IllegalParameterModelDefinitionException(
                                                               format(
                                                                      "The parameter [%s] from the %s [%s] doesn't specify the %s parameterized type",
                                                                      extensionParameter.getName(),
                                                                      declarationContext.getComponentType(),
                                                                      declarationContext.getName(),
                                                                      extensionParameter.getType()));
          }
        });

  }

  Optional<StackableType> getStackableType(Type type) {
    return stackableTypes.entrySet().stream().filter(entry -> entry.getKey().isSameType(type)).map(Map.Entry::getValue)
        .findFirst();
  }

  public static Builder builder(ClassTypeLoader typeLoader) {
    return new Builder(typeLoader);
  }

  public static class Builder {

    private final Map<Type, StackableType> stackableTypes = new HashMap<>();
    private final ClassTypeLoader typeLoader;

    public Builder(ClassTypeLoader typeLoader) {
      this.typeLoader = typeLoader;
    }

    public Builder addType(StackableType stackableType) {
      stackableTypes.put(stackableType.getType(), stackableType);
      return this;
    }

    public StackableTypesParameterContributor build() {
      return new StackableTypesParameterContributor(stackableTypes);
    }
  }


  public static StackableTypesParameterContributor defaultContributor(ClassTypeLoader typeLoader) {
    return StackableTypesParameterContributor.builder(typeLoader)
        .addType(StackableType
            .builder(ParameterResolver.class)
            .setStaticResolverFactory(value -> new StaticValueResolver<>(new StaticParameterResolver<>(value)))
            .setDelegateResolverFactory(resolver -> new ParameterResolverValueResolverWrapper(resolver))
            .setExpressionBasedResolverFactory((value, expectedType,
                                                content) -> new ExpressionBasedParameterResolverValueResolver(value, expectedType,
                                                                                                              fromType(expectedType),
                                                                                                              content))
            .build())
        .addType(StackableType
            .builder(TypedValue.class)
            .setStaticResolverFactory(value -> new StaticValueResolver<>(new TypedValue<>(value, DataType.fromObject(value))))
            .setDelegateResolverFactory(valueResolver -> new TypedValueValueResolverWrapper(valueResolver))
            .setExpressionBasedResolverFactory((expression, expectedType,
                                                content) -> new ExpressionTypedValueValueResolver(expression,
                                                                                                  expectedType, content))
            .build())
        .addType(StackableType
            .builder(Literal.class)
            .setExpressionBasedResolverFactory((expression, expectedType, content) -> new StaticLiteralValueResolver(expression,
                                                                                                                     expectedType))
            .setStaticResolverFactory((value) -> new StaticLiteralValueResolver(value.toString(), value.getClass()))
            .build())
        .build();
  }
}
