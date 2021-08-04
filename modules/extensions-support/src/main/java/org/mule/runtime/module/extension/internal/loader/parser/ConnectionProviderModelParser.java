package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;

import java.util.List;
import java.util.Optional;

public interface ConnectionProviderModelParser {

  String getName();

  String getDescription();

  List<ParameterGroupModelParser> getParameterGroupModelParsers();

  List<ExternalLibraryModel> getExternalLibraryModels();

  ConnectionManagementType getConnectionManagementType();

  ConnectionProviderFactoryModelProperty getConnectionProviderFactoryModelProperty();

  boolean supportsConnectivityTesting();

  boolean isExcludedFromConnectivitySchema();

  Optional<OAuthModelProperty> getOAuthModelProperty();

  List<ModelProperty> getAdditionalModelProperties();
}
