package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;

import java.util.List;

public interface ConfigurationModelParser {

  String getName();

  String getDescription();

  ConfigurationFactoryModelProperty getConfigurationFactoryModelProperty();

  boolean isForceNoExplicit();

  List<ExternalLibraryModel> getExternalLibraryModels();

  List<ParameterGroupModelParser> getParameterGroupParsers();

  List<OperationModelParser> getOperationParsers();

  List<ModelProperty> getAdditionalModelProperties();
}
