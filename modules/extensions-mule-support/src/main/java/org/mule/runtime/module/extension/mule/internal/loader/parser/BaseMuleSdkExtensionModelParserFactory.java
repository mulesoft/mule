/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.lang.String.format;

import org.mule.metadata.api.TypeLoader;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.internal.model.ExtensionModelHelper;
import org.mule.runtime.core.api.type.catalog.ApplicationTypeLoader;
import org.mule.runtime.extension.api.ExtensionConstants;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkExtensionModelMetadataParser;

import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Base {@link ExtensionModelParserFactory} implementation for Mule SDK.
 *
 * Implementations need to specify how to extract some parameters needed by the parser from the extension loading context.
 *
 * @since 4.5.0
 */
public abstract class BaseMuleSdkExtensionModelParserFactory implements ExtensionModelParserFactory {

  @Override
  public ExtensionModelParser createParser(ExtensionLoadingContext context) {
    return new MuleSdkExtensionModelParser(createMetadataParser(context),
                                           createTopLevelComponentsSupplier(context),
                                           createTypeLoader(context),
                                           new ExtensionModelHelper(context.getDslResolvingContext().getExtensions(),
                                                                    context.getDslResolvingContext()));
  }

  protected abstract MuleSdkExtensionModelMetadataParser createMetadataParser(ExtensionLoadingContext context);

  // TODO W-11796932: We should be able to remove this one by redesigning the DSL
  protected abstract Supplier<Stream<ComponentAst>> createTopLevelComponentsSupplier(ExtensionLoadingContext context);

  /**
   * Gets a parameter from the given {@link ExtensionLoadingContext} assuming it needs to be present.
   *
   * @param context       The {@link ExtensionLoadingContext}.
   * @param parameterName The parameter name, typically a constant from {@link ExtensionConstants}.
   * @param <T>           The expected type of the parameter.
   *
   * @return The parameter value.
   */
  protected <T> T getMandatoryParameter(ExtensionLoadingContext context, String parameterName) {
    return context.<T>getParameter(parameterName)
        .orElseThrow(() -> new NoSuchElementException(format("Parameter '%s' not found in context", parameterName)));
  }

  /**
   * @param context The {@link ExtensionLoadingContext}
   * @return A {@link TypeLoader} suitable for loading types in the context of the extension being modeled in the current context.
   */
  protected TypeLoader createTypeLoader(ExtensionLoadingContext context) {
    return new ApplicationTypeLoader(context.getDslResolvingContext().getExtensions());
  }
}
