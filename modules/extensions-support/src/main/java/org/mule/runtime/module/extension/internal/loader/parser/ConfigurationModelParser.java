/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;

import java.util.List;
import java.util.Optional;

/**
 * Parses the syntactic definition of a {@link ConfigurationModel} so that the semantics reflected in it can be extracted in a
 * uniform way, regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface ConfigurationModelParser extends StereotypeModelParser, AdditionalPropertiesModelParser {

  /**
   * @return the configuration's name
   */
  String getName();

  /**
   * @return the configuration's description
   */
  String getDescription();

  /**
   * @return the {@link ConfigurationFactoryModelProperty} used to create instances of the configuration
   */
  ConfigurationFactoryModelProperty getConfigurationFactoryModelProperty();

  /**
   * @return whether the configuration should be force to never support implicit definitions
   */
  boolean isForceNoImplicit();

  /**
   * @return a list with an {@link ExternalLibraryModel} per each external library defined at the configuration level.
   */
  List<ExternalLibraryModel> getExternalLibraryModels();

  /**
   * Returns a list with a {@link ParameterGroupModelParser} per each parameter group defined in the configuration. Each group is
   * listed in the same order as defined in the syntax.
   *
   * @return a list with the config's {@link ParameterGroupModelParser}
   */
  List<ParameterGroupModelParser> getParameterGroupParsers();

  /**
   * Returns a list with a {@link OperationModelParser} per each operation defined in the configuration. Each operation is listed
   * in the same order as defined in the syntax.
   *
   * @return a list with the config's {@link OperationModelParser}
   */
  List<OperationModelParser> getOperationParsers();

  /**
   * Returns a list with a {@link SourceModelParser} per each source defined in the configuration. Each source is listed in the
   * same order as defined in the syntax.
   *
   * @return a list with the config's {@link SourceModelParser}
   */
  List<SourceModelParser> getSourceModelParsers();

  /**
   * Returns a list with a {@link ConnectionProviderModelParser} per each connection provider defined in the configuration. Each
   * provider is listed in the same order as defined in the syntax.
   *
   * @return a list with the config's {@link OperationModelParser}
   */
  List<ConnectionProviderModelParser> getConnectionProviderModelParsers();

  /**
   * Returns a list with a {@link FunctionModelParser} per each function defined in the configuration. Each function is listed in
   * the same order as defined in the syntax.
   *
   * @return a list with the config's {@link FunctionModelParser}
   */
  List<FunctionModelParser> getFunctionModelParsers();

  /**
   * @return the configuration's {@link DeprecationModel} if one was defined
   */
  Optional<DeprecationModel> getDeprecationModel();

  /**
   * @return a {@link MuleVersion} representing the minimum mule version this component can run on
   */
  Optional<MuleVersion> getMinMuleVersion();
}
