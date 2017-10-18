/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.ExtensionConstants.EXPIRATION_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_CONFIG_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECT_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EXPIRATION_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.POOLING_PROFILE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_CONTEXT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_KEY_STORE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_TRUST_STORE_ELEMENT_IDENTIFIER;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.ParameterValueVisitor;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.internal.component.config.InternalComponentConfiguration;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegate to be used by a {@link DeclarationBasedElementModelFactory} in order to resolve the {@link DslElementModel} of an
 * infrastructure parameter.
 *
 * @since 4.0
 */
class InfrastructureElementModelDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(InfrastructureElementModelDelegate.class);

  private final Set<String> eeStreamingStrategies =
      ImmutableSet.of(REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS, REPEATABLE_FILE_STORE_OBJECTS_STREAM_ALIAS);

  public void addParameter(String parameterName, ParameterValue value,
                           ParameterModel parameterModel,
                           DslElementSyntax paramDsl,
                           InternalComponentConfiguration.Builder parentConfig,
                           DslElementModel.Builder parentElement) {

    switch (parameterName) {
      case RECONNECTION_CONFIG_PARAMETER_NAME:
        createReconnectionConfig(value, parameterModel, paramDsl, parentConfig, parentElement);
        return;

      case RECONNECTION_STRATEGY_PARAMETER_NAME:
        createReconnectionStrategy(value, parameterModel, paramDsl, parentConfig, parentElement);
        return;

      case EXPIRATION_POLICY_PARAMETER_NAME:
        cloneDeclarationToElement(parameterModel, paramDsl, parentConfig, parentElement,
                                  (ParameterObjectValue) value,
                                  EXPIRATION_POLICY_ELEMENT_IDENTIFIER, paramDsl.getNamespace());
        return;

      case STREAMING_STRATEGY_PARAMETER_NAME:
        createStreamingStrategy(value, parameterModel, paramDsl, parentConfig, parentElement);
        return;

      case REDELIVERY_POLICY_PARAMETER_NAME:
        cloneDeclarationToElement(parameterModel, paramDsl, parentConfig, parentElement,
                                  (ParameterObjectValue) value,
                                  REDELIVERY_POLICY_ELEMENT_IDENTIFIER, paramDsl.getNamespace());
        return;

      case POOLING_PROFILE_PARAMETER_NAME:
        cloneDeclarationToElement(parameterModel, paramDsl, parentConfig, parentElement,
                                  (ParameterObjectValue) value,
                                  POOLING_PROFILE_ELEMENT_IDENTIFIER, paramDsl.getNamespace());
        return;

      case TLS_PARAMETER_NAME:
        createTlsContext(value, parameterModel, paramDsl, parentConfig, parentElement);
        return;

      default:
        value.accept(new ParameterValueVisitor() {

          @Override
          public void visitSimpleValue(ParameterSimpleValue text) {
            parentConfig.withParameter(parameterName, text.getValue());
            parentElement.containing(DslElementModel.builder()
                .withModel(parameterModel)
                .withDsl(paramDsl)
                .withValue(text.getValue())
                .build());
          }
        });
    }
  }

  private void createTlsContext(ParameterValue value,
                                ParameterModel parameterModel,
                                DslElementSyntax paramDsl,
                                InternalComponentConfiguration.Builder parentConfig,
                                DslElementModel.Builder parentElement) {

    value.accept(new ParameterValueVisitor() {

      @Override
      public void visitSimpleValue(ParameterSimpleValue text) {
        parentConfig.withParameter(TLS_PARAMETER_NAME, text.getValue());
        parentElement.containing(DslElementModel.builder()
            .withModel(parameterModel)
            .withDsl(paramDsl)
            .build());
      }

      @Override
      public void visitObjectValue(ParameterObjectValue objectValue) {

        InternalComponentConfiguration.Builder tlsConfig = InternalComponentConfiguration.builder()
            .withIdentifier(builder()
                .namespace(TLS_PREFIX)
                .name(TLS_CONTEXT_ELEMENT_IDENTIFIER)
                .build());

        objectValue.getParameters()
            .forEach((name, value) -> value.accept(new ParameterValueVisitor() {

              @Override
              public void visitSimpleValue(ParameterSimpleValue text) {
                tlsConfig.withParameter(name, text.getValue());
              }

              @Override
              public void visitObjectValue(ParameterObjectValue objectValue) {
                if (!(TLS_KEY_STORE_ELEMENT_IDENTIFIER.equals(name) || TLS_TRUST_STORE_ELEMENT_IDENTIFIER.equals(name))) {
                  if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(format("Skipping unknown parameter with name [%s] for TLSContext", name));
                  }
                  return;
                }

                InternalComponentConfiguration.Builder nested = InternalComponentConfiguration.builder()
                    .withIdentifier(builder()
                        .namespace(TLS_PREFIX)
                        .name(name)
                        .build());

                cloneParameters(objectValue, nested);
                tlsConfig.withNestedComponent(nested.build());
              }
            }));

        addParameterElement(parameterModel, paramDsl, parentConfig, parentElement, tlsConfig.build());
      }
    });

  }

  private void createReconnectionConfig(ParameterValue value,
                                        ParameterModel parameterModel,
                                        DslElementSyntax paramDsl,
                                        InternalComponentConfiguration.Builder parentConfig,
                                        DslElementModel.Builder parentElement) {

    InternalComponentConfiguration.Builder config = InternalComponentConfiguration.builder()
        .withIdentifier(builder()
            .namespace(CORE_PREFIX)
            .name(RECONNECTION_CONFIG_PARAMETER_NAME)
            .build());

    final DslElementModel.Builder<Object> elementBuilder = DslElementModel.builder()
        .withModel(parameterModel)
        .withDsl(paramDsl);

    ((ParameterObjectValue) value).getParameters()
        .forEach((name, fieldValue) -> fieldValue.accept(new ParameterValueVisitor() {

          @Override
          public void visitSimpleValue(ParameterSimpleValue text) {
            config.withParameter(name, text.getValue());
          }

          @Override
          public void visitObjectValue(ParameterObjectValue objectValue) {
            if (name.equals(RECONNECTION_STRATEGY_PARAMETER_NAME)) {
              createReconnectionStrategy(fieldValue,
                                         ((ObjectType) parameterModel.getType())
                                             .getFieldByName(RECONNECTION_STRATEGY_PARAMETER_NAME).get(),
                                         paramDsl.getContainedElement(RECONNECTION_STRATEGY_PARAMETER_NAME).get(),
                                         config, elementBuilder);
            }
          }
        }));

    final ComponentConfiguration result = config.build();
    parentConfig.withNestedComponent(result);
    parentElement.containing(elementBuilder.withConfig(result).build());

  }

  private void createReconnectionStrategy(ParameterValue value,
                                          Object parameterModel,
                                          DslElementSyntax paramDsl,
                                          InternalComponentConfiguration.Builder parentConfig,
                                          DslElementModel.Builder parentElement) {

    ParameterObjectValue objectValue = (ParameterObjectValue) value;
    checkArgument(!isBlank(objectValue.getTypeId()), "Missing declaration of which reconnection to use");

    String elementName = objectValue.getTypeId().equals(RECONNECT_ALIAS)
        ? RECONNECT_ELEMENT_IDENTIFIER : RECONNECT_FOREVER_ELEMENT_IDENTIFIER;

    cloneDeclarationToElement(parameterModel, paramDsl, parentConfig, parentElement, objectValue, elementName,
                              paramDsl.getNamespace());
  }

  private void createStreamingStrategy(ParameterValue value,
                                       ParameterModel parameterModel,
                                       DslElementSyntax paramDsl,
                                       InternalComponentConfiguration.Builder parentConfig,
                                       DslElementModel.Builder parentElement) {

    ParameterObjectValue objectValue = (ParameterObjectValue) value;
    checkArgument(!isBlank(objectValue.getTypeId()), "Missing declaration of which streaming strategy to use");

    String namespace = eeStreamingStrategies.contains(objectValue.getTypeId()) ? EE_PREFIX : CORE_PREFIX;
    cloneDeclarationToElement(parameterModel, paramDsl, parentConfig, parentElement, objectValue, objectValue.getTypeId(),
                              namespace);
  }

  private void cloneDeclarationToElement(Object parameterModel, DslElementSyntax paramDsl,
                                         InternalComponentConfiguration.Builder parentConfig,
                                         DslElementModel.Builder parentElement,
                                         ParameterObjectValue objectValue, String elementName, String customNamespace) {

    InternalComponentConfiguration.Builder config = InternalComponentConfiguration.builder()
        .withIdentifier(builder()
            .namespace(isBlank(customNamespace) ? CORE_PREFIX : customNamespace)
            .name(elementName)
            .build());

    cloneParameters(objectValue, config);

    addParameterElement(parameterModel, paramDsl, parentConfig, parentElement, config.build());
  }

  private void addParameterElement(Object parameterModel, DslElementSyntax paramDsl,
                                   InternalComponentConfiguration.Builder parentConfig, DslElementModel.Builder parentElement,
                                   ComponentConfiguration result) {
    parentConfig.withNestedComponent(result);
    parentElement.containing(DslElementModel.builder()
        .withModel(parameterModel)
        .withDsl(paramDsl)
        .withConfig(result).build());
  }

  private void cloneParameters(ParameterObjectValue objectValue, final InternalComponentConfiguration.Builder redeliveryConfig) {
    objectValue.getParameters()
        .forEach((name, value) -> value.accept(new ParameterValueVisitor() {

          @Override
          public void visitSimpleValue(ParameterSimpleValue text) {
            redeliveryConfig.withParameter(name, text.getValue());
          }

        }));
  }

}
