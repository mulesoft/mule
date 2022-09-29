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
import static org.mule.runtime.module.extension.internal.loader.utils.JavaOutputResolverModelParserUtils.hasMetadataScopeAnnotation;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaOutputResolverModelParserUtils.hasOutputResolverAnnotation;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaOutputResolverModelParserUtils.parseJavaAttributesResolverModelParser;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaOutputResolverModelParserUtils.parseJavaOutputResolverModelParser;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceCallbackDeclaration;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.NamedTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.parser.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaAttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaKeyIdResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaOutputResolverModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataKeyIdModelParserUtils;
import org.mule.runtime.module.extension.internal.loader.utils.JavaOutputResolverModelParserUtils;

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
      new JavaKeyIdResolverModelParser(null, null, null, org.mule.sdk.api.metadata.NullMetadataResolver.class);

  private JavaOutputResolverModelParser javaOutputResolverModelParser =
      new JavaOutputResolverModelParser(org.mule.sdk.api.metadata.NullMetadataResolver.class);

  private JavaAttributesResolverModelParser javaAttributesResolverModelParser =
      new JavaAttributesResolverModelParser(org.mule.sdk.api.metadata.NullMetadataResolver.class);

  private List<InputResolverModelParser> javaInputResolverModelParsers = emptyList();

  public DefaultMetadataScopeAdapter(Type extensionElement, MethodElement operation) {
    javaInputResolverModelParsers = parseInputResolversModelParser(operation);

    if (hasOutputResolverAnnotation(operation) || !javaInputResolverModelParsers.isEmpty()) {
      javaOutputResolverModelParser = parseJavaOutputResolverModelParser(operation);
      javaAttributesResolverModelParser = JavaOutputResolverModelParserUtils.parseJavaAttributesResolverModelParser(operation);
      javaKeyIdResolverModelParser = parseKeyIdResolverModelParser(this::getCategoryName, operation);
    } else {
      initializeFromClass(extensionElement, operation.getEnclosingType());
    }
  }

  public DefaultMetadataScopeAdapter(Type extensionElement, Type source) {
    initializeFromClass(extensionElement, source);
  }

  public DefaultMetadataScopeAdapter(SourceCallbackDeclaration sourceCallbackDeclaration) {
    javaInputResolverModelParsers = parseInputResolversModelParser(sourceCallbackDeclaration);
  }

  private void initializeFromClass(Type extensionType, Type annotatedType) {
    // TODO MULE-10891: Add support for Source Callback parameters
    Optional<Class<?>> extensionClass = extensionType.getDeclaringClass();
    Optional<Class<?>> componentClass = annotatedType.getDeclaringClass();

    if (componentClass.isPresent() && extensionClass.isPresent()) {
      boolean hasMetadataScopeAnnotation = hasMetadataScopeAnnotation(componentClass.get()) ||
          hasMetadataScopeAnnotation(extensionClass.get());

      if (hasMetadataScopeAnnotation) {
        this.javaKeyIdResolverModelParser =
            JavaMetadataKeyIdModelParserUtils.parseJavaKeyIdResolverModelParser(extensionType, annotatedType);
        this.javaOutputResolverModelParser =
            JavaOutputResolverModelParserUtils.parseJavaOutputResolverModelParser(extensionType, annotatedType);
        this.javaAttributesResolverModelParser =
            parseJavaAttributesResolverModelParser(extensionType, annotatedType);
      }
    }
  }

  private String getCategoryName() {
    NamedTypeResolver namedTypeResolver = javaOutputResolverModelParser.getOutputResolver();
    if (javaOutputResolverModelParser.hasOutputResolver()) {
      return namedTypeResolver.getCategoryName();
    }

    NamedTypeResolver namedTypeAttributesResolver = javaAttributesResolverModelParser.getAttributesResolver();
    if (javaAttributesResolverModelParser.hasAttributesResolver()) {
      return namedTypeAttributesResolver.getCategoryName();
    }

    for (InputResolverModelParser inputResolverModelParser : javaInputResolverModelParsers) {
      InputTypeResolver<?> inputTypeResolver = inputResolverModelParser.getInputResolver();
      if (!(inputTypeResolver instanceof NullMetadataResolver)) {
        return inputTypeResolver.getCategoryName();
      }
    }

    throw new IllegalModelDefinitionException("Unable to create Keys Resolver. A Keys Resolver is being defined " +
        "without defining an Output Resolver, Input Resolver nor Attributes Resolver");
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
  public OutputTypeResolver getOutputResolver() {
    return javaOutputResolverModelParser.getOutputResolver();
  }

  @Override
  public AttributesTypeResolver getAttributesResolver() {
    return javaAttributesResolverModelParser.getAttributesResolver();
  }

  @Override
  public MetadataType getKeyResolverMetadataType() {
    return javaKeyIdResolverModelParser.getMetadataType();
  }

  @Override
  public String getKeyResolverParameterName() {
    return javaKeyIdResolverModelParser.getParameterName();
  }
}
