/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader;

import static org.mule.runtime.ast.api.ArtifactType.MULE_EXTENSION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.ast.api.xml.AstXmlParser.builder;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_LOADER_ID;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_RESOURCE_PROPERTY_NAME;
import static org.mule.runtime.module.artifact.activation.internal.ast.ArtifactAstUtils.parseArtifact;
import static org.mule.runtime.module.artifact.activation.internal.ast.validation.AstValidationUtils.logWarningsAndThrowIfContainsErrors;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.MuleSdkExtensionLoadingUtils.getRequiredLoadingParameter;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.module.artifact.activation.internal.ast.MuleSdkExtensionModelLoadingHelper;
import org.mule.runtime.module.extension.internal.loader.AbstractExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkPluginExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.ast.MuleSdkPluginExtensionModelLoadingHelper;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;

/**
 * {@link ExtensionModelLoader} implementation for Mule SDK Plugins.
 *
 * @since 4.5.0
 */
public class MuleSdkPluginExtensionModelLoader extends AbstractExtensionModelLoader {

  private static final Logger LOGGER = getLogger(MuleSdkPluginExtensionModelParserFactory.class);

  @Override
  public String getId() {
    return MULE_SDK_EXTENSION_LOADER_ID;
  }

  @Override
  protected ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context) {
    return new MuleSdkPluginExtensionModelParserFactory();
  }

  @Override
  public ExtensionModel loadExtensionModel(ExtensionModelLoadingRequest request) {
    Optional<ExtensionModel> extensionModel = Optional.empty();

    // Parses the AST if not already available in the loading context.
    ArtifactAst ast = (ArtifactAst) request.getParameters().get(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME);
    if (ast == null) {
      // Note that during parsing of the AST, the ExtensionModel will need to be generated in order to perform validations.
      extensionModel = parseAstAndBuildPluginExtensionModel(request);
    }

    // If the ExtensionModel has already been generated, return that, otherwise, do the actual loading now.
    return extensionModel.orElseGet(() -> super.loadExtensionModel(request));
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

  private Optional<ExtensionModel> parseAstAndBuildPluginExtensionModel(ExtensionModelLoadingRequest request) {
    Set<ExtensionModel> dependencies = request.getDslResolvingContext().getExtensions();

    String[] resources = {getRequiredLoadingParameter(request, MULE_SDK_RESOURCE_PROPERTY_NAME)};
    MuleSdkExtensionModelLoadingHelper loadingHelper =
        new MuleSdkPluginExtensionModelLoadingHelper(request.getArtifactCoordinates());

    try {
      // Parses the full AST of the artifact by providing a helper for loading the ExtensionModel that represents the artifact
      // itself, this is so that schema validations can be performed properly.
      ArtifactAst artifactAst = parseArtifact(resources,
                                              this::createAstParser,
                                              dependencies,
                                              false,
                                              request.getExtensionClassLoader(),
                                              loadingHelper);

      // Applies the AST validators and throws if there was any error
      logWarningsAndThrowIfContainsErrors(validatorBuilder().build().validate(artifactAst), LOGGER);
      return loadingHelper.getExtensionModel();
    } catch (ConfigurationException e) {
      // ExtensionModelParserFactory can't throw checked exceptions, hence the wrapping
      throw new MuleRuntimeException(e);
    }
  }
}
