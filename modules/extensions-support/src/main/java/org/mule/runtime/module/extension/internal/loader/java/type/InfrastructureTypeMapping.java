/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import static org.mule.runtime.api.util.collection.Collectors.toImmutableMap;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TRANSACTIONAL_TYPE_PARAMETER_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CONTEXT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_PREFIX;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.tx.TransactionType;
import org.mule.runtime.extension.api.tx.OperationTransactionalAction;
import org.mule.runtime.extension.api.tx.SourceTransactionalAction;
import org.mule.runtime.extension.api.property.QNameModelProperty;
import com.google.common.collect.ImmutableMap;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Optional;

/**
 * Mapping for types considered of "Infrastructure", of the {@link Class} of the infrastructure type and the {@link String} name
 * of it.
 *
 * @since 4.0
 */
public final class InfrastructureTypeMapping {

  private static Map<Class<?>, InfrastructureType> MAPPING = ImmutableMap.<Class<?>, InfrastructureType>builder()
      .put(TlsContextFactory.class, new InfrastructureType(TLS_PARAMETER_NAME, 8))
      .put(SourceTransactionalAction.class, new InfrastructureType(TRANSACTIONAL_ACTION_PARAMETER_NAME, 6))
      .put(OperationTransactionalAction.class, new InfrastructureType(TRANSACTIONAL_ACTION_PARAMETER_NAME, 7))
      .put(TransactionType.class, new InfrastructureType(TRANSACTIONAL_TYPE_PARAMETER_NAME, 9))
      .build();

  private static Map<String, QNameModelProperty> QNAMES = ImmutableMap.<String, QNameModelProperty>builder()
      .put(TLS_PARAMETER_NAME, new QNameModelProperty(new QName("http://www.mulesoft.org/schema/mule/tls",
                                                                TLS_CONTEXT_ELEMENT_IDENTIFIER,
                                                                TLS_PREFIX)))
      .build();

  private static Map<String, ParameterDslConfiguration> DSL_CONFIGURATIONS =
      ImmutableMap.<String, ParameterDslConfiguration>builder()
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

  private static Map<String, String> nameMap = MAPPING.entrySet().stream()
      .collect(toImmutableMap(e -> e.getKey().getName(), e -> e.getValue().getName()));

  public static Map<Class<?>, InfrastructureType> getMap() {
    return MAPPING;
  }

  public static Map<String, String> getNameMap() {
    return nameMap;
  }

  public static Optional<QNameModelProperty> getQName(String name) {
    return Optional.ofNullable(QNAMES.get(name));
  }

  public static Optional<ParameterDslConfiguration> getDslConfiguration(String name) {
    return Optional.ofNullable(DSL_CONFIGURATIONS.get(name));
  }

  private InfrastructureTypeMapping() {}

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
}
