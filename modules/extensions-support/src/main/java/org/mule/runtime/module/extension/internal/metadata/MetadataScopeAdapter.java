/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedElement;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter implementation which expands the {@link MetadataScope} to a more descriptive of the developer's metadata declaration
 * for a {@link ComponentModel component}
 *
 * @since 4.0
 */
public final class MetadataScopeAdapter {

  private Class<? extends TypeKeysResolver> keysResolver = NullMetadataResolver.class;
  private Class<? extends OutputTypeResolver> outputResolver = NullMetadataResolver.class;
  private Map<String, Class<? extends InputTypeResolver>> inputResolvers = new HashMap<>();
  private Class<? extends AttributesTypeResolver> attributesResolver = NullMetadataResolver.class;

  public MetadataScopeAdapter(Class<?> extensionType, Method operation, OperationDeclaration declaration) {
    OutputResolver outputResolverDeclaration = operation.getAnnotation(OutputResolver.class);
    Optional<MetadataKeyId> keyId = locateMetadataKeyId(declaration);

    inputResolvers = declaration.getParameters().stream()
        .filter(p -> getAnnotatedElement(p).map(e -> e.isAnnotationPresent(TypeResolver.class)).orElse(false))
        .collect(toMap(NamedDeclaration::getName,
                       p -> getAnnotatedElement(p).get().getAnnotation(TypeResolver.class).value()));

    if (outputResolverDeclaration != null || !inputResolvers.isEmpty()) {
      if (outputResolverDeclaration != null) {
        outputResolver = outputResolverDeclaration.output();
        attributesResolver = outputResolverDeclaration.attributes();
      }
      if (keyId.isPresent()) {
        keysResolver = keyId.get().value();
      }
    } else {
      initializeFromClass(extensionType, operation.getDeclaringClass());
    }
  }

  public MetadataScopeAdapter(Class<?> extensionType, Class<?> source) {
    initializeFromClass(extensionType, source);
  }

  private void initializeFromClass(Class<?> extensionType, Class<?> source) {
    MetadataScope scope = getAnnotation(source, MetadataScope.class);
    scope = scope != null ? scope : getAnnotation(extensionType, MetadataScope.class);

    if (scope != null) {
      this.keysResolver = scope.keysResolver();
      this.outputResolver = scope.outputResolver();
      this.attributesResolver = scope.attributesResolver();
    }
  }

  private Optional<MetadataKeyId> locateMetadataKeyId(ComponentDeclaration<? extends ComponentDeclaration> component) {
    Optional<MetadataKeyId> keyId = component.getParameters().stream()
        .map(IntrospectionUtils::getAnnotatedElement)
        .filter(p -> p.isPresent() && p.get().isAnnotationPresent(MetadataKeyId.class))
        .map(p -> p.get().getAnnotation(MetadataKeyId.class))
        .findFirst();

    if (!keyId.isPresent() && component.getModelProperty(ParameterGroupModelProperty.class).isPresent()) {
      keyId = component.getModelProperty(ParameterGroupModelProperty.class).get().getGroups().stream()
          .filter(g -> g.getContainer().isAnnotationPresent(MetadataKeyId.class))
          .map(g -> g.getContainer().getAnnotation(MetadataKeyId.class))
          .findFirst();
    }

    return keyId;
  }

  public boolean isCustomScope() {
    return hasOutputResolver() || hasInputResolvers();
  }

  public boolean hasInputResolvers() {
    return !inputResolvers.isEmpty();
  }

  public boolean hasOutputResolver() {
    return !outputResolver.equals(NullMetadataResolver.class);
  }

  public boolean hasAttributesResolver() {
    return !attributesResolver.equals(NullMetadataResolver.class);
  }

  public Class<? extends TypeKeysResolver> getKeysResolver() {
    return keysResolver;
  }

  public Map<String, Class<? extends InputTypeResolver>> getInputResolvers() {
    return inputResolvers;
  }

  public Class<? extends OutputTypeResolver> getOutputResolver() {
    return outputResolver;
  }

  public Class<? extends AttributesTypeResolver> getAttributesResolver() {
    return attributesResolver;
  }

}
