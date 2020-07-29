/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.utils;

import static java.util.Optional.ofNullable;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.connection.HasConnectionProviderModels;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.GlobalElementDeclarationVisitor;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;

import java.util.Objects;
import java.util.Optional;

public class ArtifactHelper {

  private ExtensionManager extensionManager;
  private ArtifactDeclaration artifactDeclaration;
  private ConfigurationComponentLocator componentLocator;

  public ArtifactHelper(
                        ExtensionManager extensionManager,
                        ConfigurationComponentLocator componentLocator,
                        ArtifactDeclaration artifactDeclaration) {
    this.extensionManager = extensionManager;
    this.componentLocator = componentLocator;
    this.artifactDeclaration = artifactDeclaration;
  }

  public ExtensionModel getExtensionModel(ElementDeclaration declaration) {
    return extensionManager
        .getExtension(declaration.getDeclaringExtension())
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not get extension model for: %s",
                                                                        declaration.getDeclaringExtension())));
  }

  public <T> Class<T> getParameterClass(ParameterModel parameterModel, ElementDeclaration containerDeclaration) {
    return getType(parameterModel.getType(), getClassLoader(getExtensionModel(containerDeclaration)));

  }

  public <T extends ParameterizedModel & EnrichableModel> Optional<T> findModel(ElementDeclaration elementDeclaration) {
    final ExtensionModel extensionModel = getExtensionModel(elementDeclaration);
    final String componentName = elementDeclaration.getName();
    final Reference<T> foundModel = new Reference<>();
    new ExtensionWalker() {

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        setAndStop(model);
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        setAndStop(model);
      }

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        setAndStop(model);
      }

      @Override
      protected void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
        setAndStop(model);
      }

      private void setAndStop(ParameterizedModel model) {
        if (Objects.equals(model.getName(), componentName)) {
          foundModel.set((T) model);
          stop();
        }
      }
    }.walk(extensionModel);
    return ofNullable(foundModel.get());
  }

  public Optional<ConfigurationElementDeclaration> findConfigurationDeclaration(String configName) {
    final Reference<ConfigurationElementDeclaration> configDeclaration = new Reference<>();
    final GlobalElementDeclarationVisitor visitor = new GlobalElementDeclarationVisitor() {

      @Override
      public void visit(ConfigurationElementDeclaration declaration) {
        if (declaration.getRefName().equals(configName)) {
          configDeclaration.set(declaration);
        }
      }
    };
    artifactDeclaration.getGlobalElements().forEach(gld -> gld.accept(visitor));
    return ofNullable(configDeclaration.get());
  }

  public Optional<ConfigurationProvider> findConfigurationProvider(String configName) {
    return findConfigurationDeclaration(configName)
        .map(ced -> Location.builder().globalName(ced.getRefName()).build())
        .flatMap(cloc -> componentLocator.find(cloc))
        .filter(cp -> cp instanceof ConfigurationProvider)
        .map(cp -> (ConfigurationProvider) cp);
  }

  public Optional<ConnectionProvider> findConnectionProvider(String configName) {
    return findConfigurationDeclaration(configName)
        .map(ced -> Location.builder().globalName(ced.getRefName()).addConnectionPart().build())
        .flatMap(conloc -> componentLocator.find(conloc))
        .filter(c -> c instanceof ConnectionProviderResolver)
        .map(cpr -> (ConnectionProviderResolver) cpr)
        .map(cpr -> handlingException(
                                      () -> ((ConnectionProvider) cpr
                                          .resolve(null)
                                          .getFirst())));
  }

  public Optional<ConfigurationInstance> getConfigurationInstance(String configName) {
    return findConfigurationProvider(configName).map(cp -> cp.get(getNullEvent()));
  }

  public Optional<Object> getConnectionInstance(String configName) {
    return findConnectionProvider(configName).map(c -> handlingException((CheckedSupplier<Object>) c::connect));
  }

  private <T> T handlingException(CheckedSupplier<T> supplier, String... errorMessage) {
    try {
      return supplier.get();
    } catch (RuntimeException e) {
      if (errorMessage.length > 0) {
        throw new MuleRuntimeException(createStaticMessage(errorMessage[0]), e);
      }
      throw e;
    }
  }


}
