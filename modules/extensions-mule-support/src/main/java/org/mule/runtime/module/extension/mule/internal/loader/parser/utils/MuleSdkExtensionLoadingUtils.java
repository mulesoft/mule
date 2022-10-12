/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser.utils;

import static java.lang.String.format;

import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.ast.internal.model.ExtensionModelHelper;
import org.mule.runtime.core.api.type.catalog.ApplicationTypeLoader;
import org.mule.runtime.extension.api.ExtensionConstants;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;

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
    return context.<T>getParameter(parameterName)
        .orElseThrow(() -> new NoSuchElementException(format("Parameter '%s' not found in context", parameterName)));
  }

  /**
   * @param context The {@link ExtensionLoadingContext}
   * @return A {@link TypeLoader} suitable for loading types in the context of the extension being modeled in the current context.
   */
  public static TypeLoader createTypeLoader(ExtensionLoadingContext context) {
    return new ApplicationTypeLoader(context.getDslResolvingContext().getExtensions());
  }

  /**
   *
   * @param context The {@link ExtensionLoadingContext}.
   * @return An {@link ExtensionModelHelper} to aid with the lookup of component models by identifier among the extensions in the
   *         context.
   */
  public static ExtensionModelHelper createExtensionModelHelper(ExtensionLoadingContext context) {
    return new ExtensionModelHelper(context.getDslResolvingContext().getExtensions(),
                                    context.getDslResolvingContext());
  }
}
