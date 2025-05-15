/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_SDK_IGNORE_COMPONENT;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_SDK_POLLING_SOURCE_LIMIT;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.internal.util.version.JdkVersionUtils.getJdkVersion;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ADD_ANNOTATIONS_TO_CONFIG_CLASS;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DISABLE_COMPONENT_IGNORE;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;

import static java.lang.String.format;
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
import java.util.Optional;
import java.util.Set;

/**
 * Base implementation for an {@link ExtensionModelLoader}
 *
 * @since 4.5.0
 */
public abstract class AbstractExtensionModelLoader extends ExtensionModelLoader {

  private static boolean IGNORE_DISABLED = getProperty(DISABLE_SDK_IGNORE_COMPONENT) != null;
  private static final boolean ENABLE_POLLING_SOURCE_LIMIT = getProperty(ENABLE_SDK_POLLING_SOURCE_LIMIT) != null;

  private final List<ExtensionModelValidator> validators = unmodifiableList(asList(
                                                                                   new JavaConfigurationModelValidator(),
                                                                                   new JavaConnectionProviderModelValidator(),
                                                                                   new DeprecationModelValidator(),
                                                                                   new ParameterPluralNameModelValidator()));

  /**
   * As part of our effort to remove the powermock dependency for the Java 17 upgrade("W-13116494"), we have temporarily
   * introduced this setter for test use only. We will need to refactor the class so that the field does not have to be static.
   *
   * @param value // TODO: W-13510775 Refactor AbstractExtensionModelLoader to make IGNORE_DISABLED not static
   */
  public static void setIgnoreDisabled(boolean value) {
    IGNORE_DISABLED = value;
  }

  public static boolean getIgnoreDisabled() {
    return IGNORE_DISABLED;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    context.addCustomValidators(validators);

    Optional<Object> disableComponentIgnore = ckeckBoolean(context, DISABLE_COMPONENT_IGNORE);
    if (IGNORE_DISABLED && !disableComponentIgnore.isPresent()) {
      context.addParameter(DISABLE_COMPONENT_IGNORE, true);
    }

    if (ENABLE_POLLING_SOURCE_LIMIT) {
      context.addParameter(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, true);
    }

    Optional<Object> addAnnotationstoConfigClass = ckeckBoolean(context, ADD_ANNOTATIONS_TO_CONFIG_CLASS);
    if (addAnnotationstoConfigClass.isPresent()) {
      context.addParameter(ADD_ANNOTATIONS_TO_CONFIG_CLASS, addAnnotationstoConfigClass.get());
    }
  }

  private Optional<Object> ckeckBoolean(ExtensionLoadingContext context, String paramMame) {
    Optional<Object> addAnnotationstoConfigClass = context.getParameter(paramMame);
    addAnnotationstoConfigClass
        .ifPresent(value -> checkState(value instanceof Boolean,
                                       format("Property value for %s expected to be boolean", paramMame)));
    return addAnnotationstoConfigClass;
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
                                                  runningJdkVersion.getMajor(),
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
