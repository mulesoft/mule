/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;

/**
 * Factory for {@link ExtensionModelParser} instances
 *
 * @since 4.5.0
 */
public interface ExtensionModelParserFactory {

  /**
   * @param context the loading context
   * @return a new {@link ExtensionModelParser}
   */
  ExtensionModelParser createParser(ExtensionLoadingContext context);
}
