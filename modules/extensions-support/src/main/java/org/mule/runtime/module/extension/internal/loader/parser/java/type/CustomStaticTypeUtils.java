/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.type;


import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.metadata.xml.api.SchemaCollector.getInstance;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;

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
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.annotations.CustomDefinedStaticTypeAnnotation;
import org.mule.runtime.module.extension.internal.loader.java.enricher.MetadataTypeEnricher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Util class that resolves the static types of parameter, outputs and attributes from sources and operartions in Java based
 * extensions.
 *
 * @since 4.5
 */
public class CustomStaticTypeUtils {

  private static final MetadataTypeEnricher enricher = new MetadataTypeEnricher();

  /**
   * resolves the output type of a given operation.
   *
   * @param operationElement operation to resolve the output
   * @return the output type of the operation
   */
  public static MetadataType getOperationOutputType(OperationElement operationElement) {
    MetadataType declarationType = operationElement.getOperationReturnMetadataType();
    return getOutputType(operationElement, declarationType, "operation", operationElement.getName())
        .map(customType -> enrichCustomType(declarationType, customType)).orElse(declarationType);
  }

  /**
   * resolves the attributes type of a given operation.
   *
   * @param operationElement operation to resolve the attributes
   * @return the attributes type of the operation
   */
  public static MetadataType getOperationAttributesType(OperationElement operationElement) {
    MetadataType declarationType = operationElement.getOperationAttributesMetadataType();
    return getAttributesType(operationElement, declarationType, "operation", operationElement.getName())
        .map(customType -> enrichCustomType(declarationType, customType)).orElse(declarationType);
  }

  /**
   * resolves type of a given parameter.
   *
   * @param parameterElement parameter to resolve the type
   * @return the type of the parameter
   */
  public static MetadataType getParameterType(ExtensionParameter parameterElement) {
    MetadataType declarationType = parameterElement.getType().asMetadataType();
    return getInputType(parameterElement, declarationType, "parameter", parameterElement.getName())
        .map(customType -> enrichCustomType(declarationType, customType)).orElse(declarationType);
  }

  /**
   * resolves the output type of a given source.
   *
   * @param sourceElement source to resolve the output
   * @return the output type of the source
   */
  public static MetadataType getSourceOutputType(SourceElement sourceElement) {
    MetadataType declarationType = sourceElement.getReturnMetadataType();
    return getOutputType(sourceElement, declarationType, "source", sourceElement.getName())
        .map(customType -> enrichCustomType(declarationType, customType)).orElse(declarationType);
  }

  /**
   * resolves the attribute type of a given source.
   *
   * @param sourceElement source to resolve the attributes
   * @return the attributes type of the source
   */
  public static MetadataType getSourceAttributesType(SourceElement sourceElement) {
    MetadataType declarationType = sourceElement.getAttributesMetadataType();
    return getAttributesType(sourceElement, declarationType, "source", sourceElement.getName())
        .map(customType -> enrichCustomType(declarationType, customType)).orElse(declarationType);
  }

  private static Optional<MetadataType> getAttributesType(WithAnnotations element, MetadataType outputType, String elementType,
                                                          String elementName) {


    Optional<MetadataType> result =
        mapReduceSingleAnnotation(element, elementType, elementName, AttributesXmlType.class,
                                  org.mule.sdk.api.annotation.metadata.fixed.AttributesXmlType.class,
                                  outputXmlTypeAnnotationValueFetcher -> getXmlType(outputXmlTypeAnnotationValueFetcher
                                      .getStringValue(AttributesXmlType::schema),
                                                                                    outputXmlTypeAnnotationValueFetcher
                                                                                        .getStringValue(AttributesXmlType::qname))
                                      .map(type -> resolveType(type,
                                                               outputType))
                                      .orElse(null),
                                  outputXmlTypeAnnotationValueFetcher -> getXmlType(outputXmlTypeAnnotationValueFetcher
                                      .getStringValue(org.mule.sdk.api.annotation.metadata.fixed.AttributesXmlType::schema),
                                                                                    outputXmlTypeAnnotationValueFetcher
                                                                                        .getStringValue(org.mule.sdk.api.annotation.metadata.fixed.AttributesXmlType::qname))
                                      .map(type -> resolveType(type,
                                                               outputType))
                                      .orElse(null));

    if (result.isPresent()) {
      return result;
    }

    result = mapReduceSingleAnnotation(element, elementType, elementName, AttributesJsonType.class,
                                       org.mule.sdk.api.annotation.metadata.fixed.AttributesJsonType.class,
                                       outputJsonTypeAnnotationValueFetcher -> getJsonType(outputJsonTypeAnnotationValueFetcher
                                           .getStringValue(AttributesJsonType::schema))
                                           .map(type -> resolveType(type, outputType)).orElse(null),
                                       outputJsonTypeAnnotationValueFetcher -> getJsonType(outputJsonTypeAnnotationValueFetcher
                                           .getStringValue(org.mule.sdk.api.annotation.metadata.fixed.AttributesJsonType::schema))
                                           .map(type -> resolveType(type, outputType)).orElse(null));

    if (result.isPresent()) {
      return result;
    }

    result = mapReduceSingleAnnotation(element, elementType, elementName, OutputResolver.class,
                                       org.mule.sdk.api.annotation.metadata.OutputResolver.class,
                                       outputTypeAnnotationValueFetcher -> {
                                         Type type = outputTypeAnnotationValueFetcher.getClassValue(OutputResolver::attributes);
                                         Class declaringClass = type.getDeclaringClass().orElse(null);
                                         if (declaringClass != null && isStaticResolver(declaringClass)) {
                                           return getCustomStaticType(declaringClass).orElse(null);
                                         } else {
                                           return null;
                                         }
                                       }, outputTypeAnnotationValueFetcher -> {
                                         Type type = outputTypeAnnotationValueFetcher
                                             .getClassValue(org.mule.sdk.api.annotation.metadata.OutputResolver::attributes);
                                         Class declaringClass = type.getDeclaringClass().orElse(null);
                                         if (declaringClass != null && isStaticResolver(declaringClass)) {
                                           return getCustomStaticType(declaringClass).orElse(null);
                                         } else {
                                           return null;
                                         }
                                       });

    if (result.isPresent()) {
      return result;
    }

    result = mapReduceSingleAnnotation(element, elementType, elementName, MetadataScope.class,
                                       org.mule.sdk.api.annotation.metadata.MetadataScope.class,
                                       outputTypeAnnotationValueFetcher -> {
                                         Type type =
                                             outputTypeAnnotationValueFetcher.getClassValue(MetadataScope::attributesResolver);
                                         Class declaringClass = type.getDeclaringClass().orElse(null);
                                         if (declaringClass != null && isStaticResolver(declaringClass)) {
                                           return getCustomStaticType(declaringClass).orElse(null);
                                         } else {
                                           return null;
                                         }
                                       }, outputTypeAnnotationValueFetcher -> {
                                         Type type = outputTypeAnnotationValueFetcher
                                             .getClassValue(org.mule.sdk.api.annotation.metadata.MetadataScope::attributesResolver);
                                         Class declaringClass = type.getDeclaringClass().orElse(null);
                                         if (declaringClass != null && isStaticResolver(declaringClass)) {
                                           return getCustomStaticType(declaringClass).orElse(null);
                                         } else {
                                           return null;
                                         }
                                       });

    if (result.isPresent()) {
      return result;
    }

    return empty();
  }

  private static Optional<MetadataType> getOutputType(WithAnnotations element, MetadataType outputType, String elementType,
                                                      String elementName) {

    Optional<MetadataType> result =
        mapReduceSingleAnnotation(element, elementType, elementName, OutputXmlType.class,
                                  org.mule.sdk.api.annotation.metadata.fixed.OutputXmlType.class,
                                  outputXmlTypeAnnotationValueFetcher -> getXmlType(outputXmlTypeAnnotationValueFetcher
                                      .getStringValue(OutputXmlType::schema),
                                                                                    outputXmlTypeAnnotationValueFetcher
                                                                                        .getStringValue(OutputXmlType::qname))
                                      .map(type -> resolveType(type,
                                                               outputType))
                                      .orElse(null),
                                  outputXmlTypeAnnotationValueFetcher -> getXmlType(outputXmlTypeAnnotationValueFetcher
                                      .getStringValue(org.mule.sdk.api.annotation.metadata.fixed.OutputXmlType::schema),
                                                                                    outputXmlTypeAnnotationValueFetcher
                                                                                        .getStringValue(org.mule.sdk.api.annotation.metadata.fixed.OutputXmlType::qname))
                                      .map(type -> resolveType(type,
                                                               outputType))
                                      .orElse(null));

    if (result.isPresent()) {
      return result;
    }

    result = mapReduceSingleAnnotation(element, elementType, elementName, OutputJsonType.class,
                                       org.mule.sdk.api.annotation.metadata.fixed.OutputJsonType.class,
                                       outputJsonTypeAnnotationValueFetcher -> getJsonType(outputJsonTypeAnnotationValueFetcher
                                           .getStringValue(OutputJsonType::schema))
                                           .map(type -> resolveType(type, outputType)).orElse(null),
                                       outputJsonTypeAnnotationValueFetcher -> getJsonType(outputJsonTypeAnnotationValueFetcher
                                           .getStringValue(org.mule.sdk.api.annotation.metadata.fixed.OutputJsonType::schema))
                                           .map(type -> resolveType(type, outputType)).orElse(null));

    if (result.isPresent()) {
      return result;
    }

    result = mapReduceSingleAnnotation(element, elementType, elementName, OutputResolver.class,
                                       org.mule.sdk.api.annotation.metadata.OutputResolver.class,
                                       outputTypeAnnotationValueFetcher -> {
                                         Type type = outputTypeAnnotationValueFetcher.getClassValue(OutputResolver::output);
                                         Class declaringClass = type.getDeclaringClass().orElse(null);
                                         if (declaringClass != null && isStaticResolver(declaringClass)) {
                                           return getCustomStaticType(declaringClass).orElse(null);
                                         } else {
                                           return null;
                                         }
                                       }, outputTypeAnnotationValueFetcher -> {
                                         Type type = outputTypeAnnotationValueFetcher
                                             .getClassValue(org.mule.sdk.api.annotation.metadata.OutputResolver::output);
                                         Class declaringClass = type.getDeclaringClass().orElse(null);
                                         if (declaringClass != null && isStaticResolver(declaringClass)) {
                                           return getCustomStaticType(declaringClass).orElse(null);
                                         } else {
                                           return null;
                                         }
                                       });

    if (result.isPresent()) {
      return result;
    }

    result = mapReduceSingleAnnotation(element, elementType, elementName, MetadataScope.class,
                                       org.mule.sdk.api.annotation.metadata.MetadataScope.class,
                                       outputTypeAnnotationValueFetcher -> {
                                         Type type =
                                             outputTypeAnnotationValueFetcher.getClassValue(MetadataScope::outputResolver);
                                         Class declaringClass = type.getDeclaringClass().orElse(null);
                                         if (declaringClass != null && isStaticResolver(declaringClass)) {
                                           return getCustomStaticType(declaringClass).orElse(null);
                                         } else {
                                           return null;
                                         }
                                       }, outputTypeAnnotationValueFetcher -> {
                                         Type type = outputTypeAnnotationValueFetcher
                                             .getClassValue(org.mule.sdk.api.annotation.metadata.MetadataScope::outputResolver);
                                         Class declaringClass = type.getDeclaringClass().orElse(null);
                                         if (declaringClass != null && isStaticResolver(declaringClass)) {
                                           return getCustomStaticType(declaringClass).orElse(null);
                                         } else {
                                           return null;
                                         }
                                       });

    if (result.isPresent()) {
      return result;
    }

    return empty();
  }

  private static Optional<MetadataType> getInputType(WithAnnotations element, MetadataType outputType, String elementType,
                                                     String elementName) {

    Optional<MetadataType> result =
        mapReduceSingleAnnotation(element, elementType, elementName, InputXmlType.class,
                                  org.mule.sdk.api.annotation.metadata.fixed.InputXmlType.class,
                                  outputXmlTypeAnnotationValueFetcher -> getXmlType(outputXmlTypeAnnotationValueFetcher
                                      .getStringValue(InputXmlType::schema),
                                                                                    outputXmlTypeAnnotationValueFetcher
                                                                                        .getStringValue(InputXmlType::qname))
                                      .map(type -> resolveType(type,
                                                               outputType))
                                      .orElse(null),
                                  outputXmlTypeAnnotationValueFetcher -> getXmlType(outputXmlTypeAnnotationValueFetcher
                                      .getStringValue(org.mule.sdk.api.annotation.metadata.fixed.InputXmlType::schema),
                                                                                    outputXmlTypeAnnotationValueFetcher
                                                                                        .getStringValue(org.mule.sdk.api.annotation.metadata.fixed.InputXmlType::qname))
                                      .map(type -> resolveType(type,
                                                               outputType))
                                      .orElse(null));

    if (result.isPresent()) {
      return result;
    }

    result = mapReduceSingleAnnotation(element, elementType, elementName, InputJsonType.class,
                                       org.mule.sdk.api.annotation.metadata.fixed.InputJsonType.class,
                                       outputJsonTypeAnnotationValueFetcher -> getJsonType(outputJsonTypeAnnotationValueFetcher
                                           .getStringValue(InputJsonType::schema))
                                           .map(type -> resolveType(type, outputType)).orElse(null),
                                       outputJsonTypeAnnotationValueFetcher -> getJsonType(outputJsonTypeAnnotationValueFetcher
                                           .getStringValue(org.mule.sdk.api.annotation.metadata.fixed.InputJsonType::schema))
                                           .map(type -> resolveType(type, outputType)).orElse(null));

    if (result.isPresent()) {
      return result;
    }

    result = mapReduceSingleAnnotation(element, elementType, elementName, TypeResolver.class,
                                       org.mule.sdk.api.annotation.metadata.TypeResolver.class,
                                       outputTypeAnnotationValueFetcher -> {
                                         Type type = outputTypeAnnotationValueFetcher.getClassValue(TypeResolver::value);
                                         Class declaringClass = type.getDeclaringClass().orElse(null);
                                         if (declaringClass != null && isStaticResolver(declaringClass)) {
                                           return getCustomStaticType(declaringClass).orElse(null);
                                         } else {
                                           return null;
                                         }
                                       }, outputTypeAnnotationValueFetcher -> {
                                         Type type = outputTypeAnnotationValueFetcher
                                             .getClassValue(org.mule.sdk.api.annotation.metadata.TypeResolver::value);
                                         Class declaringClass = type.getDeclaringClass().orElse(null);
                                         if (declaringClass != null && isStaticResolver(declaringClass)) {
                                           return getCustomStaticType(declaringClass).orElse(null);
                                         } else {
                                           return null;
                                         }
                                       });

    if (result.isPresent()) {
      return result;
    }

    return empty();
  }

  private static MetadataType enrichCustomType(MetadataType declarationType, MetadataType target) {
    Class<?> clazz = getType(declarationType)
        .orElseThrow(() -> new IllegalStateException("Could not find class in type [" + declarationType + "]"));
    Set<TypeAnnotation> a = new HashSet<>(asList(new ClassInformationAnnotation(clazz), new CustomDefinedStaticTypeAnnotation()));
    return enricher.enrich(target, a);
  }

  private static MetadataType resolveType(MetadataType annotationType, MetadataType declarationType) {
    if (declarationType instanceof ArrayType) {
      ArrayTypeBuilder arrayMetadataBuilder = BaseTypeBuilder.create(MetadataFormat.JAVA).arrayType();
      arrayMetadataBuilder.of(annotationType);
      return arrayMetadataBuilder.build();
    }
    return annotationType;
  }

  private static Optional<MetadataType> getCustomStaticType(Class<?> resolver) {
    try {
      Object resolverInstance = resolver.newInstance();
      if (resolverInstance instanceof StaticResolver) {
        return of(((StaticResolver) resolverInstance).getStaticMetadata());
      } else {
        return of(((org.mule.sdk.api.metadata.resolving.StaticResolver) resolver.newInstance()).getStaticMetadata());
      }
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalArgumentException("Can't obtain static type for element", e);
    }
  }

  private static Optional<MetadataType> getJsonType(String schema) {
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

  private static Optional<MetadataType> getXmlType(String schema, String qname) {
    if (isNotBlank(schema)) {
      if (isBlank(qname)) {
        throw new IllegalStateException("[" + schema + "] was specified but no associated QName to find in schema");
      }
      Optional<MetadataType> type;
      try (InputStream is = getSchemaContent(schema)) {
        URL schemaURL = currentThread().getContextClassLoader().getResource(schema);
        type = new XmlTypeLoader(getInstance().addSchema(schemaURL.toString(), is)).load(qname);
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

  private static InputStream getSchemaContent(String schemaName) {
    InputStream schema = currentThread().getContextClassLoader().getResourceAsStream(schemaName);
    if (schema == null) {
      throw new IllegalArgumentException("Can't load schema [" + schemaName + "]. It was not found in the resources.");
    }
    return schema;
  }

  private static boolean isStaticResolver(Class<?> resolverClazz) {
    return StaticResolver.class.isAssignableFrom(resolverClazz)
        || org.mule.sdk.api.metadata.resolving.StaticResolver.class.isAssignableFrom(resolverClazz);
  }

}
