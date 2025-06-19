/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import static org.mule.runtime.extension.api.loader.ExtensionDevelopmentFramework.MULE_DSL;

import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.extension.api.loader.ExtensionDevelopmentFramework;
import org.mule.runtime.module.extension.internal.error.ErrorsModelFactory;
import org.mule.runtime.extension.api.loader.parser.ErrorModelParser;
import org.mule.runtime.extension.api.loader.parser.OperationModelParser;
import org.mule.runtime.extension.api.loader.parser.SourceModelParser;

/**
 * Utility methods for {@link ModelLoaderDelegate} implementations
 *
 * @since 4.5.0
 */
public final class ModelLoaderDelegateUtils {

  public ModelLoaderDelegateUtils() {}

  /**
   * @param parser a {@link SourceModelParser}
   * @return whether the given {@code parser} represents a source which requires a config to function
   */
  public static boolean requiresConfig(SourceModelParser parser) {
    return parser.hasConfig() || parser.isConnected();
  }

  /**
   * @param extensionDevelopmentFramework the {@link ExtensionDevelopmentFramework} used for developing the extension being
   *                                      parsed.
   * @param parser                        a {@link OperationModelParser}
   * @return whether the given {@code parser} represents an operation which requires a config to function
   */
  public static boolean requiresConfig(ExtensionDevelopmentFramework extensionDevelopmentFramework, OperationModelParser parser) {
    // For the time being Mule SDK operations are not associated with their own connection provider, they can delegate to
    // connected
    // operations from other extensions by receiving the necessary configs as parameters
    return parser.hasConfig() || (parser.isConnected() && !extensionDevelopmentFramework.equals(MULE_DSL))
        || parser.isAutoPaging();
  }

  /**
   * Adds the {@link ErrorModel}s from the given {@code parser} to the given {@code declarer} and {@code extension}.
   *
   * @param declarer           {@link ComponentDeclarer} to populate with the {@link ErrorModel}s obtained from the
   *                           {@code parser}.
   * @param parser             {@link OperationModelParser} to get the {@link ErrorModel} parsers from.
   * @param extension          {@link ExtensionDeclarer} to populate with the {@link ErrorModel}s obtained from the
   *                           {@code parser}.
   * @param errorsModelFactory factory to generate the {@link ErrorModel}s.
   */
  public static void declareErrorModels(ComponentDeclarer declarer, OperationModelParser parser, ExtensionDeclarer extension,
                                        ErrorsModelFactory errorsModelFactory) {
    for (ErrorModelParser errorModelParser : parser.getErrorModelParsers()) {
      ErrorModel errorModel = errorsModelFactory.getErrorModel(errorModelParser);

      // Only the non-suppressed errors must appear in the component model
      if (!errorModelParser.isSuppressed()) {
        declarer.withErrorModel(errorModel);
      }

      // All the errors from all the components will be declared in the extension, even if they are suppressed. The
      // ErrorTypeRepository is populated with the errors declared in the ExtensionModel, then without changing the API,
      // there is no way to avoid declaring them there.
      extension.withErrorModel(errorModel);
    }
  }
}
