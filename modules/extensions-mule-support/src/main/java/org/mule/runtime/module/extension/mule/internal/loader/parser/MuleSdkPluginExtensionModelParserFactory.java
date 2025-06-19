/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.MuleSdkExtensionLoadingUtils.createExtensionModelHelper;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.MuleSdkExtensionLoadingUtils.createTypeLoader;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.MuleSdkExtensionLoadingUtils.getRequiredLoadingParameter;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParser;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParserFactory;

/**
 * {@link ExtensionModelParserFactory} implementation for Mule SDK in the context of plugins.
 *
 * Takes the artifact's AST as parameter of the {@link ExtensionLoadingContext} using {@link MULE_SDK_ARTIFACT_AST_PROPERTY_NAME}.
 *
 * @since 4.5.0
 */
public class MuleSdkPluginExtensionModelParserFactory implements ExtensionModelParserFactory {

  @Override
  public ExtensionModelParser createParser(ExtensionLoadingContext context) {
    ArtifactAst artifactAst = getRequiredLoadingParameter(context, MULE_SDK_ARTIFACT_AST_PROPERTY_NAME);
    return new MuleSdkPluginExtensionModelParser(artifactAst, createTypeLoader(context),
                                                 createExtensionModelHelper(context));
  }
}
