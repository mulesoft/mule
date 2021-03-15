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
    Optional<ConfigAwareModel<T>> foundModel = findConfigAwareModel(extensionModel, elementDeclaration);
    return foundModel.map(ConfigAwareModel::getModel);
  }

  public static <T extends ParameterizedModel & EnrichableModel> Optional<ConfigAwareModel<T>> findConfigAwareModel(ExtensionModel extensionModel,
                                                                                                                    IdentifiableElementDeclaration elementDeclaration) {
    final Function<NamedObject, Boolean> equalsName = (named) -> named.getName().equals(elementDeclaration.getName());

    if (elementDeclaration instanceof TopLevelParameterDeclaration) {
      return empty();
    }

    final Reference<ConfigAwareModel<T>> foundModel = new Reference<>();
    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        if (equalsName.apply(model) && elementDeclaration instanceof ConfigurationElementDeclaration) {
          foundModel.set(configAwareModel(null, model));
          stop();
        }
      }

      @Override
      protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        if (equalsName.apply(model) && elementDeclaration instanceof ConnectionElementDeclaration) {
          foundModel.set(configAwareModel(owner, model));
          stop();
        }
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        if (equalsName.apply(model) && elementDeclaration instanceof OperationElementDeclaration) {
          foundModel.set(configAwareModel(owner, model));
          stop();
        }
      }

      @Override
      protected void onConstruct(HasConstructModels owner, ConstructModel model) {
        if (equalsName.apply(model) && elementDeclaration instanceof ConstructElementDeclaration) {
          foundModel.set(configAwareModel(owner, model));
          stop();
        }
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        if (equalsName.apply(model) && elementDeclaration instanceof SourceElementDeclaration) {
          foundModel.set(configAwareModel(owner, model));
          stop();
        }
      }

      private ConfigAwareModel<T> configAwareModel(Object ownerModel, Object model) {

        // This has to be done because we actually don't have a way to validate classes in compile time.
        // Since there is no relationship between the ElementDeclaration and the Model, we have no way of forcing all
        // generics to comply
        // One case were this used to fail is for example:
        // Optional<ComponentModel> model = findModel(extensionModel, configurationElementDeclaration);
        // Here, T is expected to be a ComponentModel but when the configurationElementDeclaration is found in the ExtensionModel
        // actually references a ConfigurationModel. Hence, the casting fails and we have a runtime error.
        // With this try-catch block, we should return empty() instead of failing.
        T castedModel;
        try {
          castedModel = (T) model;
        } catch (ClassCastException e) {
          return null;
        }

        if (ownerModel instanceof ConfigurationModel) {
          return new ConfigAwareModel<>(castedModel, (ConfigurationModel) ownerModel);
        }
        return new ConfigAwareModel<>(castedModel);
      }

    }.walk(extensionModel);
    return ofNullable(foundModel.get());
  }

  public static class ConfigAwareModel<T extends ParameterizedModel & EnrichableModel> {

    private final T model;
    private final ConfigurationModel configModel;

    private ConfigAwareModel(T model) {
      this(model, null);
    }

    private ConfigAwareModel(T model,
                             ConfigurationModel configModel) {
      this.model = model;
      this.configModel = configModel;
    }

    public T getModel() {
      return this.model;
    }

    public Optional<ConfigurationModel> getConfigModel() {
      return ofNullable(this.configModel);
    }
  }
}
