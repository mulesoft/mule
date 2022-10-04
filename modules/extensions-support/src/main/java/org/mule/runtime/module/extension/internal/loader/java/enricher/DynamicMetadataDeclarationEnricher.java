/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.enricher;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.meta.model.display.LayoutModel.builderFrom;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataKeyIdModelParserUtils.getMetadataKeyPart;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataKeyIdModelParserUtils.hasKeyId;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isASTMode;

import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.TypedDeclaration;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Query;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.QueryParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataKeyIdModelParserUtils;
import org.mule.runtime.module.extension.internal.metadata.QueryMetadataResolverFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * {@link DeclarationEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for components
 * (Operations and Message sources) annotated with {@link MetadataScope} or {@link Query}. If a custom metadata scope is used, the
 * component will be considered of dynamic type.
 *
 * @since 4.0
 */
public class DynamicMetadataDeclarationEnricher implements DeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new EnricherDelegate().enrich(extensionLoadingContext);
  }

  private static class EnricherDelegate extends AbstractAnnotatedDeclarationEnricher {

    private static final NullMetadataResolver nullMetadataResolver = new NullMetadataResolver();

    @Override
    public void enrich(ExtensionLoadingContext extensionLoadingContext) {
      BaseDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
      // TODO MULE-14397 - Improve Dynamic Metadata Enricher to enrich without requiring Classes
      if (!isASTMode(declaration)) {
        Optional<ExtensionTypeDescriptorModelProperty> property =
            declaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class);

        if (!property.isPresent()) {
          return;
        }

        new IdempotentDeclarationWalker() {

          @Override
          public void onOperation(OperationDeclaration declaration) {
            enrichOperationMetadata(declaration);
          }

        }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
      }
    }

    private void enrichOperationMetadata(OperationDeclaration declaration) {
      declaration.getModelProperty(ExtensionOperationDescriptorModelProperty.class)
          .map(ExtensionOperationDescriptorModelProperty::getOperationElement)
          .ifPresent(operation -> {
            if (operation.isAnnotatedWith(Query.class)) {
              enrichWithDsql(declaration, operation);
            }
          });
    }

    private void enrichMetadataKeyParameters(ParameterizedDeclaration<?> declaration) {
      declaration.getAllParameters()
          .forEach(paramDeclaration -> paramDeclaration.getModelProperty(ExtensionParameterDescriptorModelProperty.class)
              .ifPresent(modelProperty -> parseMetadataKeyAnnotations(modelProperty.getExtensionParameter(),
                                                                      paramDeclaration)));
    }

    private void declareResolversInformation(ExecutableComponentDeclaration<? extends ComponentDeclaration> declaration,
                                             OutputTypeResolver outputResolver) {
      declareResolversInformation(declaration, outputResolver, declaration.isRequiresConnection());
    }

    private void declareResolversInformation(BaseDeclaration declaration, OutputTypeResolver outputResolver,
                                             boolean requiresConnection) {
      if (!(outputResolver instanceof NullMetadataResolver)) {

        String categoryName = outputResolver.getCategoryName();
        String outputResolverName = outputResolver.getResolverName();
        String nullResolverName = nullMetadataResolver.getResolverName();

        // TODO MULE-15638 - Once Metadata API 2.0 is implemented we will know better if the resolver requires or not a connection
        // of config.
        declaration.addModelProperty(new TypeResolversInformationModelProperty(categoryName,
                                                                               emptyMap(),
                                                                               outputResolverName,
                                                                               nullResolverName,
                                                                               nullResolverName,
                                                                               requiresConnection,
                                                                               requiresConnection,
                                                                               false));
      }
    }

    private void enrichWithDsql(OperationDeclaration declaration,
                                MethodElement method) {
      Query query = method.getAnnotation(Query.class).get();
      final MetadataResolverFactory resolverFactory = new QueryMetadataResolverFactory(
                                                                                       query.nativeOutputResolver(),
                                                                                       query.entityResolver());
      declaration.addModelProperty(new MetadataResolverFactoryModelProperty(() -> resolverFactory));

      OutputTypeResolver<?> outputResolver = resolverFactory.getOutputResolver();

      addQueryModelProperties(declaration, query);
      declareDynamicType(declaration.getOutput());
      declareMetadataKeyId(declaration, null);
      enrichMetadataKeyParameters(declaration);
      declareResolversInformation(declaration, outputResolver);
    }

    private void addQueryModelProperties(OperationDeclaration declaration, Query query) {
      ParameterDeclaration parameterDeclaration = declaration.getAllParameters()
          .stream()
          .filter(p -> p.getModelProperty(ImplementingParameterModelProperty.class).isPresent())
          .filter(p -> p.getModelProperty(ImplementingParameterModelProperty.class).get()
              .getParameter().isAnnotationPresent(MetadataKeyId.class))
          .findFirst()
          .orElseThrow(() -> new IllegalParameterModelDefinitionException(
                                                                          "Query operation must have a parameter annotated with @MetadataKeyId"));

      parameterDeclaration.addModelProperty(new QueryParameterModelProperty(query.translator()));
      parameterDeclaration.setLayoutModel(builderFrom(parameterDeclaration.getLayoutModel()).asQuery().build());
    }

    private void declareDynamicType(TypedDeclaration component) {
      component.setType(component.getType(), true);
    }

    private void declareMetadataKeyId(ComponentDeclaration<? extends ComponentDeclaration> component,
                                      String categoryName) {
      getMetadataKeyModelProperty(component, categoryName).ifPresent(component::addModelProperty);
    }

    private Optional<MetadataKeyIdModelProperty> getMetadataKeyModelProperty(ComponentDeclaration<? extends ComponentDeclaration> component,
                                                                             String categoryName) {
      Optional<MetadataKeyIdModelProperty> keyId = findMetadataKeyIdInGroups(component, categoryName);
      return keyId.isPresent() ? keyId : findMetadataKeyIdInParameters(component, categoryName);
    }

    private Optional<MetadataKeyIdModelProperty> findMetadataKeyIdInGroups(
                                                                           ComponentDeclaration<? extends ComponentDeclaration> component,
                                                                           String categoryName) {
      return component.getParameterGroups().stream()
          .map(group -> group.getModelProperty(ParameterGroupModelProperty.class).orElse(null))
          .filter(Objects::nonNull)
          .filter(group -> hasKeyId(group.getDescriptor().getAnnotatedContainer()))
          .map(group -> new MetadataKeyIdModelProperty(group.getDescriptor().getMetadataType(),
                                                       group.getDescriptor().getName(), categoryName))
          .findFirst();
    }

    private Optional<MetadataKeyIdModelProperty> findMetadataKeyIdInParameters(
                                                                               ComponentDeclaration<? extends ComponentDeclaration> component,
                                                                               String categoryName) {
      return component.getParameterGroups().stream()
          .flatMap(g -> g.getParameters().stream())
          .filter(p -> getExtensionParameter(p).map(JavaMetadataKeyIdModelParserUtils::hasKeyId).orElse(false))
          .map(p -> new MetadataKeyIdModelProperty(p.getType(), p.getName(), categoryName))
          .findFirst();
    }

    private Optional<ExtensionParameter> getExtensionParameter(ParameterDeclaration parameterDeclaration) {
      return parameterDeclaration
          .getModelProperty(ExtensionParameterDescriptorModelProperty.class)
          .map(ExtensionParameterDescriptorModelProperty::getExtensionParameter);
    }

    /**
     * Enriches the {@link ParameterDeclarer} with a {@link MetadataKeyPartModelProperty} if the parsedParameter is annotated
     * either as {@link MetadataKeyId} or {@link MetadataKeyPart}
     *
     * @param element         the method annotated parameter parsed
     * @param baseDeclaration the {@link ParameterDeclarer} associated to the parsed parameter
     */
    private void parseMetadataKeyAnnotations(ExtensionParameter element, BaseDeclaration baseDeclaration) {
      if (hasKeyId(element)) {
        baseDeclaration.addModelProperty(new MetadataKeyPartModelProperty(1, false));
      }

      getMetadataKeyPart(element).ifPresent(metadataKeyPart -> baseDeclaration
          .addModelProperty(new MetadataKeyPartModelProperty(metadataKeyPart.getFirst(), metadataKeyPart.getSecond())));
    }

  }

}
