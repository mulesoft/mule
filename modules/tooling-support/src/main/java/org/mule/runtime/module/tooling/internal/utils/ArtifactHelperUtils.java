/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.utils;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.construct.HasConstructModels;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ConnectionElementDeclaration;
import org.mule.runtime.app.declaration.api.ConstructElementDeclaration;
import org.mule.runtime.app.declaration.api.IdentifiableElementDeclaration;
import org.mule.runtime.app.declaration.api.OperationElementDeclaration;
import org.mule.runtime.app.declaration.api.SourceElementDeclaration;
import org.mule.runtime.app.declaration.api.TopLevelParameterDeclaration;

import java.util.Optional;
import java.util.function.Function;

public class ArtifactHelperUtils {

  private ArtifactHelperUtils() {}

  public static <T extends ParameterizedModel & EnrichableModel> Optional<T> findModel(ExtensionModel extensionModel,
                                                                                       IdentifiableElementDeclaration elementDeclaration) {
    final Function<NamedObject, Boolean> equalsName = (named) -> named.getName().equals(elementDeclaration.getName());

    if (elementDeclaration instanceof TopLevelParameterDeclaration) {
      return empty();
    }

    final Reference<T> foundModel = new Reference<>();
    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        if (equalsName.apply(model) && elementDeclaration instanceof ConfigurationElementDeclaration) {
          foundModel.set((T) model);
          stop();
        }
      }

      @Override
      protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        if (equalsName.apply(model) && elementDeclaration instanceof ConnectionElementDeclaration) {
          foundModel.set((T) model);
          stop();
        }
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        if (equalsName.apply(model) && elementDeclaration instanceof OperationElementDeclaration) {
          foundModel.set((T) model);
          stop();
        }
      }

      @Override
      protected void onConstruct(HasConstructModels owner, ConstructModel model) {
        if (equalsName.apply(model) && elementDeclaration instanceof ConstructElementDeclaration) {
          foundModel.set((T) model);
          stop();
        }
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        if (equalsName.apply(model) && elementDeclaration instanceof SourceElementDeclaration) {
          foundModel.set((T) model);
          stop();
        }
      }
    }.walk(extensionModel);
    return ofNullable(foundModel.get());
  }
}
