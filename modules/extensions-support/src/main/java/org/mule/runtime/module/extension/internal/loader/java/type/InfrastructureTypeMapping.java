/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableMap;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_CONFIG_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.SCHEDULING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_TYPE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECT_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECT_FOREVER_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_BYTE_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.NON_REPEATABLE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.RECONNECT_FOREVER_TYPE_KEY;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.RECONNECT_SIMPLE_TYPE_KEY;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.REDELIVERY_POLICY_TYPE_KEY;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.STREAMING_STRATEGY_NON_REPEATABLE_ITERABLE;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.STREAMING_STRATEGY_NON_REPEATABLE_STREAM;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.STREAMING_STRATEGY_REPEATABLE_FILE_ITERABLE;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.STREAMING_STRATEGY_REPEATABLE_FILE_STREAM;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.STREAMING_STRATEGY_REPEATABLE_IN_MEMORY_ITERABLE;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.STREAMING_STRATEGY_REPEATABLE_IN_MEMORY_STREAM;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureParameterBuilder.getInfrastructureParameterType;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CONTEXT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CRL_FILE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CUSTOM_OCSP_RESPONDER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_STANDARD_REVOCATION_CHECK_ELEMENT_IDENTIFIER;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.scheduler.SchedulingStrategy;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.property.QNameModelProperty;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.extension.api.tx.SourceTransactionalAction;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.xml.namespace.QName;

import com.google.common.collect.ImmutableMap;

/**
 * Mapping for types considered of "Infrastructure", of the {@link Class} of the infrastructure type and the {@link String} name
 * of it.
 *
 * @since 4.0
 */
public final class InfrastructureTypeMapping {

  public static final String TLS_NAMESPACE_URI = "http://www.mulesoft.org/schema/mule/tls";

  public static final XmlDslModel TLS_XML_DSL_MODEL = XmlDslModel.builder()
      .setPrefix(TLS_PREFIX)
      .setNamespace(TLS_NAMESPACE_URI)
      .setSchemaVersion(MULE_VERSION)
      .setXsdFileName("mule-tls.xsd")
      .setSchemaLocation("http://www.mulesoft.org/schema/mule/tls/current/mule-tls.xsd")
      .build();

  public static final String REDELIVERY_POLICY_ELEMENT_NAME = "redelivery-policy";

  private static final Map<InfrastructureTypeKey, InfrastructureType> MAPPING =
      ImmutableMap.<InfrastructureTypeKey, InfrastructureType>builder()
          .put(new JavaClassInfrastructureTypeKey(TlsContextFactory.class),
               new InfrastructureType(TLS_PARAMETER_NAME, 8))
          .put(new JavaClassInfrastructureTypeKey(SourceTransactionalAction.class),
               new InfrastructureType(TRANSACTIONAL_ACTION_PARAMETER_NAME, 6))
          .put(new JavaClassInfrastructureTypeKey(OperationTransactionalAction.class),
               new InfrastructureType(TRANSACTIONAL_ACTION_PARAMETER_NAME, 7))
          .put(new JavaClassInfrastructureTypeKey(TransactionType.class),
               new InfrastructureType(TRANSACTIONAL_TYPE_PARAMETER_NAME, 9))
          .put(new JavaClassInfrastructureTypeKey(SchedulingStrategy.class),
               new InfrastructureType(SCHEDULING_STRATEGY_PARAMETER_NAME, 10))
          .put(new MetadataTypeInfrastructureTypeKey("errorMapping"),
               new InfrastructureType(ERROR_MAPPINGS_PARAMETER_NAME, 11))

          .put(new MetadataTypeInfrastructureTypeKey(REDELIVERY_POLICY_TYPE_KEY),
               new InfrastructureType(REDELIVERY_POLICY_PARAMETER_NAME, 13))

          .put(new MetadataTypeInfrastructureTypeKey(RECONNECT_SIMPLE_TYPE_KEY),
               new InfrastructureType(RECONNECT_SIMPLE_TYPE_KEY, 14))
          .put(new MetadataTypeInfrastructureTypeKey(RECONNECT_FOREVER_TYPE_KEY),
               new InfrastructureType(RECONNECT_FOREVER_TYPE_KEY, 15))

          .put(new MetadataTypeInfrastructureTypeKey(STREAMING_STRATEGY_REPEATABLE_IN_MEMORY_STREAM),
               new InfrastructureType(STREAMING_STRATEGY_REPEATABLE_IN_MEMORY_STREAM, 16))
          .put(new MetadataTypeInfrastructureTypeKey(STREAMING_STRATEGY_REPEATABLE_FILE_STREAM),
               new InfrastructureType(STREAMING_STRATEGY_REPEATABLE_FILE_STREAM, 17))
          .put(new MetadataTypeInfrastructureTypeKey(STREAMING_STRATEGY_NON_REPEATABLE_STREAM),
               new InfrastructureType(STREAMING_STRATEGY_NON_REPEATABLE_STREAM, 18))
          .put(new MetadataTypeInfrastructureTypeKey(STREAMING_STRATEGY_REPEATABLE_IN_MEMORY_ITERABLE),
               new InfrastructureType(STREAMING_STRATEGY_REPEATABLE_IN_MEMORY_ITERABLE, 19))
          .put(new MetadataTypeInfrastructureTypeKey(STREAMING_STRATEGY_REPEATABLE_FILE_ITERABLE),
               new InfrastructureType(STREAMING_STRATEGY_REPEATABLE_FILE_ITERABLE, 20))
          .put(new MetadataTypeInfrastructureTypeKey(STREAMING_STRATEGY_NON_REPEATABLE_ITERABLE),
               new InfrastructureType(STREAMING_STRATEGY_NON_REPEATABLE_ITERABLE, 21))

          .build();

  private static final Map<Type, InfrastructureType> TYPE_MAPPING = MAPPING.entrySet()
      .stream()
      .filter(entry -> entry.getKey() instanceof JavaClassInfrastructureTypeKey)
      .collect(toImmutableMap(entry -> new TypeWrapper(((JavaClassInfrastructureTypeKey) entry.getKey()).getJavaType(),
                                                       new DefaultExtensionsTypeLoaderFactory()
                                                           .createTypeLoader(InfrastructureTypeMapping.class.getClassLoader())),
                              Map.Entry::getValue));

  public static Optional<InfrastructureType> getInfrastructureType(Type type) {
    return TYPE_MAPPING.entrySet()
        .stream()
        .filter(entry -> entry.getKey().isSameType(type))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  private static final Map<String, QNameModelProperty> QNAMES = ImmutableMap.<String, QNameModelProperty>builder()
      .put(SCHEDULING_STRATEGY_PARAMETER_NAME,
           new QNameModelProperty(new QName(CORE_NAMESPACE, SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER, CORE_PREFIX)))
      .put(ERROR_MAPPINGS_PARAMETER_NAME,
           new QNameModelProperty(new QName(CORE_NAMESPACE, ERROR_MAPPINGS_PARAMETER_NAME, CORE_PREFIX)))
      .put(REDELIVERY_POLICY_PARAMETER_NAME,
           new QNameModelProperty(new QName(CORE_NAMESPACE, REDELIVERY_POLICY_ELEMENT_NAME, CORE_PREFIX)))
      .put(RECONNECTION_CONFIG_PARAMETER_NAME,
           new QNameModelProperty(new QName(CORE_NAMESPACE, RECONNECTION_CONFIG_PARAMETER_NAME, CORE_PREFIX)))

      .put(RECONNECT_SIMPLE_TYPE_KEY,
           new QNameModelProperty(new QName(CORE_NAMESPACE, RECONNECT_ALIAS, CORE_PREFIX)))
      .put(RECONNECT_FOREVER_TYPE_KEY,
           new QNameModelProperty(new QName(CORE_NAMESPACE, RECONNECT_FOREVER_ALIAS, CORE_PREFIX)))

      .put(STREAMING_STRATEGY_REPEATABLE_IN_MEMORY_STREAM,
           new QNameModelProperty(new QName(CORE_NAMESPACE, REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS, CORE_PREFIX)))
      .put(STREAMING_STRATEGY_REPEATABLE_FILE_STREAM,
           new QNameModelProperty(new QName(CORE_NAMESPACE, REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS, EE_PREFIX)))
      .put(STREAMING_STRATEGY_NON_REPEATABLE_STREAM,
           new QNameModelProperty(new QName(CORE_NAMESPACE, NON_REPEATABLE_BYTE_STREAM_ALIAS, CORE_PREFIX)))
      .put(STREAMING_STRATEGY_REPEATABLE_IN_MEMORY_ITERABLE,
           new QNameModelProperty(new QName(CORE_NAMESPACE, REPEATABLE_IN_MEMORY_OBJECTS_STREAM_ALIAS, CORE_PREFIX)))
      .put(STREAMING_STRATEGY_REPEATABLE_FILE_ITERABLE,
           new QNameModelProperty(new QName(CORE_NAMESPACE, REPEATABLE_FILE_STORE_OBJECTS_STREAM_ALIAS, EE_PREFIX)))
      .put(STREAMING_STRATEGY_NON_REPEATABLE_ITERABLE,
           new QNameModelProperty(new QName(CORE_NAMESPACE, NON_REPEATABLE_OBJECTS_STREAM_ALIAS, CORE_PREFIX)))

      .put(TLS_PARAMETER_NAME,
           new QNameModelProperty(new QName(TLS_NAMESPACE_URI,
                                            TLS_CONTEXT_ELEMENT_IDENTIFIER,
                                            TLS_PREFIX)))
      .put(TLS_CUSTOM_OCSP_RESPONDER_ELEMENT_IDENTIFIER,
           new QNameModelProperty(new QName(TLS_NAMESPACE_URI,
                                            TLS_CUSTOM_OCSP_RESPONDER_ELEMENT_IDENTIFIER,
                                            TLS_PREFIX)))
      .put(TLS_STANDARD_REVOCATION_CHECK_ELEMENT_IDENTIFIER,
           new QNameModelProperty(new QName(TLS_NAMESPACE_URI,
                                            TLS_STANDARD_REVOCATION_CHECK_ELEMENT_IDENTIFIER,
                                            TLS_PREFIX)))
      .put(TLS_CRL_FILE_ELEMENT_IDENTIFIER,
           new QNameModelProperty(new QName(TLS_NAMESPACE_URI,
                                            TLS_CRL_FILE_ELEMENT_IDENTIFIER,
                                            TLS_PREFIX)))
      .build();

  private static final Map<ComponentIdentifier, InfrastructureTypeKey> IDENTIFIER_TYPE_MAPPING = MAPPING.entrySet()
      .stream()
      .filter(entry -> getQName(entry.getValue().getName()).isPresent())
      .collect(toImmutableMap(entry -> {
        final QName qName = getQName(entry.getValue().getName()).get().getValue();
        return ComponentIdentifier.builder()
            .namespaceUri(qName.getNamespaceURI())
            .namespace(qName.getPrefix())
            .name(qName.getLocalPart())
            .build();
      },
                              Map.Entry::getKey));

  private static final Map<String, ParameterDslConfiguration> DSL_CONFIGURATIONS =
      ImmutableMap.<String, ParameterDslConfiguration>builder()
          .put(SCHEDULING_STRATEGY_PARAMETER_NAME,
               ParameterDslConfiguration.builder()
                   .allowsInlineDefinition(true)
                   .allowTopLevelDefinition(false)
                   .allowsReferences(false)
                   .build())
          .put(ERROR_MAPPINGS_PARAMETER_NAME,
               ParameterDslConfiguration.builder()
                   .allowsInlineDefinition(true)
                   .allowTopLevelDefinition(false)
                   .allowsReferences(false)
                   .build())
          .put(TLS_PARAMETER_NAME,
               ParameterDslConfiguration.builder()
                   .allowsInlineDefinition(true)
                   .allowTopLevelDefinition(true)
                   .allowsReferences(true)
                   .build())
          .put(TRANSACTIONAL_ACTION_PARAMETER_NAME,
               ParameterDslConfiguration.builder()
                   .allowsInlineDefinition(false)
                   .allowTopLevelDefinition(false)
                   .allowsReferences(false)
                   .build())
          .put(TRANSACTIONAL_TYPE_PARAMETER_NAME,
               ParameterDslConfiguration.builder()
                   .allowsInlineDefinition(false)
                   .allowTopLevelDefinition(false)
                   .allowsReferences(false)
                   .build())
          .build();

  private static Map<String, String> nameMap =
      MAPPING.entrySet()
          .stream()
          .filter(entry -> entry.getKey() instanceof JavaClassInfrastructureTypeKey)
          .collect(toImmutableMap(e -> ((JavaClassInfrastructureTypeKey) e.getKey()).getJavaType().getName(),
                                  e -> e.getValue().getName()));

  public static Map<String, String> getNameMap() {
    return nameMap;
  }

  public static Optional<QNameModelProperty> getQName(String name) {
    return ofNullable(QNAMES.get(name));
  }

  public static Optional<ParameterDslConfiguration> getDslConfiguration(String name) {
    return ofNullable(DSL_CONFIGURATIONS.get(name));
  }

  public static Optional<Class<?>> getTypeFor(ComponentIdentifier compId) {
    return ofNullable(IDENTIFIER_TYPE_MAPPING.get(compId))
        .filter(itk -> itk instanceof JavaClassInfrastructureTypeKey)
        .map(itk -> ((JavaClassInfrastructureTypeKey) itk).getJavaType());
  }

  public static Optional<MetadataType> getInfrastructureMetadataTypeFor(ComponentIdentifier compId,
                                                                        Function<Class<?>, MetadataType> javaClassMapper) {
    return ofNullable(IDENTIFIER_TYPE_MAPPING.get(compId))
        .flatMap(typeFor -> {
          if (typeFor instanceof MetadataTypeInfrastructureTypeKey) {
            return ((MetadataTypeInfrastructureTypeKey) typeFor).metadataType();
          } else {
            return ofNullable(javaClassMapper.apply(((JavaClassInfrastructureTypeKey) typeFor).getJavaType()));
          }
        });
  }

  public static class InfrastructureType {

    private final String name;
    private final int sequence;

    InfrastructureType(String name, int sequence) {
      this.name = name;
      this.sequence = sequence;
    }

    public String getName() {
      return name;
    }

    public int getSequence() {
      return sequence;
    }
  }


  private interface InfrastructureTypeKey {

  }

  private static class JavaClassInfrastructureTypeKey implements InfrastructureTypeKey {

    private final Class<?> javaType;

    public JavaClassInfrastructureTypeKey(Class<?> javaType) {
      this.javaType = javaType;
    }

    public Class<?> getJavaType() {
      return javaType;
    }
  }

  private static class MetadataTypeInfrastructureTypeKey implements InfrastructureTypeKey {

    private final String typeName;

    public MetadataTypeInfrastructureTypeKey(String typeName) {
      this.typeName = typeName;
    }

    public String getTypeName() {
      return typeName;
    }

    public Optional<MetadataType> metadataType() {
      return of(getInfrastructureParameterType(getTypeName()));
    }
  }

}
