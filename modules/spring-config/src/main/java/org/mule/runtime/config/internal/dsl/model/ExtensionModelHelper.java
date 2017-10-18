/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ERROR_HANDLER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.api.util.NameUtils.COMPONENT_NAME_SEPARATOR;
import static org.mule.runtime.api.util.NameUtils.toCamelCase;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.ORIGINAL_IDENTIFIER;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModelVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.api.meta.model.nested.NestableElementModelVisitor;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Helper class to work with a set of {@link ExtensionModel}s
 * <p/>
 * Contains a cache for searches within the extension models so we avoid processing each extension model twice.
 * <p/>
 * It's recommended that the application only has one instance of this class to avoid processing the extension models several
 * times.
 *
 * since 4.0
 */
public class ExtensionModelHelper {

  private final Set<ExtensionModel> extensionsModels;
  private Cache<ComponentIdentifier, Optional<? extends org.mule.runtime.api.meta.model.ComponentModel>> extensionComponentModelByComponentIdentifier =
      CacheBuilder.newBuilder().build();

  /**
   * @param extensionModels the set of {@link ExtensionModel}s to work with. Usually this is the set of models configured within a
   *        mule artifact.
   */
  public ExtensionModelHelper(Set<ExtensionModel> extensionModels) {
    this.extensionsModels = extensionModels;
  }

  /**
   * Find a {@link DslElementModel} for a given {@link ComponentModel}
   *
   * @param componentModel the component model from the configuration.
   * @return the {@link DslElementModel} associated with the configuration or an {@link Optional#empty()} if there isn't one.
   */
  public TypedComponentIdentifier.ComponentType findComponentType(ComponentModel componentModel) {
    Optional<? extends org.mule.runtime.api.meta.model.ComponentModel> extensionComponentModelOptional =
        findComponentModel(componentModel.getCustomAttributes().containsKey(ORIGINAL_IDENTIFIER)
            ? (ComponentIdentifier) componentModel.getCustomAttributes().get(ORIGINAL_IDENTIFIER)
            : componentModel.getIdentifier());
    return extensionComponentModelOptional.map(extensionComponentModel -> {
      Reference<TypedComponentIdentifier.ComponentType> componentTypeReference = new Reference<>();
      extensionComponentModel.accept(new ComponentModelVisitor() {

        @Override
        public void visit(OperationModel model) {
          componentTypeReference.set(OPERATION);
        }

        @Override
        public void visit(SourceModel model) {
          componentTypeReference.set(SOURCE);
        }

        @Override
        public void visit(ConstructModel model) {
          if (model.getStereotype().equals(MuleStereotypes.ERROR_HANDLER)) {
            componentTypeReference.set(ERROR_HANDLER);
            return;
          }
          if (model.getStereotype().equals(MuleStereotypes.FLOW)) {
            componentTypeReference.set(FLOW);
            return;
          }
          NestedComponentVisitor nestedComponentVisitor = new NestedComponentVisitor(componentTypeReference);
          for (NestableElementModel nestableElementModel : model.getNestedComponents()) {
            nestableElementModel.accept(nestedComponentVisitor);
            if (componentTypeReference.get() != null) {
              return;
            }
          }
        }
      });
      return componentTypeReference.get() == null ? UNKNOWN : componentTypeReference.get();
    }).orElse(UNKNOWN);
  }

  /**
   * Finds a {@link org.mule.runtime.api.meta.model.ComponentModel} within the provided set of {@link ExtensionModel}s by a
   * {@link ComponentIdentifier}.
   *
   * @param componentIdentifier the identifier to use for the search.
   * @return the found {@link org.mule.runtime.api.meta.model.ComponentModel} or {@link Optional#empty()} if it couldn't be found.
   */
  public Optional<? extends org.mule.runtime.api.meta.model.ComponentModel> findComponentModel(ComponentIdentifier componentIdentifier) {
    try {
      return extensionComponentModelByComponentIdentifier.get(componentIdentifier, () -> {
        String componentName = toCamelCase(componentIdentifier.getName(), COMPONENT_NAME_SEPARATOR);
        for (ExtensionModel extensionModel : extensionsModels) {
          if (extensionModel.getXmlDslModel().getPrefix().equals(componentIdentifier.getNamespace())) {
            List<HasOperationModels> operationModelsProviders = ImmutableList.<HasOperationModels>builder()
                .add(extensionModel).addAll(extensionModel.getConfigurationModels()).build();
            List<HasSourceModels> sourceModelsProviders = ImmutableList.<HasSourceModels>builder()
                .add(extensionModel).addAll(extensionModel.getConfigurationModels()).build();
            List<HasConstructModels> constructModelsProviders = singletonList(extensionModel);
            for (HasOperationModels operationModelsProvider : operationModelsProviders) {
              Optional<OperationModel> operationModel = operationModelsProvider.getOperationModel(componentName);
              if (operationModel.isPresent()) {
                return operationModel;
              }
            }
            for (HasSourceModels sourceModelsProvider : sourceModelsProviders) {
              Optional<SourceModel> sourceModel = sourceModelsProvider.getSourceModel(componentName);
              if (sourceModel.isPresent()) {
                return sourceModel;
              }
            }
            for (HasConstructModels constructModelsProvider : constructModelsProviders) {
              Optional<ConstructModel> constructModel = constructModelsProvider.getConstructModel(componentName);
              if (constructModel.isPresent()) {
                return constructModel;
              }
            }
          }
        }
        return empty();
      });
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * Visitor of {@link ConstructModel} that determines it
   * {@link org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType}
   */
  static class NestedComponentVisitor implements NestableElementModelVisitor {

    private Reference<TypedComponentIdentifier.ComponentType> reference;

    public NestedComponentVisitor(Reference<TypedComponentIdentifier.ComponentType> reference) {
      this.reference = reference;
    }

    @Override
    public void visit(NestedComponentModel component) {}

    @Override
    public void visit(NestedChainModel component) {
      reference.set(SCOPE);
    }

    @Override
    public void visit(NestedRouteModel component) {
      reference.set(ROUTER);
    }
  }

}
