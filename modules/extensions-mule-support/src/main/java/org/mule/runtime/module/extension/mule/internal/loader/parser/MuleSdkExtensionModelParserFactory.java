/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.lang.String.format;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_NAME_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_TYPE_LOADER_PROPERTY_NAME;

import org.mule.runtime.ast.internal.model.ExtensionModelHelper;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;

import java.util.NoSuchElementException;

/**
 * {@link ExtensionModelParserFactory} implementation for Mule SDK
 *
 * @since 4.5.0
 */
public class MuleSdkExtensionModelParserFactory implements ExtensionModelParserFactory {

  @Override
  public ExtensionModelParser createParser(ExtensionLoadingContext context) {
    return new MuleSdkExtensionModelParser(
                                           getProperty(context, MULE_SDK_EXTENSION_NAME_PROPERTY_NAME),
                                           getProperty(context, MULE_SDK_ARTIFACT_AST_PROPERTY_NAME),
                                           getProperty(context, MULE_SDK_TYPE_LOADER_PROPERTY_NAME),
                                           new ExtensionModelHelper(context.getDslResolvingContext().getExtensions(),
                                                                    context.getDslResolvingContext()));
  }

  private <T> T getProperty(ExtensionLoadingContext context, String propertyName) {
    return (T) context.getParameter(propertyName)
        .orElseThrow(() -> new NoSuchElementException(format("Property '%s' not found in context", propertyName)));
  }
}
