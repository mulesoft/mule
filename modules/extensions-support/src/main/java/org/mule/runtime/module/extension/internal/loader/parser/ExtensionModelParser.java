/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ExceptionHandlerModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.LicenseModelProperty;

import java.util.List;
import java.util.Optional;

public interface ExtensionModelParser {

  /**
   * @return The Extension's Name
   */
  String getName();

  /**
   * @return The Extension's {@link Category}
   */
  Category getCategory();

  /**
   * @return The Extension's Vendor
   */
  String getVendor();

  List<ConfigurationModelParser> getConfigurationParsers();

  List<OperationModelParser> getOperationModelParsers();

  List<SourceModelParser> getSourceModelParsers();

  List<ConnectionProviderModelParser> getConnectionProviderModelParsers();

  List<FunctionModelParser> getFunctionModelParsers();

  LicenseModelProperty getLicenseModelProperty();

  List<ExternalLibraryModel> getExternalLibraryModels();

  Optional<ExceptionHandlerModelProperty> getExtensionHandlerModelProperty();

  List<ModelProperty> getAdditionalModelProperties();

}
