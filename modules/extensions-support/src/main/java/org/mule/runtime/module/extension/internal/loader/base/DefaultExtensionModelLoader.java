/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.base;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_SDK_IGNORE_COMPONENT;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_SDK_POLLING_SOURCE_LIMIT;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DISABLE_COMPONENT_IGNORE;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;

import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.enricher.BooleanParameterDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.DefaultEncodingDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.DynamicMetadataDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ExtensionDescriptionsEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaConfigurationDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaOAuthDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.MimeTypeParametersDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ObjectStoreParameterDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ParameterAllowedStereotypesDeclarionEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ParameterLayoutOrderDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.PollingSourceDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.RedeliveryPolicyDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.RefNameDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.RequiredForMetadataDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.RuntimeVersionDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.SampleDataDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ValueProvidersParameterDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.validation.ComponentLocationModelValidator;
import org.mule.runtime.module.extension.internal.loader.base.validator.ConfigurationModelValidator;
import org.mule.runtime.module.extension.internal.loader.base.validator.ConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.loader.base.validator.DeprecationModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.IgnoredExtensionParameterModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.InjectedFieldsModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.InputParametersTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.JavaSubtypesModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.MediaTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.MetadataComponentModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.NullSafeModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.OAuthConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.OperationParametersTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.OperationReturnTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.PagedOperationModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.ParameterGroupModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.ParameterPluralNameModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.ParameterTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.PojosModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.PrivilegedApiValidator;
import org.mule.runtime.module.extension.internal.loader.validation.SampleDataModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.SourceCallbacksModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.ValueProviderModelValidator;

import java.util.List;

public class DefaultExtensionModelLoader extends ExtensionModelLoader {

  private static final String DEFAULT_LOADER_ID = "default";

  private static final boolean IGNORE_DISABLED = getProperty(DISABLE_SDK_IGNORE_COMPONENT) != null;
  private static final boolean ENABLE_POLLING_SOURCE_LIMIT = getProperty(ENABLE_SDK_POLLING_SOURCE_LIMIT) != null;

  public static final String VERSION = "version";

//  public static final String TYPE_PROPERTY_NAME = "type";
//  public static final String EXTENSION_TYPE = "EXTENSION_TYPE";
  private final List<ExtensionModelValidator> customValidators = unmodifiableList(asList(
      new ConfigurationModelValidator(),
      new ConnectionProviderModelValidator(),
      new PojosModelValidator(),
      new DeprecationModelValidator(),
      new InputParametersTypeModelValidator(),
      new JavaSubtypesModelValidator(),
      new MediaTypeModelValidator(),
      new MetadataComponentModelValidator(),
      new NullSafeModelValidator(),
      new OperationReturnTypeModelValidator(),
      new OperationParametersTypeModelValidator(),
      new SourceCallbacksModelValidator(),
      new PagedOperationModelValidator(),
      new ParameterGroupModelValidator(),
      new ParameterTypeModelValidator(),
      new ParameterPluralNameModelValidator(),
      new OAuthConnectionProviderModelValidator(),
      new ValueProviderModelValidator(),
      new SampleDataModelValidator(),
      new PrivilegedApiValidator(),
      new ComponentLocationModelValidator(),
      new InjectedFieldsModelValidator(),
      new IgnoredExtensionParameterModelValidator()));

  private final List<DeclarationEnricher> customDeclarationEnrichers = unmodifiableList(asList(
      new BooleanParameterDeclarationEnricher(),
      new RefNameDeclarationEnricher(),
      new DefaultEncodingDeclarationEnricher(),
      new RuntimeVersionDeclarationEnricher(),
      // TODO: MOVE TO EXT_API when
      // https://www.mulesoft.org/jira/browse/MULE-13070
      new MimeTypeParametersDeclarationEnricher(),
      new DynamicMetadataDeclarationEnricher(),
      new RequiredForMetadataDeclarationEnricher(),
      new JavaConfigurationDeclarationEnricher(),
      new JavaOAuthDeclarationEnricher(),
      new RedeliveryPolicyDeclarationEnricher(),
      new ExtensionDescriptionsEnricher(),
      new ValueProvidersParameterDeclarationEnricher(),
      new SampleDataDeclarationEnricher(),
      new ParameterAllowedStereotypesDeclarionEnricher(),
      new ParameterLayoutOrderDeclarationEnricher(),
      new ObjectStoreParameterDeclarationEnricher(),
      new PollingSourceDeclarationEnricher()));

  /**
   * {@inheritDoc}
   */
  @Override
  public String getId() {
    return DEFAULT_LOADER_ID;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    context.addCustomValidators(customValidators);
    context.addCustomDeclarationEnrichers(customDeclarationEnrichers);

    if (IGNORE_DISABLED) {
      context.addParameter(DISABLE_COMPONENT_IGNORE, true);
    }
    if (ENABLE_POLLING_SOURCE_LIMIT) {
      context.addParameter(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, true);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void declareExtension(ExtensionLoadingContext context) {
    ExtensionElement extensionType = getExtensionType(context);
    String version =
        context.<String>getParameter(VERSION).orElseThrow(() -> new IllegalArgumentException("version not specified"));
    factory.getLoader(extensionType, version).declare(context);
  }
}
