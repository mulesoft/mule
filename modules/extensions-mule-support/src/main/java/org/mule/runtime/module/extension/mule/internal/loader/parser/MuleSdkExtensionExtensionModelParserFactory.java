/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.ast.api.ArtifactType.MULE_EXTENSION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.ast.api.xml.AstXmlParser.builder;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_NAME_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_RESOURCE_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.module.artifact.activation.api.ast.ArtifactAstUtils.handleValidationResult;
import static org.mule.runtime.module.artifact.activation.api.ast.ArtifactAstUtils.parseArtifact;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkArtifactLocalExtensionModelMetadataParser;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkExtensionExtensionModelMetadataParser;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkExtensionModelMetadataParser;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * {@link ExtensionModelParserFactory} implementation for Mule SDK in the context of extensions.
 *
 * Takes a resource file as parameter of the {@link ExtensionLoadingContext} using {@link MULE_SDK_RESOURCE_PROPERTY_NAME}.
 *
 * @since 4.5.0
 */
public class MuleSdkExtensionExtensionModelParserFactory extends BaseMuleSdkExtensionModelParserFactory
    implements ExtensionModelParserFactory {

  private ArtifactAst cachedArtifactAst;

  /**
   * @param context The loading context.
   * @return whether this factory is capable of creating a parser for the given {@link ExtensionLoadingContext}.
   */
  public static boolean handles(ExtensionLoadingContext context) {
    // It needs a context containing a resource file.
    if (context.getParameter(MULE_SDK_RESOURCE_PROPERTY_NAME).isPresent()) {
      return true;
    }

    // ...or an already parsed AST that has an extension definition.
    return context.<ArtifactAst>getParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME)
        .map(ast -> ast.getArtifactType().equals(MULE_EXTENSION))
        .orElse(true);
  }

  @Override
  protected MuleSdkExtensionModelMetadataParser createMetadataParser(ExtensionLoadingContext context) {
    // The existence of the name here means that the AST parser is trying to load the artifact-local ExtensionModel in order
    // to complete the AST.
    Optional<String> extensionName = context.getParameter(MULE_SDK_EXTENSION_NAME_PROPERTY_NAME);

    if (extensionName.isPresent()) {
      // In this case, we need to treat is as artifact-local, even when the artifact is an extension.
      return new MuleSdkArtifactLocalExtensionModelMetadataParser(extensionName.get());
    } else {
      return new MuleSdkExtensionExtensionModelMetadataParser(getArtifactAst(context));
    }
  }

  @Override
  protected Supplier<Stream<ComponentAst>> createTopLevelComponentsSupplier(ExtensionLoadingContext context) {
    // At this point we can assume there is only one top level component which is the extension:extension component
    // We don't need to check for this because it should be guaranteed by previous validations
    ComponentAst rootComponent = getArtifactAst(context).topLevelComponents().get(0);
    return rootComponent::directChildrenStream;
  }

  private AstXmlParser createAstParser(Set<ExtensionModel> dependencies, boolean disableValidations) {
    Builder astBuilder = builder()
        .withArtifactType(MULE_EXTENSION)
        .withExtensionModels(dependencies);

    if (disableValidations) {
      astBuilder.withSchemaValidationsDisabled();
    }

    return astBuilder.build();
  }

  private ArtifactAst parseAst(ExtensionLoadingContext context) throws ConfigurationException {
    Set<ExtensionModel> dependencies = context.getDslResolvingContext().getExtensions();

    String[] resources = {getRequiredLoadingParameter(context, MULE_SDK_RESOURCE_PROPERTY_NAME)};
    ArtifactAst artifactAst = parseArtifact(resources,
                                            this::createAstParser,
                                            dependencies,
                                            false,
                                            context.getExtensionClassLoader(),
                                            "this", // should be irrelevant as long as it doesn't collide
                                            context.getParameter(VERSION_PROPERTY_NAME));

    // Applies the AST validators and throws if there was any error
    handleValidationResult(validatorBuilder().build().validate(artifactAst));

    return artifactAst;
  }

  private ArtifactAst parseAstChecked(ExtensionLoadingContext context) {
    // ExtensionModelParserFactory can't throw checked exceptions, hence the wrapping
    try {
      return parseAst(context);
    } catch (ConfigurationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private ArtifactAst getArtifactAst(ExtensionLoadingContext context) {
    if (cachedArtifactAst == null) {
      // The AST may be given already parsed. If not, we need to parse it from the resource file.
      cachedArtifactAst =
          context.<ArtifactAst>getParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME).orElseGet(() -> parseAstChecked(context));
    }

    return cachedArtifactAst;
  }
}
