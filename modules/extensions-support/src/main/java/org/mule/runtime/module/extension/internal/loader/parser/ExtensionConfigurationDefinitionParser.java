package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;

import java.util.List;

public interface ExtensionConfigurationDefinitionParser {

  String getName();

  String getDescription();

  ConfigurationFactoryModelProperty getConfigurationFactoryModelProperty();

  boolean isForceNoExplicit();

  List<ExternalLibraryModel> getExternalLibraryModels();
}
