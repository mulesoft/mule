/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static org.apache.commons.lang3.text.WordUtils.capitalize;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.api.util.NameUtils.hyphenize;
import static org.mule.runtime.api.util.NameUtils.sanitizeName;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.function.FunctionModel;
import org.mule.runtime.api.meta.model.function.HasFunctionModels;
import org.mule.runtime.api.meta.model.nested.NestableElementModelVisitor;
import org.mule.runtime.api.meta.model.nested.NestedChainModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.api.dsl.model.ComponentModel;
import org.mule.runtime.config.api.dsl.model.DslElementModel;

import java.util.Optional;
import java.util.Set;

/**
 * Helper class to work with a set of {@link ExtensionModel}s
 * <p/>
 * Contains a cache for searches within the extension models so we avoid processing each extension model twice.
 *
 * since 4.0
 */
public class ExtensionModelHelper {

  private final Set<ExtensionModel> extensionsModels;

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
    Reference<TypedComponentIdentifier.ComponentType> componentTypeReference = new Reference<>();
    resolveComponentName(componentModel.getIdentifier().getName());
    for (ExtensionModel extensionsModel : extensionsModels) {
      if (extensionsModel.getXmlDslModel().getPrefix().equals(componentModel.getIdentifier().getNamespace())) {
        new ExtensionWalker() {

          @Override
          protected void onOperation(HasOperationModels owner, OperationModel model) {
            if (!resolveComponentName(model.getName()).equals(componentModel.getIdentifier().getName())) {
              return;
            }
            componentTypeReference.set(OPERATION);
            stop();
          }

          @Override
          protected void onConstruct(HasConstructModels owner, ConstructModel model) {
            if (!resolveComponentName(model.getName()).equals(componentModel.getIdentifier().getName())) {
              return;
            }
            model.getNestedComponents().forEach(nestedComponentType -> {
              nestedComponentType.accept(new NestedComponentVisitor(componentTypeReference));
            });
            stop();
          }

          @Override
          protected void onSource(HasSourceModels owner, SourceModel model) {
            if (!resolveComponentName(model.getName()).equals(componentModel.getIdentifier().getName())) {
              return;
            }
            componentTypeReference.set(SOURCE);
            stop();
          }

          @Override
          protected void onConfiguration(ConfigurationModel model) {
            return;
          }

          @Override
          protected void onFunction(HasFunctionModels owner, FunctionModel model) {
            return;
          }

          @Override
          protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
            return;
          }

          @Override
          protected void onParameterGroup(ParameterizedModel owner, ParameterGroupModel model) {
            return;
          }

          @Override
          protected void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
            return;
          }
        }.walk(extensionsModel);
      }
    }
    return componentTypeReference.get() != null ? componentTypeReference.get() : UNKNOWN;
  }

  public String resolveComponentName(String componentModelName) {
    return hyphenize(sanitizeName(capitalize(componentModelName))).replaceAll("\\s+", "");
  }

  static class NestedComponentVisitor implements NestableElementModelVisitor {

    private Reference<TypedComponentIdentifier.ComponentType> reference;

    public NestedComponentVisitor(Reference<TypedComponentIdentifier.ComponentType> reference) {
      this.reference = reference;
    }

    @Override
    public void visit(NestedComponentModel component) {

    }

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
