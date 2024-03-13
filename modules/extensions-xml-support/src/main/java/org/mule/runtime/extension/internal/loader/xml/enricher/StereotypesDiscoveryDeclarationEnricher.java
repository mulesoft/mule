/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.xml.enricher;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.POST_STRUCTURE;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.el.BindingContextUtils;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.internal.ast.property.GlobalElementComponentModelModelProperty;
import org.mule.runtime.extension.internal.ast.property.OperationComponentModelModelProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


/**
 * {@link DeclarationEnricher} implementation which enriches the {@link ExtensionModel} and their {@link OperationModel} from the
 * used extensions adding the needed {@link StereotypeModel} to the current declaration.
 *
 * @since 4.2
 */
public class StereotypesDiscoveryDeclarationEnricher implements DeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return POST_STRUCTURE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    withContextClassLoader(extensionLoadingContext.getExtensionClassLoader(),
                           () -> new EnricherDelegate().apply(extensionLoadingContext));
  }

  private static class EnricherDelegate {

    public void apply(ExtensionLoadingContext extensionLoadingContext) {
      ExtensionDeclaration extensionDeclaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
      Optional<GlobalElementComponentModelModelProperty> modelProperty =
          extensionDeclaration.getModelProperty(GlobalElementComponentModelModelProperty.class);
      new IdempotentDeclarationWalker() {

        @Override
        protected void onConfiguration(ConfigurationDeclaration declaration) {
          walkDeclaration(modelProperty, declaration.getAllParameters());
        }

        @Override
        protected void onConnectionProvider(ConnectedDeclaration owner,
                                            ConnectionProviderDeclaration declaration) {
          if (owner instanceof ConfigurationDeclaration) {
            walkDeclaration(modelProperty, declaration.getAllParameters());
          }
        }

        @Override
        protected void onOperation(OperationDeclaration declaration) {
          declaration.getModelProperty(OperationComponentModelModelProperty.class)
              .ifPresent(modelProperty -> {
                ComponentAst bodyComponentModel = modelProperty.getBodyComponentModel();

                declaration.getAllParameters().stream()
                    .filter(parameterDeclaration -> parameterDeclaration.getType() instanceof StringType)
                    .forEach(parameterDeclaration -> traverseProperty(bodyComponentModel.directChildren(),
                                                                      parameterDeclaration));
              });


        }
      }.walk(extensionDeclaration);
    }

    private void walkDeclaration(Optional<GlobalElementComponentModelModelProperty> globalElementModelProperty,
                                 List<ParameterDeclaration> allParameters) {
      globalElementModelProperty.ifPresent(modelProperty -> allParameters.stream()
          .filter(parameterDeclaration -> parameterDeclaration.getType() instanceof StringType)
          .forEach(parameterDeclaration -> traverseProperty(modelProperty.getGlobalElements(),
                                                            parameterDeclaration)));
    }

    /**
     * Given a {@code parameterDeclaration} it will look up all the usages in the elements of the smart connector where for each
     * occurrence it will extract the stereotypes of the underlying connector's parameter to copy into the current declaration.
     * <br/>
     * If the result of those finding throws repeated stereotypes, then it will reduce that collection by taking the intersection,
     * making its usage within the smart connector compliant.
     *
     * @param componentModels      elements of the smart connector to read the possible usages of the parametrization
     * @param parameterDeclaration parameter used to look up in the elements' usages, and the one that will hold the stereotypes
     *                             if found.
     */
    private void traverseProperty(Collection<ComponentAst> componentModels,
                                  ParameterDeclaration parameterDeclaration) {
      final List<List<StereotypeModel>> allowedStereotypeModels = new ArrayList<>();
      componentModels.forEach(componentModel -> {
        allowedStereotypeModels.add(findStereotypes(componentModel, parameterDeclaration));
        componentModel.recursiveStream()
            .forEach(innerComponentModel -> allowedStereotypeModels
                .add(findStereotypes(innerComponentModel, parameterDeclaration)));
      });

      allowedStereotypeModels.stream()
          .filter(stereotypeModels -> !stereotypeModels.isEmpty())
          .reduce((stereotypeModels, stereotypeModels2) -> {
            final List<StereotypeModel> partialIntersection = new ArrayList<>(stereotypeModels);
            partialIntersection.retainAll(stereotypeModels2);
            return partialIntersection;
          }).ifPresent(parameterDeclaration::setAllowedStereotypeModels);
    }

    private List<StereotypeModel> findStereotypes(ComponentAst componentModel, ParameterDeclaration propertyDeclaration) {
      final String expectedPropertyReference = ExpressionManager.DEFAULT_EXPRESSION_PREFIX + BindingContextUtils.VARS + "."
          + propertyDeclaration.getName() + ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
      if (!componentModel.getModel(ParameterizedModel.class).isPresent()) {
        return emptyList();
      }

      return componentModel.getParameters().stream()
          .filter(paramAst -> expectedPropertyReference.equals(paramAst.getResolvedRawValue()))
          .map(paramAst -> paramAst.getModel().getName())
          .map(attributeName -> findStereotypes(componentModel, attributeName))
          .flatMap(Collection::stream)
          .collect(toList());
    }

    private List<StereotypeModel> findStereotypes(ComponentAst componentModel, String attributeName) {
      return componentModel.getModel(ParameterizedModel.class)
          .flatMap(pmzd -> pmzd.getAllParameterModels()
              .stream()
              .filter(pm -> pm.getName().equals(attributeName))
              .findAny())
          .map(pm -> pm.getAllowedStereotypes())
          .orElse(emptyList());
    }

  }
}
