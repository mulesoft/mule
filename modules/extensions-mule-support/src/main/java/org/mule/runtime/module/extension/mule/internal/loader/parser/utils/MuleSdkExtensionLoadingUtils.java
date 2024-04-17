/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.utils;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.ast.api.model.ExtensionModelHelper.defaultExtensionModelHelper;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXPRESSION_LANGUAGE_METADATA_SERVICE_PROPERTY_NAME;

import static java.lang.String.format;

import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.model.ExtensionModelHelper;
import org.mule.runtime.extension.api.ExtensionConstants;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.type.catalog.DefaultArtifactTypeLoader;

import java.util.NoSuchElementException;

/**
 * Utilities for loading extensions.
 *
 * @since 4.5
 */
public class MuleSdkExtensionLoadingUtils {

  /**
   * Gets a parameter from the given {@link ExtensionLoadingContext} assuming it needs to be present.
   *
   * @param context       The {@link ExtensionLoadingContext}.
   * @param parameterName The parameter name, typically a constant from {@link ExtensionConstants}.
   * @param <T>           The expected type of the parameter.
   *
   * @return The parameter value.
   */
  public static <T> T getRequiredLoadingParameter(ExtensionLoadingContext context, String parameterName) {
    return context.<T>getParameter(parameterName).orElseThrow(() -> createParameterNotFoundException(parameterName));
  }

  /**
   * @param context The {@link ExtensionLoadingContext}
   * @return A {@link TypeLoader} suitable for loading types in the context of the extension being modeled in the current context.
   */
  public static TypeLoader createTypeLoader(ExtensionLoadingContext context) {
    ExpressionLanguageMetadataService expressionLanguageMetadataService =
        getRequiredLoadingParameter(context, MULE_SDK_EXPRESSION_LANGUAGE_METADATA_SERVICE_PROPERTY_NAME);
    DefaultArtifactTypeLoader typeLoader =
        new DefaultArtifactTypeLoader(context.getDslResolvingContext().getExtensions(), expressionLanguageMetadataService);
    try {
      typeLoader.initialise();
    } catch (InitialisationException initialisationException) {
      throw new MuleRuntimeException(createStaticMessage("Failed to initialise type loader."), initialisationException);
    }
    return typeLoader;
  }

  /**
   *
   * @param context The {@link ExtensionLoadingContext}.
   * @return An {@link ExtensionModelHelper} to aid with the lookup of component models by identifier among the extensions in the
   *         context.
   */
  public static ExtensionModelHelper createExtensionModelHelper(ExtensionLoadingContext context) {
    return defaultExtensionModelHelper(context.getDslResolvingContext().getExtensions(),
                                       context.getDslResolvingContext());
  }

  private static RuntimeException createParameterNotFoundException(String parameterName) {
    return new NoSuchElementException(format("Parameter '%s' not found in context", parameterName));
  }

  private MuleSdkExtensionLoadingUtils() {
    // Private constructor to prevent instantiation.
  }
}
