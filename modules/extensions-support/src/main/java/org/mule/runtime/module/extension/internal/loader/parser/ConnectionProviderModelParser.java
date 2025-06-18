/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion;

import java.util.List;
import java.util.Optional;

/**
 * Parses the syntactic definition of a {@link ConnectionProviderModel} so that the semantics reflected in it can be extracted in
 * a uniform way, regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface ConnectionProviderModelParser extends SemanticTermsParser, StereotypeModelParser {

  /**
   * @return the provider's name
   */
  String getName();

  /**
   * @return the provider's description
   */
  String getDescription();

  /**
   * Returns a list with a {@link ParameterGroupModelParser} per each parameter group defined in the provider. Each group is
   * listed in the same order as defined in the syntax.
   *
   * @return a list with the config's {@link ParameterGroupModelParser}
   */
  List<ParameterGroupModelParser> getParameterGroupModelParsers();

  /**
   * @return a list with an {@link ExternalLibraryModel} per each external library defined at the configuration level.
   */
  List<ExternalLibraryModel> getExternalLibraryModels();

  /**
   * @return the {@link ConnectionManagementType} used by this provider
   */
  ConnectionManagementType getConnectionManagementType();

  /**
   * @return the {@link ConnectionProviderFactoryModelProperty} used to create instances of this provider
   */
  Optional<ConnectionProviderFactoryModelProperty> getConnectionProviderFactoryModelProperty();

  /**
   * @return whether this provider supports connectivity testing or not.
   */
  boolean supportsConnectivityTesting();

  /**
   * @return whether this provider connections may participate in an XA transaction.
   * @since 4.10
   */
  boolean supportsXa();

  /**
   * @return whether this provider should be skipped when generating the extension's connectivity schemas
   */
  boolean isExcludedFromConnectivitySchema();

  /**
   * @return if the provider supports OAuth, returns an {@link OAuthModelProperty} which describes its grant types
   */
  Optional<OAuthModelProperty> getOAuthModelProperty();

  /**
   * Returns a list with all the {@link ModelProperty model properties} to be applied at the connection provider level which are
   * specifically linked to the type of syntax used to define the extension.
   *
   * @return a list with {@link ModelProperty} instances.
   */
  List<ModelProperty> getAdditionalModelProperties();

  /**
   * @return the connection provider's {@link DeprecationModel} if one was defined
   */
  Optional<DeprecationModel> getDeprecationModel();

  /**
   * @return the connection provider's {@link DisplayModel}
   */
  Optional<DisplayModel> getDisplayModel();

  /**
   * @return a {@link ResolvedMinMuleVersion} that contains the minimum mule version this component can run on and the reason why
   *         that version was assigned.
   */
  Optional<ResolvedMinMuleVersion> getResolvedMinMuleVersion();

}
