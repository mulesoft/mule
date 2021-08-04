/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
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
