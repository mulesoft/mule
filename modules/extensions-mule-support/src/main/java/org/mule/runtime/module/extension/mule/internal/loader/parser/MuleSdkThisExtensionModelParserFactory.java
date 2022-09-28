/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.ast.api.ArtifactType.MULE_EXTENSION;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_NAME_PROPERTY_NAME;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkThisExtensionModelMetadataParser;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkExtensionModelMetadataParser;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * {@link ExtensionModelParserFactory} implementation for Mule SDK in the context of its own artifact.
 * <p>
 * Takes two parameters on the {@link ExtensionLoadingContext}:
 * <ul>
 * <li>{@link MULE_SDK_ARTIFACT_AST_PROPERTY_NAME}: The {@link ArtifactAst} of the artifact for which the {@link ExtensionModel}
 * is being loaded.</li>
 * <li>{@link MULE_SDK_EXTENSION_NAME_PROPERTY_NAME}: The name to assign to the resulting extension.</li>
 * </ul>
 *
 * @since 4.5.0
 */
public class MuleSdkThisExtensionModelParserFactory extends BaseMuleSdkExtensionModelParserFactory
    implements ExtensionModelParserFactory {

  @Override
  protected MuleSdkExtensionModelMetadataParser createMetadataParser(ExtensionLoadingContext context) {
    String extensionName = getRequiredLoadingParameter(context, MULE_SDK_EXTENSION_NAME_PROPERTY_NAME);
    return new MuleSdkThisExtensionModelMetadataParser(extensionName);
  }

  @Override
  protected Supplier<Stream<ComponentAst>> createTopLevelComponentsSupplier(ExtensionLoadingContext context) {
    ArtifactAst artifactAst = getRequiredLoadingParameter(context, MULE_SDK_ARTIFACT_AST_PROPERTY_NAME);
    if (artifactAst.getArtifactType().equals(MULE_EXTENSION)) {
      // For Mule SDK Extensions, the top levels are actually the children of a single top level element which represents the
      // root.
      // We don't want to be performing validations at this point, we have other stages for that.
      return () -> artifactAst.topLevelComponentsStream().flatMap(ComponentAst::directChildrenStream);
    }

    return artifactAst::topLevelComponentsStream;
  }
}
