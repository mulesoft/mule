/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserFactory.getExtensionElement;

import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.privileged.DeclarationEnrichers;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.AbstractExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.java.enricher.DefaultEncodingDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.DsqlDynamicMetadataDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.ExtensionDescriptionsEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.JavaConfigurationDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.JavaMimeTypeParametersDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.JavaOAuthDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.JavaObjectStoreParameterDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.ParameterAllowedStereotypesDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.PollingSourceDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.RefNameDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.RequiredForMetadataDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.RuntimeVersionDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.SampleDataDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.ValueProvidersParameterDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.validation.ComponentLocationModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.IgnoredExtensionParameterModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.InjectedFieldsModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.JavaInputParametersTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.JavaOAuthConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.JavaParameterTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.JavaSampleDataModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.JavaScopeModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.JavaSubtypesModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.JavaValueProviderModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.MediaTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.MetadataComponentModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.NullSafeModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.OperationParametersTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.OperationReturnTypeModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.PagedOperationModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.ParameterGroupModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.PojosModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.PrivilegedApiValidator;
import org.mule.runtime.module.extension.internal.loader.java.validation.SourceCallbacksModelValidator;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserFactory;
import org.mule.runtime.module.extension.internal.loader.validator.JavaConfigurationModelValidator;
import org.mule.runtime.module.extension.internal.loader.validator.JavaConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.loader.validator.DeprecationModelValidator;

import java.util.Collection;
import java.util.List;

public abstract class AbstractJavaExtensionModelLoader extends AbstractExtensionModelLoader {

  public static final String TYPE_PROPERTY_NAME = "type";
  public static final String EXTENSION_TYPE = "EXTENSION_TYPE";

  public static final String VERSION = "version";

  private final List<ExtensionModelValidator> customValidators = unmodifiableList(asList(
                                                                                         new JavaConfigurationModelValidator(),
                                                                                         new JavaConnectionProviderModelValidator(),
                                                                                         new PojosModelValidator(),
                                                                                         new DeprecationModelValidator(),
                                                                                         new JavaInputParametersTypeModelValidator(),
                                                                                         new JavaSubtypesModelValidator(),
                                                                                         new MediaTypeModelValidator(),
                                                                                         new MetadataComponentModelValidator(),
                                                                                         new NullSafeModelValidator(),
                                                                                         new OperationReturnTypeModelValidator(),
                                                                                         new OperationParametersTypeModelValidator(),
                                                                                         new SourceCallbacksModelValidator(),
                                                                                         new PagedOperationModelValidator(),
                                                                                         new ParameterGroupModelValidator(),
                                                                                         new JavaParameterTypeModelValidator(),
                                                                                         new JavaOAuthConnectionProviderModelValidator(),
                                                                                         new JavaValueProviderModelValidator(),
                                                                                         new JavaSampleDataModelValidator(),
                                                                                         new PrivilegedApiValidator(),
                                                                                         new ComponentLocationModelValidator(),
                                                                                         new InjectedFieldsModelValidator(),
                                                                                         new IgnoredExtensionParameterModelValidator(),
                                                                                         new JavaScopeModelValidator()));

  private final List<DeclarationEnricher> customDeclarationEnrichers = unmodifiableList(asList(
                                                                                               new RefNameDeclarationEnricher(),
                                                                                               new DefaultEncodingDeclarationEnricher(),
                                                                                               new RuntimeVersionDeclarationEnricher(),
                                                                                               // TODO: MOVE TO EXT_API when
                                                                                               // https://www.mulesoft.org/jira/browse/MULE-13070
                                                                                               new JavaMimeTypeParametersDeclarationEnricher(),
                                                                                               new DsqlDynamicMetadataDeclarationEnricher(),
                                                                                               new RequiredForMetadataDeclarationEnricher(),
                                                                                               new JavaConfigurationDeclarationEnricher(),
                                                                                               new JavaOAuthDeclarationEnricher(),
                                                                                               new ExtensionDescriptionsEnricher(),
                                                                                               new ValueProvidersParameterDeclarationEnricher(),
                                                                                               new SampleDataDeclarationEnricher(),
                                                                                               new ParameterAllowedStereotypesDeclarationEnricher(),
                                                                                               new JavaObjectStoreParameterDeclarationEnricher(),
                                                                                               new PollingSourceDeclarationEnricher()));

  private final String id;

  public AbstractJavaExtensionModelLoader(String id) {
    this.id = id;
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
    super.configureContextBeforeDeclaration(context);

    context.addCustomValidators(customValidators);
    context.addCustomDeclarationEnrichers(customDeclarationEnrichers);
    context.addCustomDeclarationEnrichers(getPrivilegedDeclarationEnrichers(context));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context) {
    return new JavaExtensionModelParserFactory();
  }

  private Collection<DeclarationEnricher> getPrivilegedDeclarationEnrichers(ExtensionLoadingContext context) {
    ExtensionElement extensionType = getExtensionElement(context);
    if (extensionType.getDeclaringClass().isPresent()) {
      try {
        // TODO: MULE-12744. If this call throws an exception it means that the extension cannot access the privileged API.
        ClassLoader extensionClassLoader = context.getExtensionClassLoader();
        Class annotation = extensionClassLoader.loadClass(DeclarationEnrichers.class.getName());


        return (Collection<DeclarationEnricher>) extensionType.getValueFromAnnotation((Class<DeclarationEnrichers>) annotation)
            .map(value -> withContextClassLoader(extensionClassLoader,
                                                 () -> value.getClassArrayValue(DeclarationEnrichers::value)).stream()
                                                     .map(type -> instantiateOrFail(type.getDeclaringClass().get()))
                                                     .collect(toList()))
            .orElse(emptyList());
      } catch (ClassNotFoundException e) {
        // Do nothing
      }
    }
    return emptyList();
  }



  private <R> R instantiateOrFail(Class<R> clazz) {
    try {
      return ClassUtils.instantiateClass(clazz);
    } catch (Exception e) {
      throw new IllegalArgumentException("Error instantiating class: [" + clazz + "].", e);
    }
  }
}
