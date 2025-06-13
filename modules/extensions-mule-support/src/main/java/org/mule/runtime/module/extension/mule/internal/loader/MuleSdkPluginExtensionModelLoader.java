/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.ast.api.ArtifactType.MULE_EXTENSION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.ast.api.xml.AstXmlParser.builder;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXPRESSION_LANGUAGE_METADATA_SERVICE_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_LOADER_ID;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_RESOURCE_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.module.artifact.activation.internal.ast.ArtifactAstUtils.loadConfigResources;
import static org.mule.runtime.module.artifact.activation.internal.ast.ArtifactAstUtils.parseArtifact;
import static org.mule.runtime.module.artifact.activation.internal.ast.validation.AstValidationUtils.logWarningsAndThrowIfContainsErrors;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.MuleSdkExtensionLoadingUtils.getRequiredLoadingParameter;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.loader.AbstractParserBasedExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.artifact.activation.api.ast.AstXmlParserSupplier;
import org.mule.runtime.module.artifact.activation.internal.ast.ArtifactAstUtils;
import org.mule.runtime.module.artifact.activation.internal.ast.MuleSdkExtensionModelLoadingMediator;
import org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkPluginExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.ast.MuleSdkPluginExtensionModelLoadingMediator;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;

/**
 * {@link ExtensionModelLoader} implementation for Mule SDK Plugins.
 *
 * @since 4.5.0
 */
public class MuleSdkPluginExtensionModelLoader extends AbstractParserBasedExtensionModelLoader {

  private static final Logger LOGGER = getLogger(MuleSdkPluginExtensionModelLoader.class);
  private static final String MULE_SDK_EXTENSION_MODEL_PROPERTY_NAME = "_muleSdkArtifactExtensionModel";

  @Override
  public String getId() {
    return MULE_SDK_EXTENSION_LOADER_ID;
  }

  @Override
  protected ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context) {
    return new MuleSdkPluginExtensionModelParserFactory();
  }

  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    super.configureContextBeforeDeclaration(context);

    // Parses the AST if not already available in the loading context.
    Optional<ArtifactAst> ast = context.getParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME);
    if (!ast.isPresent()) {
      // Note that during parsing of the AST, the ExtensionModel will need to be generated in order to perform validations.
      context.addParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME, parseAstAndBuildPluginExtensionModel(context));
    }
  }

  @Override
  protected ExtensionModel doCreate(ExtensionLoadingContext context) {
    // If the ExtensionModel has already been generated, return that, otherwise, do the actual loading now.
    return context.<ExtensionModel>getParameter(MULE_SDK_EXTENSION_MODEL_PROPERTY_NAME).orElseGet(() -> super.doCreate(context));
  }

  private AstXmlParser createAstParser(Set<ExtensionModel> dependencies, boolean disableValidations) {
    AstXmlParser.Builder astBuilder = builder()
        .withArtifactType(MULE_EXTENSION)
        .withExtensionModels(dependencies);

    if (disableValidations) {
      astBuilder.withSchemaValidationsDisabled();
    }

    return astBuilder.build();
  }

  /**
   * Parses the plugin's AST from the resource file.
   * <p>
   * In order to perform AST validations the {@link ExtensionModel} needs to be loaded.
   * <p>
   * As an optimization, the loaded {@link ExtensionModel} will be stored in the context so that we can avoid loading it again
   * later on.
   *
   * @param context the context that will be used for the declaration.
   * @return the plugin's {@link ArtifactAst}.
   * @see ArtifactAstUtils#parseArtifact(String[], AstXmlParserSupplier, Set, boolean, ClassLoader,
   *      MuleSdkExtensionModelLoadingMediator)
   */
  private ArtifactAst parseAstAndBuildPluginExtensionModel(ExtensionLoadingContext context) {
    Set<ExtensionModel> dependencies = context.getDslResolvingContext().getExtensions();

    ExpressionLanguageMetadataService expressionLanguageMetadataService =
        getRequiredLoadingParameter(context, MULE_SDK_EXPRESSION_LANGUAGE_METADATA_SERVICE_PROPERTY_NAME);
    String[] resources = {getRequiredLoadingParameter(context, MULE_SDK_RESOURCE_PROPERTY_NAME)};
    String version = getRequiredLoadingParameter(context, VERSION_PROPERTY_NAME);

    // Registers a callback in case the parser discovers the ExtensionModel as part of the process.
    MuleSdkExtensionModelLoadingMediator loadingHelper =
        new MuleSdkPluginExtensionModelLoadingMediator(expressionLanguageMetadataService,
                                                       context,
                                                       version,
                                                       this,
                                                       em -> context.addParameter(MULE_SDK_EXTENSION_MODEL_PROPERTY_NAME, em));

    try {
      // Parses the full AST of the artifact by providing a helper for loading the ExtensionModel that represents the artifact
      // itself, this is so that schema validations can be performed properly.
      ArtifactAst artifactAst =
          parseArtifact(context.getArtifactCoordinates().map(ArtifactCoordinates::getArtifactId).orElse(null),
                        left(loadConfigResources(resources, context.getExtensionClassLoader())),
                        this::createAstParser,
                        dependencies,
                        false,
                        context.getExtensionClassLoader(),
                        loadingHelper);

      // Applies the AST validators and throws if there was any error
      logWarningsAndThrowIfContainsErrors(validatorBuilder().build().validate(artifactAst), LOGGER);
      return artifactAst;
    } catch (ConfigurationException e) {
      // ExtensionModelParserFactory can't throw checked exceptions, hence the wrapping
      throw new MuleRuntimeException(e);
    }
  }
}
