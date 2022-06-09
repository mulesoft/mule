/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.stream.Collectors.toMap;
import static org.mule.metadata.api.utils.MetadataTypeUtils.isEnum;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedElement;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;

import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceCallbackDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.TypedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOutputDeclaration;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverSupplier;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.annotations.CustomDefinedStaticTypeAnnotation;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaAttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaOutputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.JavaOutputResolverModelParserUtils;
import org.mule.sdk.api.metadata.resolving.NamedTypeResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Adapter implementation which expands the {@link MetadataScope} to a more descriptive of the developer's metadata declaration
 * for a {@link ComponentModel component}
 *
 * @since 4.0
 */
public final class DefaultMetadataScopeAdapter implements MetadataScopeAdapter {

  private Supplier<NullMetadataResolver> nullMetadataResolverSupplier = new NullMetadataResolverSupplier();
  private Supplier<? extends TypeKeysResolver> keysResolver = nullMetadataResolverSupplier;
  private Map<String, Supplier<? extends InputTypeResolver>> inputResolvers = new HashMap<>();

  private JavaOutputResolverModelParser javaOutputResolverModelParser =
      new JavaOutputResolverModelParser(org.mule.sdk.api.metadata.NullMetadataResolver.class, false);

  private JavaAttributesResolverModelParser javaAttributesResolverModelParser =
      new JavaAttributesResolverModelParser(org.mule.sdk.api.metadata.NullMetadataResolver.class, false);

  public DefaultMetadataScopeAdapter(Type extensionElement, MethodElement operation, OperationDeclaration declaration) {
    Optional<OutputResolver> outputResolverDeclaration = operation.getAnnotation(OutputResolver.class);
    Optional<Pair<MetadataKeyId, MetadataType>> keyId = locateMetadataKeyId(declaration);

    inputResolvers = getInputResolvers(declaration);

    if (outputResolverDeclaration.isPresent() || !inputResolvers.isEmpty()) {
      outputResolverDeclaration.ifPresent(resolverDeclaration -> {
        if (!hasCustomStaticType(declaration.getOutput())) {
          javaOutputResolverModelParser = JavaOutputResolverModelParserUtils.parseOutputResolverModelParser(operation);
        }
        if (!hasCustomStaticType(declaration.getOutputAttributes())) {
          javaAttributesResolverModelParser = JavaOutputResolverModelParserUtils.parseAttributesResolverModelParser(operation);
        }
      });
      keyId.ifPresent(pair -> keysResolver = getKeysResolver(pair.getRight(), pair.getLeft(),
                                                             () -> getCategoryName(javaOutputResolverModelParser,
                                                                                   javaAttributesResolverModelParser,
                                                                                   inputResolvers)));
    } else {
      initializeFromClass(extensionElement, operation.getEnclosingType(), declaration);
    }
  }

  public DefaultMetadataScopeAdapter(Type extensionElement, Type source, SourceDeclaration sourceDeclaration) {
    initializeFromClass(extensionElement, source, sourceDeclaration);
  }

  public DefaultMetadataScopeAdapter(SourceCallbackDeclaration sourceCallbackDeclaration) {
    inputResolvers = getInputResolvers(sourceCallbackDeclaration);
  }

  private Map<String, Supplier<? extends InputTypeResolver>> getInputResolvers(ParameterizedDeclaration<? extends BaseDeclaration> declaration) {
    return declaration.getAllParameters().stream()
        .filter(p -> getAnnotatedElement(p).map(e -> e.isAnnotationPresent(TypeResolver.class)).orElse(false))
        .filter(p -> !hasCustomStaticType(p))
        .collect(toMap(p -> p.getName(),
                       p -> ResolverSupplier.of(getAnnotatedElement(p).get().getAnnotation(TypeResolver.class).value())));
  }

  private void initializeFromClass(Type extensionType, Type annotatedType, WithOutputDeclaration declaration) {
    // TODO MULE-10891: Add support for Source Callback parameters
    Optional<Class<?>> extensionClass = extensionType.getDeclaringClass();
    Optional<Class<?>> componentClass = annotatedType.getDeclaringClass();

    if (componentClass.isPresent() && extensionClass.isPresent()) {
      MetadataScope scope = getAnnotation(componentClass.get(), MetadataScope.class);
      scope = scope != null ? scope : getAnnotation(extensionClass.get(), MetadataScope.class);

      if (scope != null && !hasCustomStaticType(declaration.getOutput())) {
        this.keysResolver = ResolverSupplier.of(scope.keysResolver());
        this.javaOutputResolverModelParser =
            JavaOutputResolverModelParserUtils.parseOutputResolverModelParser(extensionType, annotatedType);
        this.javaAttributesResolverModelParser =
            JavaOutputResolverModelParserUtils.parseAttributesResolverModelParser(extensionType, annotatedType);
      }
    }
  }

  private Optional<Pair<MetadataKeyId, MetadataType>> locateMetadataKeyId(
                                                                          ComponentDeclaration<? extends ComponentDeclaration> component) {

    Optional<Pair<MetadataKeyId, MetadataType>> keyId = component.getAllParameters().stream()
        .map((declaration) -> new ImmutablePair<>(declaration, getAnnotatedElement(declaration)))
        .filter(p -> p.getRight().isPresent() && p.getRight().get().isAnnotationPresent(MetadataKeyId.class))
        .map(p -> (Pair<MetadataKeyId, MetadataType>) new ImmutablePair<>(p.getRight().get().getAnnotation(MetadataKeyId.class),
                                                                          p.getLeft().getType()))
        .findFirst();

    if (!keyId.isPresent()) {
      for (ParameterGroupDeclaration group : component.getParameterGroups()) {
        keyId = group.getModelProperty(ParameterGroupModelProperty.class)
            .map(ParameterGroupModelProperty::getDescriptor)
            .filter(g -> g.getAnnotatedContainer().isAnnotatedWith(MetadataKeyId.class))
            .map(g -> new ImmutablePair<>(g.getContainer().getAnnotation(MetadataKeyId.class),
                                          g.getType().asMetadataType()));

        if (keyId.isPresent()) {
          break;
        }
      }
    }

    return keyId;
  }

  private Supplier<? extends TypeKeysResolver> getKeysResolver(MetadataType metadataType, MetadataKeyId metadataKeyId,
                                                               Supplier<String> categoryName) {
    Supplier<? extends TypeKeysResolver> keysResolver;
    if (metadataKeyId.value().equals(NullMetadataResolver.class)) {
      if (metadataType instanceof BooleanType) {
        keysResolver = () -> new BooleanKeyResolver(categoryName.get());
      } else if (isEnum(metadataType)) {
        keysResolver = () -> new EnumKeyResolver(metadataType.getAnnotation(EnumAnnotation.class).get(), categoryName.get());
      } else {
        keysResolver = nullMetadataResolverSupplier;
      }
    } else {
      keysResolver = ResolverSupplier.of(metadataKeyId.value());
    }
    return keysResolver;
  }

  private String getCategoryName(JavaOutputResolverModelParser javaOutputResolverModelParser,
                                 JavaAttributesResolverModelParser javaAttributesResolverModelParser,
                                 Map<String, Supplier<? extends InputTypeResolver>> inputResolvers) {

    NamedTypeResolver namedTypeResolver = javaOutputResolverModelParser.getOutputResolver();
    if (!(namedTypeResolver instanceof org.mule.sdk.api.metadata.NullMetadataResolver)) {
      return namedTypeResolver.getCategoryName();
    }

    NamedTypeResolver namedTypeAttributesResolver = javaAttributesResolverModelParser.getAttributesResolver();
    if (!(namedTypeAttributesResolver instanceof org.mule.sdk.api.metadata.NullMetadataResolver)) {
      return namedTypeAttributesResolver.getCategoryName();
    }

    for (Supplier<? extends InputTypeResolver> supplier : inputResolvers.values()) {
      InputTypeResolver inputTypeResolver = supplier.get();
      if (!(inputTypeResolver instanceof NullMetadataResolver)) {
        return inputTypeResolver.getCategoryName();
      }
    }

    throw new IllegalModelDefinitionException("Unable to create Keys Resolver. A Keys Resolver is being defined " +
        "without defining an Output Resolver, Input Resolver nor Attributes Resolver");
  }

  private boolean hasCustomStaticType(TypedDeclaration declaration) {
    return declaration.getType().getAnnotation(CustomDefinedStaticTypeAnnotation.class).isPresent();
  }

  public boolean isCustomScope() {
    return hasOutputResolver() || hasInputResolvers();
  }

  public boolean hasInputResolvers() {
    return !inputResolvers.isEmpty();
  }

  public boolean hasOutputResolver() {
    return javaOutputResolverModelParser.hasOutputResolver();
  }

  public boolean hasAttributesResolver() {
    return javaAttributesResolverModelParser.hasAttributesResolver();
  }

  public Supplier<? extends TypeKeysResolver> getKeysResolver() {
    return keysResolver;
  }

  public Map<String, Supplier<? extends InputTypeResolver>> getInputResolvers() {
    return inputResolvers;
  }

  public org.mule.sdk.api.metadata.resolving.OutputTypeResolver getOutputResolver() {
    return javaOutputResolverModelParser.getOutputResolver();
  }

  public org.mule.sdk.api.metadata.resolving.AttributesTypeResolver getAttributesResolver() {
    return javaAttributesResolverModelParser.getAttributesResolver();
  }
}
