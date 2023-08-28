/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
