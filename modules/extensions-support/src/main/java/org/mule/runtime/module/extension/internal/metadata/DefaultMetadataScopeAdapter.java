/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.Collections.emptyList;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaInputResolverModelParserUtils.parseInputResolversModelParser;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataKeyIdModelParserUtils.parseKeyIdResolverModelParser;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaOutputResolverModelParserUtils.hasOutputResolverAnnotation;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaOutputResolverModelParserUtils.parseAttributesResolverModelParser;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaOutputResolverModelParserUtils.parseOutputResolverModelParser;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceCallbackDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.TypedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOutputDeclaration;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.annotations.CustomDefinedStaticTypeAnnotation;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaAttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaInputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaKeyIdResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaOutputResolverModelParser;
import org.mule.sdk.api.metadata.resolving.NamedTypeResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Adapter implementation which expands the {@link MetadataScope} to a more descriptive of the developer's metadata declaration
 * for a {@link ComponentModel component}
 *
 * @since 4.0
 */
public final class DefaultMetadataScopeAdapter implements MetadataScopeAdapter {

  private JavaKeyIdResolverModelParser javaKeyIdResolverModelParser =
      new JavaKeyIdResolverModelParser(null, null, org.mule.sdk.api.metadata.NullMetadataResolver.class);

  private JavaOutputResolverModelParser javaOutputResolverModelParser =
      new JavaOutputResolverModelParser(org.mule.sdk.api.metadata.NullMetadataResolver.class, false);

  private JavaAttributesResolverModelParser javaAttributesResolverModelParser =
      new JavaAttributesResolverModelParser(org.mule.sdk.api.metadata.NullMetadataResolver.class, false);

  private List<JavaInputResolverModelParser> javaInputResolverModelParsers = emptyList();

  public DefaultMetadataScopeAdapter(Type extensionElement, MethodElement operation, OperationDeclaration declaration) {
    javaInputResolverModelParsers = parseInputResolversModelParser(operation);

    if (hasOutputResolverAnnotation(operation) || !javaInputResolverModelParsers.isEmpty()) {
      if (!hasCustomStaticType(declaration.getOutput())) {
        javaOutputResolverModelParser = parseOutputResolverModelParser(operation);
      }
      if (!hasCustomStaticType(declaration.getOutputAttributes())) {
        javaAttributesResolverModelParser = parseAttributesResolverModelParser(operation);
      }

      Supplier<String> categoryName = () -> getCategoryName(javaOutputResolverModelParser,
                                                            javaAttributesResolverModelParser, javaInputResolverModelParsers);

      javaKeyIdResolverModelParser = parseKeyIdResolverModelParser(categoryName, operation);
    } else {
      initializeFromClass(extensionElement, operation.getEnclosingType(), declaration);
    }
  }

  public DefaultMetadataScopeAdapter(Type extensionElement, Type source, SourceDeclaration sourceDeclaration) {
    initializeFromClass(extensionElement, source, sourceDeclaration);
  }

  public DefaultMetadataScopeAdapter(SourceCallbackDeclaration sourceCallbackDeclaration) {
    javaInputResolverModelParsers = parseInputResolversModelParser(sourceCallbackDeclaration);
  }

  private void initializeFromClass(Type extensionType, Type annotatedType, WithOutputDeclaration declaration) {
    // TODO MULE-10891: Add support for Source Callback parameters
    Optional<Class<?>> extensionClass = extensionType.getDeclaringClass();
    Optional<Class<?>> componentClass = annotatedType.getDeclaringClass();

    if (componentClass.isPresent() && extensionClass.isPresent()) {
      MetadataScope scope = getAnnotation(componentClass.get(), MetadataScope.class);
      scope = scope != null ? scope : getAnnotation(extensionClass.get(), MetadataScope.class);

      if (scope != null && !hasCustomStaticType(declaration.getOutput())) {
        this.javaKeyIdResolverModelParser =
            parseKeyIdResolverModelParser(extensionType, annotatedType);
        this.javaOutputResolverModelParser =
            parseOutputResolverModelParser(extensionType, annotatedType);
        this.javaAttributesResolverModelParser =
            parseAttributesResolverModelParser(extensionType, annotatedType);
      }
    }
  }

  private String getCategoryName(JavaOutputResolverModelParser javaOutputResolverModelParser,
                                 JavaAttributesResolverModelParser javaAttributesResolverModelParser,
                                 List<JavaInputResolverModelParser> javaInputResolverModelParsers) {

    NamedTypeResolver namedTypeResolver = javaOutputResolverModelParser.getOutputResolver();
    if (!(namedTypeResolver instanceof org.mule.sdk.api.metadata.NullMetadataResolver)) {
      return namedTypeResolver.getCategoryName();
    }

    NamedTypeResolver namedTypeAttributesResolver = javaAttributesResolverModelParser.getAttributesResolver();
    if (!(namedTypeAttributesResolver instanceof org.mule.sdk.api.metadata.NullMetadataResolver)) {
      return namedTypeAttributesResolver.getCategoryName();
    }

    for (JavaInputResolverModelParser inputResolverModelParser : javaInputResolverModelParsers) {
      InputTypeResolver<?> inputTypeResolver = inputResolverModelParser.getInputResolver();
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

  @Override
  public boolean isCustomScope() {
    return hasOutputResolver() || hasInputResolvers();
  }

  @Override
  public boolean hasKeysResolver() {
    return javaKeyIdResolverModelParser.hasKeyIdResolver();
  }

  @Override
  public boolean hasInputResolvers() {
    return !javaInputResolverModelParsers.isEmpty();
  }

  @Override
  public boolean hasOutputResolver() {
    return javaOutputResolverModelParser.hasOutputResolver();
  }

  @Override
  public boolean hasAttributesResolver() {
    return javaAttributesResolverModelParser.hasAttributesResolver();
  }

  @Override
  public boolean isPartialKeyResolver() {
    return javaKeyIdResolverModelParser.isPartialKeyResolver();
  }

  @Override
  public TypeKeysResolver getKeysResolver() {
    return javaKeyIdResolverModelParser.getKeyResolver();
  }

  @Override
  public Map<String, Supplier<? extends InputTypeResolver>> getInputResolvers() {
    Map<String, Supplier<? extends InputTypeResolver>> inputTypeResolvers = new HashMap<>();
    javaInputResolverModelParsers
        .forEach(parser -> inputTypeResolvers.put(parser.getParameterName(), parser::getInputResolver));
    return inputTypeResolvers;
  }

  @Override
  public org.mule.sdk.api.metadata.resolving.OutputTypeResolver getOutputResolver() {
    return javaOutputResolverModelParser.getOutputResolver();
  }

  @Override
  public org.mule.sdk.api.metadata.resolving.AttributesTypeResolver getAttributesResolver() {
    return javaAttributesResolverModelParser.getAttributesResolver();
  }
}
