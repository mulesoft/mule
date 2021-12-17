/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java;

import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_SDK_IGNORE_COMPONENT;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_SDK_POLLING_SOURCE_LIMIT;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DISABLE_COMPONENT_IGNORE;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserFactory.getExtensionElement;

import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.privileged.DeclarationEnrichers;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.module.extension.internal.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.module.extension.api.loader.ModelLoaderDelegateFactory;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.api.loader.AbstractExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.delegate.DefaultExtensionModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.validator.ConfigurationModelValidator;
import org.mule.runtime.module.extension.internal.loader.validator.ConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.loader.validator.DeprecationModelValidator;
import org.mule.runtime.module.extension.internal.loader.validator.ParameterPluralNameModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.enricher.DefaultEncodingDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.DynamicMetadataDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.ExtensionDescriptionsEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.JavaConfigurationDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.JavaMimeTypeParametersDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.JavaOAuthDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.JavaObjectStoreParameterDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.ParameterAllowedStereotypesDeclarionEnricher;
import org.mule.runtime.module.extension.internal.loader.java.enricher.ParameterLayoutOrderDeclarationEnricher;
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
import org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserFactory;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

public abstract class AbstractJavaExtensionModelLoader extends AbstractExtensionModelLoader {

  private static final boolean IGNORE_DISABLED = getProperty(DISABLE_SDK_IGNORE_COMPONENT) != null;
  private static final boolean ENABLE_POLLING_SOURCE_LIMIT = getProperty(ENABLE_SDK_POLLING_SOURCE_LIMIT) != null;
  public static final String TYPE_PROPERTY_NAME = "type";

  public static final String VERSION = "version";

  private final List<ExtensionModelValidator> customValidators = unmodifiableList(asList(
                                                                                         new ConfigurationModelValidator(),
                                                                                         new ConnectionProviderModelValidator(),
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
                                                                                         new ParameterPluralNameModelValidator(),
                                                                                         new JavaOAuthConnectionProviderModelValidator(),
                                                                                         new JavaValueProviderModelValidator(),
                                                                                         new JavaSampleDataModelValidator(),
                                                                                         new PrivilegedApiValidator(),
                                                                                         new ComponentLocationModelValidator(),
                                                                                         new InjectedFieldsModelValidator(),
                                                                                         new IgnoredExtensionParameterModelValidator()));

  private final List<DeclarationEnricher> customDeclarationEnrichers = unmodifiableList(asList(
                                                                                               new RefNameDeclarationEnricher(),
                                                                                               new DefaultEncodingDeclarationEnricher(),
                                                                                               new RuntimeVersionDeclarationEnricher(),
                                                                                               // TODO: MOVE TO EXT_API when
                                                                                               // https://www.mulesoft.org/jira/browse/MULE-13070
                                                                                               new JavaMimeTypeParametersDeclarationEnricher(),
                                                                                               new DynamicMetadataDeclarationEnricher(),
                                                                                               new RequiredForMetadataDeclarationEnricher(),
                                                                                               new JavaConfigurationDeclarationEnricher(),
                                                                                               new JavaOAuthDeclarationEnricher(),
                                                                                               new ExtensionDescriptionsEnricher(),
                                                                                               new ValueProvidersParameterDeclarationEnricher(),
                                                                                               new SampleDataDeclarationEnricher(),
                                                                                               new ParameterAllowedStereotypesDeclarionEnricher(),
                                                                                               new ParameterLayoutOrderDeclarationEnricher(),
                                                                                               new JavaObjectStoreParameterDeclarationEnricher(),
                                                                                               new PollingSourceDeclarationEnricher()));

  private final String id;
  private final ModelLoaderDelegateFactory modelLoaderDelegateFactory;

  @Deprecated
  public AbstractJavaExtensionModelLoader(String id, BiFunction<Class<?>, String, ModelLoaderDelegate> delegate) {
    this(id, (ModelLoaderDelegateFactory) (extensionElement, version) -> delegate
        .apply(extensionElement.getDeclaringClass().get(), version));
  }

  @Deprecated
  public AbstractJavaExtensionModelLoader(String id, ModelLoaderDelegateFactory modelLoaderDelegateFactory) {
    this.id = id;
    this.modelLoaderDelegateFactory = modelLoaderDelegateFactory;
  }

  public AbstractJavaExtensionModelLoader(String id) {
    this(id, (ModelLoaderDelegateFactory) (e, v) -> new DefaultExtensionModelLoaderDelegate(v));
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
    context.addCustomDeclarationEnrichers(getPrivilegedDeclarationEnrichers(context));
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
  protected ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context) {
    return new JavaExtensionModelParserFactory();
  }

  protected ModelLoaderDelegate getModelLoaderDelegate(String version) {
    return modelLoaderDelegateFactory.getLoader(null, version);
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
