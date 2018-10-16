/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.privileged.DeclarationEnrichers;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.enricher.BackPressureDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.BooleanParameterDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ClusterSupportEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ConnectionDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.DefaultEncodingDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.DeprecatedModelDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.DisplayDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.DynamicMetadataDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ErrorsDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ExtensionDescriptionsEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ExtensionsErrorsDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ImportedTypesDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaConfigurationDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaExportedTypesDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaOAuthDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaPrivilegedExportedTypesDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaXmlDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.MimeTypeParametersDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.NotificationsDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ObjectStoreParameterDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ParameterAllowedStereotypesDeclarionEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ParameterDslDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ParameterLayoutOrderDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.PollingSourceDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.RedeliveryPolicyDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.RefNameDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.SubTypesDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.ValueProvidersParameterDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.enricher.stereotypes.StereotypesDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ExtensionTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.validation.*;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

public class AbstractJavaExtensionModelLoader extends ExtensionModelLoader {

  public static final String TYPE_PROPERTY_NAME = "type";
  public static final String EXTENSION_TYPE = "EXTENSION_TYPE";
  public static final String VERSION = "version";
  private final List<ExtensionModelValidator> customValidators = unmodifiableList(asList(
                                                                                         new ConfigurationModelValidator(),
                                                                                         new ConnectionProviderModelValidator(),
                                                                                         new DeprecatedModelValidator(),
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
                                                                                         new PrivilegedApiValidator(),
                                                                                         new ComponentLocationModelValidator(),
                                                                                         new InjectedFieldsModelValidator()));

  private final List<DeclarationEnricher> customDeclarationEnrichers = unmodifiableList(asList(
                                                                                               new JavaXmlDeclarationEnricher(),
                                                                                               new BooleanParameterDeclarationEnricher(),
                                                                                               new RefNameDeclarationEnricher(),
                                                                                               new DefaultEncodingDeclarationEnricher(),
                                                                                               new DeprecatedModelDeclarationEnricher(),
                                                                                               new ConnectionDeclarationEnricher(),
                                                                                               new ErrorsDeclarationEnricher(),
                                                                                               new NotificationsDeclarationEnricher(),
                                                                                               new JavaExportedTypesDeclarationEnricher(),
                                                                                               new StereotypesDeclarationEnricher(),
                                                                                               new ExtensionsErrorsDeclarationEnricher(),
                                                                                               //TODO: MOVE TO EXT_API when https://www.mulesoft.org/jira/browse/MULE-13070
                                                                                               new MimeTypeParametersDeclarationEnricher(),
                                                                                               new DisplayDeclarationEnricher(),
                                                                                               new DynamicMetadataDeclarationEnricher(),
                                                                                               new ImportedTypesDeclarationEnricher(),
                                                                                               new JavaConfigurationDeclarationEnricher(),
                                                                                               new JavaPrivilegedExportedTypesDeclarationEnricher(),
                                                                                               new JavaOAuthDeclarationEnricher(),
                                                                                               new RedeliveryPolicyDeclarationEnricher(),
                                                                                               new SubTypesDeclarationEnricher(),
                                                                                               new ExtensionDescriptionsEnricher(),
                                                                                               new ValueProvidersParameterDeclarationEnricher(),
                                                                                               new ParameterAllowedStereotypesDeclarionEnricher(),
                                                                                               new ParameterLayoutOrderDeclarationEnricher(),
                                                                                               new ClusterSupportEnricher(),
                                                                                               new ParameterLayoutOrderDeclarationEnricher(),
                                                                                               new BackPressureDeclarationEnricher(),
                                                                                               new ObjectStoreParameterDeclarationEnricher(),
                                                                                               new PollingSourceDeclarationEnricher(),
                                                                                               new ParameterDslDeclarationEnricher()));

  private final String id;
  private ModelLoaderDelegateFactory factory;

  @Deprecated
  public AbstractJavaExtensionModelLoader(String id, BiFunction<Class<?>, String, ModelLoaderDelegate> delegate) {
    this(id, (ModelLoaderDelegateFactory) (extensionElement, version) -> delegate
        .apply(extensionElement.getDeclaringClass().get(), version));
  }

  public AbstractJavaExtensionModelLoader(String id, ModelLoaderDelegateFactory factory) {
    this.id = id;
    this.factory = factory;
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

  private Collection<DeclarationEnricher> getPrivilegedDeclarationEnrichers(ExtensionLoadingContext context) {
    ExtensionElement extensionType = getExtensionType(context);
    if (extensionType.getDeclaringClass().isPresent()) {
      try {
        // TODO: MULE-12744. If this call throws an exception it means that the extension cannot access the privileged API.
        ClassLoader extensionClassLoader = context.getExtensionClassLoader();
        Class annotation = extensionClassLoader.loadClass(DeclarationEnrichers.class.getName());
        return (Collection<DeclarationEnricher>) extensionType.getAnnotation((Class<DeclarationEnrichers>) annotation)
            .map(enrichers -> withContextClassLoader(extensionClassLoader,
                                                     () -> stream(enrichers.value())
                                                         .map(this::instantiateOrFail)
                                                         .collect(toList())))
            .orElse(emptyList());
      } catch (ClassNotFoundException e) {
        // Do nothing
      }
    }
    return emptyList();
  }

  private ExtensionElement getExtensionType(ExtensionLoadingContext context) {
    return context.<ExtensionElement>getParameter(EXTENSION_TYPE).orElseGet(() -> {
      String type = (String) context.getParameter("type").get();
      try {
        ClassLoader extensionClassLoader = context.getExtensionClassLoader();
        return new ExtensionTypeWrapper<>(loadClass(type, extensionClassLoader),
                                          new DefaultExtensionsTypeLoaderFactory().createTypeLoader(extensionClassLoader));
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(format("Class '%s' cannot be loaded", type), e);
      }
    });
  }

  private <R> R instantiateOrFail(Class<R> clazz) {
    try {
      return ClassUtils.instantiateClass(clazz);
    } catch (Exception e) {
      throw new IllegalArgumentException("Error instantiating class: [" + clazz + "].", e);
    }
  }
}
