/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion;

import java.util.List;
import java.util.Optional;

/**
 * Parses the syntactic definition of a {@link ParameterModel} so that the semantics reflected in it can be extracted in a uniform
 * way, regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface ParameterModelParser extends SemanticTermsParser, AllowedStereotypesModelParser {

  /**
   * @return the parameter's name
   */
  String getName();

  /**
   * @return the parameter's description
   */
  String getDescription();

  /**
   * @return a {@link MetadataType} describing the parameter's type
   */
  MetadataType getType();

  /**
   * @return whether the parameter is required
   */
  boolean isRequired();

  /**
   * @return the parameter's default value or {@code null} if the parameter is required or no default value provided
   */
  Object getDefaultValue();

  /**
   * @return the parameter's {@link ParameterRole role}
   */
  ParameterRole getRole();

  /**
   * @return the parameter's {@link ExpressionSupport}
   */
  ExpressionSupport getExpressionSupport();

  /**
   * @return the parameter's {@link LayoutModel} if one was defined
   */
  Optional<LayoutModel> getLayoutModel();

  /**
   * @return the parameter's {@link ParameterDslConfiguration} if one was defined
   */
  Optional<ParameterDslConfiguration> getDslConfiguration();

  /**
   * @return whether this parameter should be skipped when generating the extension's connectivity schemas
   */
  boolean isExcludedFromConnectivitySchema();

  /**
   * @return whether this parameter is a config override
   */
  boolean isConfigOverride();

  /**
   * @return whether this parameter acts as the id of the owning component
   */
  boolean isComponentId();

  /**
   * Returns a list with all the {@link ModelProperty model properties} to be applied at the parameter level which are
   * specifically linked to the type of syntax used to define the extension.
   *
   * @return a list with {@link ModelProperty} instances.
   */
  List<ModelProperty> getAdditionalModelProperties();

  /**
   * @return the parameter's {@link DeprecationModel} if one was defined
   */
  Optional<DeprecationModel> getDeprecationModel();

  /**
   * @return the parameter's {@link DisplayModel}
   */
  Optional<DisplayModel> getDisplayModel();

  /**
   * @return the parameter's {@link OAuthParameterModelProperty}
   */
  Optional<OAuthParameterModelProperty> getOAuthParameterModelProperty();

  /**
   * @return a {@link MuleVersion} representing the minimum mule version this component can run on
   */
  Optional<ResolvedMinMuleVersion> getResolvedMinMuleVersion();
}
