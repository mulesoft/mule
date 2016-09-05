/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.extension.api.annotation.Query;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OutputDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.introspection.property.LayoutModelPropertyBuilder;
import org.mule.runtime.extension.api.introspection.property.MetadataContentModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.introspection.property.QueryOperationModelProperty;
import org.mule.runtime.module.extension.internal.model.property.QueryParameterModelProperty;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.ParameterWrapper;
import org.mule.runtime.module.extension.internal.metadata.MetadataScopeAdapter;
import org.mule.runtime.module.extension.internal.metadata.QueryMetadataResolverFactory;
import org.mule.runtime.module.extension.internal.model.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * {@link ModelEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for components (Operations and
 * Message sources) annotated with {@link MetadataScope} or {@link Query}. If a custom metadata scope is used,
 * the component will be considered of dynamic type.
 *
 * @since 4.0
 */
public class DynamicMetadataModelEnricher extends AbstractAnnotatedModelEnricher {

  private Class<?> extensionType;
  private ClassTypeLoader typeLoader;

  @Override
  public void enrich(DescribingContext describingContext) {
    extensionType = extractExtensionType(describingContext.getExtensionDeclarer().getDeclaration());
    if (extensionType != null) {
      typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(currentThread().getContextClassLoader());

      new IdempotentDeclarationWalker() {

        @Override
        public void onSource(SourceDeclaration declaration) {
          enrichSourceMetadata(declaration);
        }

        @Override
        public void onOperation(OperationDeclaration declaration) {
          enrichOperationMetadata(declaration);
        }

        @Override
        public void onParameter(ParameterDeclaration declaration) {
          enrichParameter(declaration);
        }
      }.walk(describingContext.getExtensionDeclarer().getDeclaration());
    }
  }

  private void enrichParameter(ParameterDeclaration declaration) {
    Optional<AnnotatedElement> annotatedElement = getAnnotatedElement(declaration);

    if (annotatedElement.isPresent()) {
      parseMetadataAnnotations(annotatedElement.get(), declaration);
    }
  }

  private void enrichSourceMetadata(SourceDeclaration sourceDeclaration) {
    final Optional<ImplementingTypeModelProperty> modelProperty =
        sourceDeclaration.getModelProperty(ImplementingTypeModelProperty.class);
    if (modelProperty.isPresent()) {
      final Class<?> operationMethod = modelProperty.get().getType();
      MetadataScopeAdapter metadataScope = new MetadataScopeAdapter(getMetadataScope(operationMethod));
      MetadataResolverFactory metadataResolverFactory = getMetadataResolverFactory(metadataScope);
      sourceDeclaration.setMetadataResolverFactory(metadataResolverFactory);

      if (metadataScope.isCustomScope()) {
        declareOutput(sourceDeclaration.getOutput(), sourceDeclaration.getOutputAttributes(), metadataScope);
        declareComponentMetadataKeyId(sourceDeclaration);
      }
    }
  }

  private void enrichOperationMetadata(OperationDeclaration declaration) {
    declaration.getModelProperty(ImplementingMethodModelProperty.class)
        .ifPresent(prop -> {
          final Method method = prop.getMethod();

          if (method.isAnnotationPresent(Query.class)) {
            Query query = method.getAnnotation(Query.class);
            declaration.setMetadataResolverFactory(new QueryMetadataResolverFactory(query.nativeOutputResolver(),
                                                                                    query.entityResolver()));
            addQueryModelProperties(declaration, query);
            declareOutputType(declaration.getOutput());
            declareComponentMetadataKeyId(declaration);
          } else {
            MetadataScopeAdapter metadataScope = new MetadataScopeAdapter(getMetadataScope(method));
            MetadataResolverFactory metadataResolverFactory = getMetadataResolverFactory(metadataScope);
            declaration.setMetadataResolverFactory(metadataResolverFactory);

            if (metadataScope.isCustomScope()) {
              declareOutput(declaration.getOutput(), declaration.getOutputAttributes(), metadataScope);
              declareContent(declaration, method);
              declareComponentMetadataKeyId(declaration);
            }
          }
        });
  }


  private void addQueryModelProperties(OperationDeclaration declaration, Query query) {
    ParameterDeclaration parameterDeclaration = declaration.getParameters()
        .stream()
        .filter(p -> p.getModelProperty(ImplementingParameterModelProperty.class).isPresent())
        .filter(p -> p.getModelProperty(ImplementingParameterModelProperty.class).get()
            .getParameter().isAnnotationPresent(MetadataKeyId.class))
        .findFirst()
        .orElseThrow(
                     () -> new IllegalStateException("Query operation must have a parameter annotated with @MetadataKeyId"));

    parameterDeclaration.addModelProperty(new QueryParameterModelProperty(query.translator()));
    parameterDeclaration.addModelProperty(LayoutModelPropertyBuilder.create().withText(true).build());
    declaration.addModelProperty(new QueryOperationModelProperty());
  }

  private void declareContent(OperationDeclaration operationDeclaration, Method operationMethod) {
    final Parameter[] parameters = operationMethod.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].getAnnotation(Content.class) != null) {
        final ParameterWrapper parameterWrapper = new ParameterWrapper(operationMethod, i);
        operationDeclaration.getParameters().stream()
            .filter(parameterDeclaration -> parameterDeclaration.getName().equals(parameterWrapper.getAlias()))
            .forEach(parameterDeclaration -> parameterDeclaration.setType(parameterDeclaration.getType(), true));
      }
    }
  }

  private MetadataScope getMetadataScope(Method method) {
    MetadataScope scope = method.getAnnotation(MetadataScope.class);
    return scope != null ? scope : getMetadataScope(method.getDeclaringClass());
  }

  private MetadataScope getMetadataScope(Class<?> source) {
    MetadataScope scope = getAnnotation(source, MetadataScope.class);
    return scope != null ? scope : getAnnotation(extensionType, MetadataScope.class);
  }

  private MetadataResolverFactory getMetadataResolverFactory(MetadataScopeAdapter scope) {
    return scope.isCustomScope() ? new DefaultMetadataResolverFactory(scope.getKeysResolver(), scope.getContentResolver(),
                                                                      scope.getOutputResolver(), scope.getAttributesResolver())
        : new NullMetadataResolverFactory();

  }

  private void declareOutput(OutputDeclaration outputDeclaration,
                             OutputDeclaration attributesDeclaration,
                             MetadataScopeAdapter metadataScope) {
    if (metadataScope.hasOutputResolver()) {
      declareOutputType(outputDeclaration);
    }

    if (metadataScope.hasAttributesResolver()) {
      declareOutputType(attributesDeclaration);
    }
  }

  private void declareOutputType(OutputDeclaration component) {
    component.setType(component.getType(), true);
  }

  private void declareComponentMetadataKeyId(ComponentDeclaration<? extends ComponentDeclaration> component) {

    Optional<MetadataType> keyId = component.getParameters().stream()
        .filter(p -> getAnnotatedElement(p).map(element -> element.isAnnotationPresent(MetadataKeyId.class)).orElse(false))
        .map(ParameterDeclaration::getType)
        .findFirst();

    if (!keyId.isPresent() && component.getModelProperty(ParameterGroupModelProperty.class).isPresent()) {
      keyId = component.getModelProperty(ParameterGroupModelProperty.class).get().getGroups().stream()
          .filter(g -> g.getContainer().isAnnotationPresent(MetadataKeyId.class))
          .map(g -> typeLoader.load(g.getType()))
          .findFirst();
    }

    if (keyId.isPresent()) {
      component.addModelProperty(new MetadataKeyIdModelProperty(keyId.get()));
    }
  }

  /**
   * Enriches the {@link ParameterDeclarer} with a {@link MetadataKeyPartModelProperty} or a {@link MetadataContentModelProperty}
   * if the parsedParameter is annotated either as {@link MetadataKeyId}, {@link MetadataKeyPart} or {@link Content} respectively.
   *
   * @param element         the method annotated parameter parsed
   * @param baseDeclaration the {@link ParameterDeclarer} associated to the parsed parameter
   */
  private void parseMetadataAnnotations(AnnotatedElement element, BaseDeclaration baseDeclaration) {
    if (element.isAnnotationPresent(Content.class)) {
      baseDeclaration.addModelProperty(new MetadataContentModelProperty());
    }

    if (element.isAnnotationPresent(MetadataKeyId.class)) {
      baseDeclaration.addModelProperty(new MetadataKeyPartModelProperty(1));
    }

    if (element.isAnnotationPresent(MetadataKeyPart.class)) {
      MetadataKeyPart metadataKeyPart = element.getAnnotation(MetadataKeyPart.class);
      baseDeclaration.addModelProperty(new MetadataKeyPartModelProperty(metadataKeyPart.order()));
    }
  }

  private Optional<AnnotatedElement> getAnnotatedElement(BaseDeclaration<?> declaration) {
    final Optional<DeclaringMemberModelProperty> declaringMember =
        declaration.getModelProperty(DeclaringMemberModelProperty.class);
    final Optional<ImplementingParameterModelProperty> implementingParameter =
        declaration.getModelProperty(ImplementingParameterModelProperty.class);

    AnnotatedElement annotatedElement = null;
    if (declaringMember.isPresent()) {
      annotatedElement = declaringMember.get().getDeclaringField();
    }

    if (implementingParameter.isPresent()) {
      annotatedElement = implementingParameter.get().getParameter();
    }

    return Optional.ofNullable(annotatedElement);
  }
}
