/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.internal;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.api.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.api.dsl.DslConstants.POOLING_PROFILE_ELEMENT_IDENTIFIER;
import static org.mule.runtime.api.dsl.DslConstants.RECONNECT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.api.dsl.DslConstants.RECONNECT_FOREVER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.api.dsl.DslConstants.REDELIVERY_POLICY_ELEMENT_IDENTIFIER;
import static org.mule.runtime.api.dsl.DslConstants.TLS_CONTEXT_ELEMENT_IDENTIFIER;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.ExtensionConstants.INFRASTRUCTURE_PARAMETER_NAMES;
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECT_ALIAS;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.api.component.config.ComponentIdentifier;
import org.mule.runtime.api.app.declaration.ParameterElementDeclaration;
import org.mule.runtime.api.app.declaration.ParameterValueVisitor;
import org.mule.runtime.api.app.declaration.fluent.ParameterObjectValue;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.config.spring.dsl.model.DslElementModel;
import org.mule.runtime.config.spring.dsl.model.DslElementModelFactory;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

/**
 * Default implementation of a {@link DslElementModelFactory}
 *
 * @since 1.0
 */
class InfrastructureElementModelDelegate {

  public void addParameter(ParameterElementDeclaration declaration,
                           ParameterModel parameterModel,
                           DslElementSyntax paramDsl,
                           ComponentConfiguration.Builder parentConfig,
                           DslElementModel.Builder parentElement) {

    checkArgument(INFRASTRUCTURE_PARAMETER_NAMES.contains(declaration.getName()),
                  format("The parameter '%s' is not of infrastructure kind", declaration.getName()));

    switch (declaration.getName()) {
      case RECONNECTION_STRATEGY_PARAMETER_NAME:
        createReconnectionStrategy(declaration, parameterModel, paramDsl, parentConfig, parentElement);
        return;

      case REDELIVERY_POLICY_PARAMETER_NAME:
        cloneDeclarationToElement(parameterModel, paramDsl, parentConfig, parentElement,
                                  (ParameterObjectValue) declaration.getValue(),
                                  REDELIVERY_POLICY_ELEMENT_IDENTIFIER);
        return;

      case POOLING_PROFILE_PARAMETER_NAME:
        cloneDeclarationToElement(parameterModel, paramDsl, parentConfig, parentElement,
                                  (ParameterObjectValue) declaration.getValue(),
                                  POOLING_PROFILE_ELEMENT_IDENTIFIER);
        return;

      case TLS_PARAMETER_NAME:
        createTlsContext(declaration, parameterModel, paramDsl, parentConfig, parentElement);
        return;

      default:
        addSimpleParameter(declaration, parentConfig);
        parentElement.containing(DslElementModel.builder()
            .withModel(parameterModel)
            .withDsl(paramDsl)
            .build());
    }
  }

  private void createTlsContext(ParameterElementDeclaration declaration,
                                ParameterModel parameterModel,
                                DslElementSyntax paramDsl,
                                ComponentConfiguration.Builder parentConfig,
                                DslElementModel.Builder parentElement) {

    declaration.getValue().accept(new ParameterValueVisitor() {

      @Override
      public void visitSimpleValue(String value) {
        parentConfig.withParameter(TLS_PARAMETER_NAME, value);
        parentElement.containing(DslElementModel.builder()
            .withModel(parameterModel)
            .withDsl(paramDsl)
            .build());
      }

      @Override
      public void visitObjectValue(ParameterObjectValue objectValue) {

        ComponentConfiguration.Builder tlsConfig = ComponentConfiguration.builder()
            .withIdentifier(ComponentIdentifier.builder()
                .withNamespace("tls")
                .withName(TLS_CONTEXT_ELEMENT_IDENTIFIER)
                .build());

        objectValue.getParameters()
            .forEach((name, value) -> value.accept(new ParameterValueVisitor() {

              @Override
              public void visitSimpleValue(String value) {
                tlsConfig.withParameter(name, value);
              }

              @Override
              public void visitObjectValue(ParameterObjectValue objectValue) {
                ComponentConfiguration.Builder nested = ComponentConfiguration.builder()
                    .withIdentifier(ComponentIdentifier.builder()
                        .withNamespace("tls")
                        .withName(name)
                        .build());

                cloneParameters(objectValue, nested);
                tlsConfig.withNestedComponent(nested.build());
              }
            }));

        addParameterElement(parameterModel, paramDsl, parentConfig, parentElement, tlsConfig.build());
      }
    });

  }

  private void createReconnectionStrategy(ParameterElementDeclaration declaration,
                                          ParameterModel parameterModel,
                                          DslElementSyntax paramDsl,
                                          ComponentConfiguration.Builder parentConfig,
                                          DslElementModel.Builder parentElement) {

    ParameterObjectValue objectValue = (ParameterObjectValue) declaration.getValue();
    checkArgument(!isBlank(objectValue.getTypeId()), "Missing declaration of which reconnection to use");

    String elementName = objectValue.getTypeId().equals(RECONNECT_ALIAS)
        ? RECONNECT_ELEMENT_IDENTIFIER : RECONNECT_FOREVER_ELEMENT_IDENTIFIER;

    cloneDeclarationToElement(parameterModel, paramDsl, parentConfig, parentElement, objectValue, elementName);
  }

  private void cloneDeclarationToElement(ParameterModel parameterModel, DslElementSyntax paramDsl,
                                         ComponentConfiguration.Builder parentConfig, DslElementModel.Builder parentElement,
                                         ParameterObjectValue objectValue, String elementName) {

    ComponentConfiguration.Builder redeliveryConfig = ComponentConfiguration.builder()
        .withIdentifier(ComponentIdentifier.builder()
            .withNamespace(CORE_NAMESPACE)
            .withName(elementName)
            .build());

    cloneParameters(objectValue, redeliveryConfig);

    addParameterElement(parameterModel, paramDsl, parentConfig, parentElement, redeliveryConfig.build());
  }

  private void addParameterElement(ParameterModel parameterModel, DslElementSyntax paramDsl,
                                   ComponentConfiguration.Builder parentConfig, DslElementModel.Builder parentElement,
                                   ComponentConfiguration result) {
    parentConfig.withNestedComponent(result);
    parentElement.containing(DslElementModel.builder()
        .withModel(parameterModel)
        .withDsl(paramDsl)
        .withConfig(result).build());
  }

  private void cloneParameters(ParameterObjectValue objectValue, final ComponentConfiguration.Builder redeliveryConfig) {
    objectValue.getParameters()
        .forEach((name, value) -> value.accept(new ParameterValueVisitor() {

          @Override
          public void visitSimpleValue(String value) {
            redeliveryConfig.withParameter(name, value);
          }

        }));
  }

  private void addSimpleParameter(ParameterElementDeclaration declaration,
                                  final ComponentConfiguration.Builder parentConfig) {
    declaration.getValue().accept(new ParameterValueVisitor() {

      @Override
      public void visitSimpleValue(String value) {
        parentConfig.withParameter(declaration.getName(), value);
      }
    });
  }

}
