/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.utils;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.app.declaration.api.GlobalElementDeclarationVisitor;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.List;
import java.util.Optional;

public class ArtifactHelper {

  private ExtensionManager extensionManager;
  private ArtifactDeclaration artifactDeclaration;
  private ConfigurationComponentLocator componentLocator;

  public ArtifactHelper(ExtensionManager extensionManager,
                        ConfigurationComponentLocator componentLocator,
                        ArtifactDeclaration artifactDeclaration) {
    this.extensionManager = extensionManager;
    this.componentLocator = componentLocator;
    this.artifactDeclaration = artifactDeclaration;
  }

  public ExtensionModel getExtensionModel(ElementDeclaration declaration) {
    return findExtension(declaration)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("There is no extensionModel for extension: %s",
                                                                        declaration.getDeclaringExtension())));
  }

  public Optional<ExtensionModel> findExtension(ElementDeclaration declaration) {
    return extensionManager.getExtension(declaration.getDeclaringExtension());
  }

  public <T> Class<T> getParameterClass(ParameterModel parameterModel, ElementDeclaration containerDeclaration) {
    return getType(parameterModel.getType(), getClassLoader(getExtensionModel(containerDeclaration)));
  }

  public <T extends ParameterizedModel & EnrichableModel> Optional<T> findModel(ExtensionModel extensionModel,
                                                                                ElementDeclaration elementDeclaration) {
    return ArtifactHelperUtils.findModel(extensionModel, elementDeclaration);
  }

  public Optional<? extends ComponentModel> findComponentModel(ExtensionModel extensionModel,
                                                               ComponentElementDeclaration<?> componentDeclaration) {
    return findModel(extensionModel, componentDeclaration).filter(m -> m instanceof ComponentModel).map(m -> (ComponentModel) m);
  }

  public boolean hasParameterOfType(ComponentModel componentModel,
                                    StereotypeModel referenceStereotype) {
    return componentModel.getAllParameterModels()
        .stream()
        .filter(paramModel -> paramModel.getAllowedStereotypes()
            .stream()
            .anyMatch(allowed -> allowed.isAssignableTo(referenceStereotype)))
        .findAny().isPresent();
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

  private Optional<ConfigurationProvider> findConfigurationProvider(String configName) {
    return findConfigurationDeclaration(configName)
        .map(ced -> Location.builder().globalName(ced.getRefName()).build())
        .flatMap(cloc -> componentLocator.find(cloc))
        .filter(cp -> cp instanceof ConfigurationProvider)
        .map(cp -> (ConfigurationProvider) cp);
  }

  public Optional<ConfigurationInstance> getConfigurationInstance(String configName) {
    return findConfigurationProvider(configName).map(cp -> {
      CoreEvent fakeEvent = getNullEvent();
      try {
        return cp.get(fakeEvent);
      } finally {
        if (fakeEvent != null) {
          ((BaseEventContext) fakeEvent.getContext()).success();
        }
      }
    });
  }

  public List<String> getExtensions() {
    return extensionManager.getExtensions().stream().map(extensionModel -> extensionModel.getName()).collect(toList());
  }

}
