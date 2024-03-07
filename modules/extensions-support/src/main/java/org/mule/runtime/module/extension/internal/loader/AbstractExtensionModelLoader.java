/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import static org.mule.runtime.api.util.JavaConstants.JAVA_VERSION_8;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_SDK_POLLING_SOURCE_LIMIT;
import static org.mule.runtime.core.internal.util.version.JdkVersionUtils.getJdkVersion;
import static org.mule.runtime.core.internal.util.version.JdkVersionUtils.isJava8;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import org.mule.runtime.core.internal.util.version.JdkVersionUtils.JdkVersion;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.module.extension.internal.loader.delegate.DefaultExtensionModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.internal.loader.validator.DeprecationModelValidator;
import org.mule.runtime.module.extension.internal.loader.validator.JavaConfigurationModelValidator;
import org.mule.runtime.module.extension.internal.loader.validator.JavaConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.loader.validator.ParameterPluralNameModelValidator;
import org.mule.runtime.module.extension.internal.runtime.operation.IllegalSourceException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base implementation for an {@link ExtensionModelLoader}
 *
 * @since 4.5.0
 */
public abstract class AbstractExtensionModelLoader extends ExtensionModelLoader {

  private static final boolean ENABLE_POLLING_SOURCE_LIMIT = getProperty(ENABLE_SDK_POLLING_SOURCE_LIMIT) != null;

  private final List<ExtensionModelValidator> validators = unmodifiableList(asList(
                                                                                   new JavaConfigurationModelValidator(),
                                                                                   new JavaConnectionProviderModelValidator(),
                                                                                   new DeprecationModelValidator(),
                                                                                   new ParameterPluralNameModelValidator()));

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    context.addCustomValidators(validators);

    if (ENABLE_POLLING_SOURCE_LIMIT) {
      context.addParameter(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, true);
    }
  }

  /**
   * @param context the loading context
   * @return the {@link ExtensionModelParserFactory} to be used
   */
  protected abstract ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context);

  /**
   * {@inheritDoc}
   */
  @Override
  protected final void declareExtension(ExtensionLoadingContext context) {
    String version =
        context.<String>getParameter(VERSION_PROPERTY_NAME)
            .orElseThrow(() -> new IllegalArgumentException("version not specified"));

    ExtensionModelParserFactory parserFactory = getExtensionModelParserFactory(context);

    try {
      getModelLoaderDelegate(context, version).declare(parserFactory, context);
    } catch (Exception e) {
      if (e.getCause() != null && e.getCause().getCause() != null && e.getCause().getCause() instanceof NoClassDefFoundError) {
        // Handle errors caused by the java version before the actual validation takes place, since the validation needs the
        // extension model
        NoClassDefFoundError ncdfe = (NoClassDefFoundError) e.getCause().getCause();

        if (ncdfe.getMessage().startsWith("javax/")) {
          JdkVersion runningJdkVersion = getJdkVersion();
          Set<String> supportedJavaVersions = context.getExtensionDeclarer().getDeclaration().getSupportedJavaVersions();
          if (supportedJavaVersions.isEmpty()) {
            supportedJavaVersions = new HashSet<>(asList("11", "1.8"));
          }

          throw new IllegalSourceException(format("Extension '%s' version %s does not support Mule 4.6+ on Java %s. Supported Java versions are: %s. (%s)",
                                                  context.getExtensionDeclarer().getDeclaration().getName(),
                                                  context.getExtensionDeclarer().getDeclaration().getVersion(),
                                                  isJava8(runningJdkVersion) ? JAVA_VERSION_8
                                                      : valueOf(runningJdkVersion.getMajor()),
                                                  supportedJavaVersions,
                                                  ncdfe.toString()));
        }
      }

      throw e;
    }
  }

  protected ModelLoaderDelegate getModelLoaderDelegate(ExtensionLoadingContext context, String version) {
    return new DefaultExtensionModelLoaderDelegate(version);
  }
}
