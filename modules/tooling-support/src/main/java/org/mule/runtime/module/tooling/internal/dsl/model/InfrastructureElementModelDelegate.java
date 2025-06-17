/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.dsl.model;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_PREFIX;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.EE_PREFIX;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.EXPIRATION_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.POOLING_PROFILE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.RECONNECT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.TLS_CONTEXT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.TLS_KEY_STORE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.TLS_PREFIX;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.TLS_REVOCATION_CHECK_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.TLS_TRUST_STORE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.extension.api.ExtensionConstants.EXPIRATION_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_CONFIG_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.SCHEDULING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECT_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_FILE_STORE_OBJECTS_STREAM_ALIAS;
import static org.mule.runtime.extension.api.loader.util.InfrastructureTypeUtils.getInfrastructureType;

import static java.lang.String.format;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.ParameterValueVisitor;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.extension.api.property.QNameModelProperty;
import org.mule.runtime.metadata.api.dsl.DslElementModel;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

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

  public static final ComponentIdentifier RECONNECTION_CONFIG_PARAMETER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(RECONNECTION_CONFIG_PARAMETER_NAME).build();
  public static final ComponentIdentifier SCHEDULING_STRATEGY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER).build();
  public static final ComponentIdentifier TLS_CONTEXT_IDENTIFIER =
      builder().namespace(TLS_PREFIX).name(TLS_CONTEXT_ELEMENT_IDENTIFIER).build();

  private final Set<String> eeStreamingStrategies =
      ImmutableSet.of(REPEATABLE_FILE_STORE_BYTES_STREAM_ALIAS, REPEATABLE_FILE_STORE_OBJECTS_STREAM_ALIAS);

  public void addParameter(String parameterName, ParameterValue value,
                           ParameterModel parameterModel,
                           DslElementSyntax paramDsl,
                           ComponentConfiguration.Builder parentConfig,
                           DslElementModel.Builder parentElement,
                           DslResolvingContext context, DslSyntaxResolver dsl) {

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

      case SCHEDULING_STRATEGY_PARAMETER_NAME:
        createSchedulingStrategy((ParameterObjectValue) value, parameterModel, paramDsl, parentConfig, parentElement, context,
                                 dsl);
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

  private void createSchedulingStrategy(ParameterObjectValue value,
                                        ParameterModel parameterModel,
                                        DslElementSyntax paramDsl,
                                        ComponentConfiguration.Builder parentConfig,
                                        DslElementModel.Builder parentElement,
                                        DslResolvingContext context,
                                        DslSyntaxResolver dsl) {

    ComponentConfiguration.Builder schedulingWrapperConfig = ComponentConfiguration.builder()
        .withIdentifier(SCHEDULING_STRATEGY_IDENTIFIER);

    DslElementModel.Builder schedulingElement = DslElementModel.builder()
        .withDsl(paramDsl)
        .withModel(parameterModel);

    context.getTypeCatalog().getType(value.getTypeId())
        .ifPresent(strategyType -> {
          dsl.resolve(strategyType)
              .ifPresent(strategyDsl -> {
                ComponentConfiguration.Builder strategyConfig = ComponentConfiguration.builder()
                    .withIdentifier(builder()
                        .namespace(CORE_PREFIX)
                        .name(strategyDsl.getElementName())
                        .build());

                cloneParameters(value, strategyConfig);

                ComponentConfiguration strategy = strategyConfig.build();

                schedulingWrapperConfig.withNestedComponent(strategy);

                ComponentConfiguration schedulingWrapper = schedulingWrapperConfig.build();
                schedulingElement
                    .withConfig(schedulingWrapper)
                    .containing(DslElementModel.builder()
                        .withModel(strategyType)
                        .withDsl(strategyDsl)
                        .withConfig(strategy)
                        .build());

                parentConfig.withNestedComponent(schedulingWrapper);
                parentElement.containing(schedulingElement.build());
              });
        });

  }

  private void createTlsContext(ParameterValue value,
                                ParameterModel parameterModel,
                                DslElementSyntax paramDsl,
                                ComponentConfiguration.Builder parentConfig,
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

        ComponentConfiguration.Builder tlsConfig = ComponentConfiguration.builder()
            .withIdentifier(TLS_CONTEXT_IDENTIFIER);

        objectValue.getParameters()
            .forEach((name, value) -> value.accept(new ParameterValueVisitor() {

              @Override
              public void visitSimpleValue(ParameterSimpleValue text) {
                tlsConfig.withParameter(name, text.getValue());
              }

              @Override
              public void visitObjectValue(ParameterObjectValue objectValue) {
                if (!(TLS_KEY_STORE_ELEMENT_IDENTIFIER.equals(name)
                    || TLS_TRUST_STORE_ELEMENT_IDENTIFIER.equals(name)
                    || TLS_REVOCATION_CHECK_ELEMENT_IDENTIFIER.equals(name))) {

                  LOGGER.debug("Skipping unknown parameter with name [{}] for TLSContext", name);
                  return;
                }

                ComponentConfiguration.Builder innerComponent =
                    ComponentConfiguration.builder()
                        .withIdentifier(builder()
                            .namespace(TLS_PREFIX)
                            .name(name)
                            .build());

                if (TLS_REVOCATION_CHECK_ELEMENT_IDENTIFIER.equals(name)) {
                  getInfrastructureType(objectValue.getTypeId())
                      .getQNameModelProperty()
                      .map(QNameModelProperty::getValue)
                      .ifPresent(qname -> {
                        ComponentConfiguration.Builder nested = ComponentConfiguration.builder()
                            .withIdentifier(builder()
                                .namespace(qname.getPrefix())
                                .name(qname.getLocalPart())
                                .build());
                        cloneParameters(objectValue, nested);
                        innerComponent.withNestedComponent(nested.build());
                      });
                } else {
                  cloneParameters(objectValue, innerComponent);
                }

                tlsConfig.withNestedComponent(innerComponent.build());
              }
            }));

        addParameterElement(parameterModel, paramDsl, parentConfig, parentElement, tlsConfig.build());
      }
    });

  }

  private void createReconnectionConfig(ParameterValue value,
                                        ParameterModel parameterModel,
                                        DslElementSyntax paramDsl,
                                        ComponentConfiguration.Builder parentConfig,
                                        DslElementModel.Builder parentElement) {

    ComponentConfiguration.Builder config = ComponentConfiguration.builder()
        .withIdentifier(RECONNECTION_CONFIG_PARAMETER_IDENTIFIER);

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
                                          ComponentConfiguration.Builder parentConfig,
                                          DslElementModel.Builder parentElement) {

    ParameterObjectValue objectValue = (ParameterObjectValue) value;
    checkArgument(!isBlank(objectValue.getTypeId()), "Missing declaration of which reconnection to use");

    String elementName = objectValue.getTypeId().equals(RECONNECT_ALIAS)
        ? RECONNECT_ELEMENT_IDENTIFIER
        : RECONNECT_FOREVER_ELEMENT_IDENTIFIER;

    cloneDeclarationToElement(parameterModel, paramDsl, parentConfig, parentElement, objectValue, elementName,
                              paramDsl.getNamespace());
  }

  private void createStreamingStrategy(ParameterValue value,
                                       ParameterModel parameterModel,
                                       DslElementSyntax paramDsl,
                                       ComponentConfiguration.Builder parentConfig,
                                       DslElementModel.Builder parentElement) {

    ParameterObjectValue objectValue = (ParameterObjectValue) value;
    checkArgument(!isBlank(objectValue.getTypeId()), "Missing declaration of which streaming strategy to use");

    String namespace = eeStreamingStrategies.contains(objectValue.getTypeId()) ? EE_PREFIX : CORE_PREFIX;
    cloneDeclarationToElement(parameterModel, paramDsl, parentConfig, parentElement, objectValue, objectValue.getTypeId(),
                              namespace);
  }

  private void cloneDeclarationToElement(Object parameterModel, DslElementSyntax paramDsl,
                                         ComponentConfiguration.Builder parentConfig,
                                         DslElementModel.Builder parentElement,
                                         ParameterObjectValue objectValue, String elementName, String customNamespace) {

    ComponentConfiguration.Builder config = ComponentConfiguration.builder()
        .withIdentifier(builder()
            .namespace(isBlank(customNamespace) ? CORE_PREFIX : customNamespace)
            .name(elementName)
            .build());

    cloneParameters(objectValue, config);

    addParameterElement(parameterModel, paramDsl, parentConfig, parentElement, config.build());
  }

  private void addParameterElement(Object parameterModel, DslElementSyntax paramDsl,
                                   ComponentConfiguration.Builder parentConfig, DslElementModel.Builder parentElement,
                                   ComponentConfiguration result) {
    parentConfig.withNestedComponent(result);
    parentElement.containing(DslElementModel.builder()
        .withModel(parameterModel)
        .withDsl(paramDsl)
        .withConfig(result).build());
  }

  private void cloneParameters(ParameterObjectValue objectValue,
                               final ComponentConfiguration.Builder config) {
    objectValue.getParameters()
        .forEach((name, value) -> value.accept(new ParameterValueVisitor() {

          @Override
          public void visitSimpleValue(ParameterSimpleValue text) {
            config.withParameter(name, text.getValue());
          }

        }));
  }

}
