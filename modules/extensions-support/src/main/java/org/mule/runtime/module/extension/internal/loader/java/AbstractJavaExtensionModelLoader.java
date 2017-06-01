/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.module.extension.internal.loader.enricher.ClassLoaderDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ConfigNameDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ConnectionDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ExtensionsErrorsDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.DataTypeDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.DisplayDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.DynamicMetadataDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ErrorsDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ExtensionDescriptionsEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ImportedTypesDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaConfigurationDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaExportedTypesDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaOAuthDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaXmlDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ParameterLayoutOrderDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.SubTypesDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.validation.ConfigurationModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.ConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.ExportedTypesModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.JavaSubtypesModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.MetadataComponentModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.NullSafeModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.OAuthConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.OperationParametersTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.OperationReturnTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.ParameterGroupModelValidator;
import org.mule.runtime.module.extension.internal.loader.validation.ParameterTypeModelValidator;

import java.util.List;
import java.util.function.BiFunction;

public class AbstractJavaExtensionModelLoader extends ExtensionModelLoader {

  public static final String TYPE_PROPERTY_NAME = "type";
  public static final String VERSION = "version";

  private final List<ExtensionModelValidator> customValidators = unmodifiableList(asList(
                                                                                         new ConfigurationModelValidator(),
                                                                                         new ConnectionProviderModelValidator(),
                                                                                         new ExportedTypesModelValidator(),
                                                                                         new JavaSubtypesModelValidator(),
                                                                                         new MetadataComponentModelValidator(),
                                                                                         new NullSafeModelValidator(),
                                                                                         new OperationReturnTypeModelValidator(),
                                                                                         new OperationParametersTypeModelValidator(),
                                                                                         new ParameterGroupModelValidator(),
                                                                                         new ParameterTypeModelValidator(),
                                                                                         new OAuthConnectionProviderModelValidator()));

  private final List<DeclarationEnricher> customDeclarationEnrichers = unmodifiableList(asList(
                                                                                               new ClassLoaderDeclarationEnricher(),
                                                                                               new JavaXmlDeclarationEnricher(),
                                                                                               new ConfigNameDeclarationEnricher(),
                                                                                               new ConnectionDeclarationEnricher(),
                                                                                               new ErrorsDeclarationEnricher(),
                                                                                               new ExtensionsErrorsDeclarationEnricher(),
                                                                                               new DataTypeDeclarationEnricher(),
                                                                                               new DisplayDeclarationEnricher(),
                                                                                               new DynamicMetadataDeclarationEnricher(),
                                                                                               new ImportedTypesDeclarationEnricher(),
                                                                                               new JavaConfigurationDeclarationEnricher(),
                                                                                               new JavaExportedTypesDeclarationEnricher(),
                                                                                               new JavaOAuthDeclarationEnricher(),
                                                                                               new SubTypesDeclarationEnricher(),
                                                                                               new ExtensionDescriptionsEnricher(),
                                                                                               new ParameterLayoutOrderDeclarationEnricher()));

  private final String id;
  private final BiFunction<Class<?>, String, ModelLoaderDelegate> delegateFactory;

  public AbstractJavaExtensionModelLoader(String id, BiFunction<Class<?>, String, ModelLoaderDelegate> delegate) {
    this.id = id;
    this.delegateFactory = delegate;
  }

  /**
  
   * {@inheritDoc}
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    context.addCustomValidators(customValidators);
    context.addCustomDeclarationEnrichers(customDeclarationEnrichers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void declareExtension(ExtensionLoadingContext context) {
    Class<?> extensionType = getExtensionType(context);
    String version =
        context.<String>getParameter(VERSION).orElseThrow(() -> new IllegalArgumentException("version not specified"));
    delegateFactory.apply(extensionType, version).declare(context);
  }

  private Class<?> getExtensionType(ExtensionLoadingContext context) {
    String type = context.<String>getParameter(TYPE_PROPERTY_NAME).get();
    if (isBlank(type)) {
      throw new IllegalArgumentException(format("Property '%s' has not been specified", TYPE_PROPERTY_NAME));
    }

    try {
      return loadClass(type, context.getExtensionClassLoader());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(format("Class '%s' cannot be loaded", type), e);
    }
  }
}
