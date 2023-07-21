/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_SDK_IGNORE_COMPONENT;
import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_SDK_POLLING_SOURCE_LIMIT;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DISABLE_COMPONENT_IGNORE;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.module.extension.internal.loader.delegate.DefaultExtensionModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.delegate.ModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.internal.loader.validator.JavaConfigurationModelValidator;
import org.mule.runtime.module.extension.internal.loader.validator.JavaConnectionProviderModelValidator;
import org.mule.runtime.module.extension.internal.loader.validator.DeprecationModelValidator;
import org.mule.runtime.module.extension.internal.loader.validator.ParameterPluralNameModelValidator;

import java.util.List;
import java.util.Optional;

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

    Optional<Object> disableComponentIgnore = context.getParameter(DISABLE_COMPONENT_IGNORE);
    disableComponentIgnore
        .ifPresent(value -> checkState(value instanceof Boolean,
                                       format("Property value for %s expected to be boolean", DISABLE_COMPONENT_IGNORE)));

    if (IGNORE_DISABLED && !disableComponentIgnore.isPresent()) {
      context.addParameter(DISABLE_COMPONENT_IGNORE, true);
    }
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
    getModelLoaderDelegate(context, version).declare(parserFactory, context);
  }

  protected ModelLoaderDelegate getModelLoaderDelegate(ExtensionLoadingContext context, String version) {
    return new DefaultExtensionModelLoaderDelegate(version);
  }
}
