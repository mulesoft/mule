/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.metadata.xml.api.SchemaCollector.getInstance;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.INITIALIZE;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;

import org.mule.metadata.api.annotation.TypeAnnotation;
import org.mule.metadata.api.builder.ArrayTypeBuilder;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.metadata.json.api.JsonTypeLoader;
import org.mule.metadata.xml.api.XmlTypeLoader;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.TypedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithParametersDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.api.metadata.resolving.InputStaticTypeResolver;
import org.mule.runtime.api.metadata.resolving.StaticResolver;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.metadata.fixed.AttributesJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.AttributesXmlType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.InputJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.InputXmlType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputXmlType;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.annotations.CustomDefinedStaticTypeAnnotation;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Overrides the default output and input loaded types for the ones specified by the user using the custom static types features,
 * such as {@link InputXmlType}, {@link OutputJsonType} and {@link InputStaticTypeResolver} for custom build types between others.
 *
 * @since 4.1
 */
public final class CustomStaticTypeDeclarationEnricher implements DeclarationEnricher {

  private final MetadataTypeEnricher enricher = new MetadataTypeEnricher();

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return INITIALIZE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      protected void onOperation(WithOperationsDeclaration owner, OperationDeclaration operation) {
        OutputDeclaration output = operation.getOutput();
        OutputDeclaration attributes = operation.getOutputAttributes();
        operation.getModelProperty(ImplementingMethodModelProperty.class)
            .map(ImplementingMethodModelProperty::getMethod)
            .ifPresent(method -> {
              getOutputType(method, output.getType())
                  .ifPresent(type -> declareCustomType(output, type));
              getAttributesType(method, attributes.getType())
                  .ifPresent(type -> declareCustomType(attributes, type));
            });
        declareParametersCustomStaticTypes(operation);
      }

      @Override
      protected void onSource(WithSourcesDeclaration owner, SourceDeclaration source) {
        OutputDeclaration output = source.getOutput();
        OutputDeclaration attributes = source.getOutputAttributes();
        source.getModelProperty(ImplementingTypeModelProperty.class)
            .map(ImplementingTypeModelProperty::getType)
            .ifPresent(clazz -> {
              getOutputType(clazz, output.getType()).ifPresent(type -> declareCustomType(output, type));
              getAttributesType(clazz, attributes.getType())
                  .ifPresent(type -> declareCustomType(attributes, type));
            });

        source.getSuccessCallback().ifPresent(this::declareParametersCustomStaticTypes);
        source.getErrorCallback().ifPresent(this::declareParametersCustomStaticTypes);
      }

      private void declareParametersCustomStaticTypes(WithParametersDeclaration operation) {
        for (ParameterDeclaration param : operation.getAllParameters()) {
          param.getModelProperty(ImplementingParameterModelProperty.class)
              .map(ImplementingParameterModelProperty::getParameter)
              .ifPresent(annotated -> getInputType(annotated, param.getType()).ifPresent(type -> declareCustomType(param, type)));
        }
      }
    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private Optional<MetadataType> getAttributesType(AnnotatedElement element, MetadataType outputAttributesType) {
    AttributesXmlType xml = element.getAnnotation(AttributesXmlType.class);
    if (xml != null) {
      return getXmlType(xml.schema(), xml.qname()).map(type -> resolveType(type, outputAttributesType));
    }
    AttributesJsonType json = element.getAnnotation(AttributesJsonType.class);
    if (json != null) {
      return getJsonType(json.schema()).map(type -> resolveType(type, outputAttributesType));
    }
    OutputResolver resolver = element.getAnnotation(OutputResolver.class);
    if (resolver != null && isStaticResolver(resolver.attributes())) {
      return getCustomStaticType(resolver.attributes());
    }
    MetadataScope scope = element.getAnnotation(MetadataScope.class);
    if (scope != null && isStaticResolver(scope.attributesResolver())) {
      return getCustomStaticType(scope.attributesResolver());
    }
    return empty();
  }

  private Optional<MetadataType> getOutputType(AnnotatedElement element, MetadataType outputType) {
    OutputXmlType xml = element.getAnnotation(OutputXmlType.class);
    if (xml != null) {
      return getXmlType(xml.schema(), xml.qname()).map(type -> resolveType(type, outputType));
    }
    OutputJsonType json = element.getAnnotation(OutputJsonType.class);
    if (json != null) {
      return getJsonType(json.schema()).map(type -> resolveType(type, outputType));
    }
    OutputResolver resolver = element.getAnnotation(OutputResolver.class);
    if (resolver != null && isStaticResolver(resolver.output())) {
      return getCustomStaticType(resolver.output());
    }
    MetadataScope scope = element.getAnnotation(MetadataScope.class);
    if (scope != null && isStaticResolver(scope.outputResolver())) {
      return getCustomStaticType(scope.outputResolver());
    }
    return empty();
  }

  private Optional<MetadataType> getInputType(AnnotatedElement element, MetadataType parameterType) {
    InputXmlType xml = element.getAnnotation(InputXmlType.class);
    if (xml != null) {
      return getXmlType(xml.schema(), xml.qname()).map(type -> resolveType(type, parameterType));
    }
    InputJsonType json = element.getAnnotation(InputJsonType.class);
    if (json != null) {
      return getJsonType(json.schema()).map(type -> resolveType(type, parameterType));
    }
    TypeResolver resolver = element.getAnnotation(TypeResolver.class);
    if (resolver != null && isStaticResolver(resolver.value())) {
      return getCustomStaticType(resolver.value());
    }
    return empty();
  }

  private <T extends BaseDeclaration & TypedDeclaration> void declareCustomType(T declaration, MetadataType target) {
    MetadataType type = declaration.getType();
    Class<?> clazz = getType(type).orElseThrow(() -> new IllegalStateException("Could not find class in type [" + type + "]"));
    Set<TypeAnnotation> a = new HashSet<>(asList(new ClassInformationAnnotation(clazz), new CustomDefinedStaticTypeAnnotation()));
    declaration.setType(enricher.enrich(target, a), false);
  }

  private MetadataType resolveType(MetadataType annotationType, MetadataType declarationType) {
    if (declarationType instanceof ArrayType) {
      ArrayTypeBuilder arrayMetadataBuilder = BaseTypeBuilder.create(MetadataFormat.JAVA).arrayType();
      arrayMetadataBuilder.of(annotationType);
      return arrayMetadataBuilder.build();
    }
    return annotationType;
  }

  private Optional<MetadataType> getCustomStaticType(Class<?> resolver) {
    try {
      return of(((StaticResolver) resolver.newInstance()).getStaticMetadata());
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalArgumentException("Can't obtain static type for element", e);
    }
  }

  private Optional<MetadataType> getJsonType(String schema) {
    String schemaContent;
    try (InputStream is = getSchemaContent(schema)) {
      schemaContent = IOUtils.toString(is);
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
    Optional<MetadataType> type = new JsonTypeLoader(schemaContent).load(null);
    if (!type.isPresent()) {
      throw new IllegalArgumentException("Could not load type from Json schema [" + schema + "]");
    }
    return type;
  }

  private Optional<MetadataType> getXmlType(String schema, String qname) {
    if (isNotBlank(schema)) {
      if (isBlank(qname)) {
        throw new IllegalStateException("[" + schema + "] was specified but no associated QName to find in schema");
      }
      Optional<MetadataType> type;
      try (InputStream is = getSchemaContent(schema)) {
        type = new XmlTypeLoader(getInstance().addSchema(schema, is)).load(qname);
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }
      if (!type.isPresent()) {
        throw new IllegalArgumentException("Type [" + qname + "] wasn't found in XML schema [" + schema + "]");
      }
      return type;
    }
    return empty();
  }

  private InputStream getSchemaContent(String schemaName) {
    InputStream schema = currentThread().getContextClassLoader().getResourceAsStream(schemaName);
    if (schema == null) {
      throw new IllegalArgumentException("Can't load schema [" + schemaName + "]. It was not found in the resources.");
    }
    return schema;
  }

  private boolean isStaticResolver(Class<?> resolverClazz) {
    return StaticResolver.class.isAssignableFrom(resolverClazz);
  }
}
