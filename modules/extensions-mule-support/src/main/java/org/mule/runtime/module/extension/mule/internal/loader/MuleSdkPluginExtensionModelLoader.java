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
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.module.artifact.activation.internal.ast.ArtifactAstUtils.parseArtifact;
import static org.mule.runtime.module.artifact.activation.internal.ast.validation.AstValidationUtils.logWarningsAndThrowIfContainsErrors;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.utils.MuleSdkExtensionLoadingUtils.getRequiredLoadingParameter;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.AbstractExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.MuleSdkPluginExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.ast.MuleSdkPluginExtensionModelLoadingHelper;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;

/**
 * {@link ExtensionModelLoader} implementation for Mule SDK Plugins.
 *
 * @since 4.5.0
 */
public class MuleSdkPluginExtensionModelLoader extends AbstractExtensionModelLoader {

  private static final Logger LOGGER = getLogger(MuleSdkPluginExtensionModelParserFactory.class);
  private static final String MULE_SDK_EXTENSION_MODEL_PROPERTY_NAME = "_muleSdkArtifactExtensionModel";

  @Override
  public String getId() {
    return MULE_SDK_EXTENSION_LOADER_ID;
  }

  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    super.configureContextBeforeDeclaration(context);

    // Parses the AST if not already available in the loading context.
    Optional<ArtifactAst> ast = context.getParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME);
    if (!ast.isPresent()) {
      // Note that during parsing of the AST, the ExtensionModel will need to be generated in order to perform validations.
      context.addParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME, parseAst(context));
    }
  }

  @Override
  protected ExtensionModelParserFactory getExtensionModelParserFactory(ExtensionLoadingContext context) {
    return new MuleSdkPluginExtensionModelParserFactory();
  }

  @Override
  public ExtensionModel loadExtensionModel(ExtensionLoadingContext context, ArtifactCoordinates artifactCoordinates) {
    // The extension model may have been already created and placed in the context as a parameter.
    Optional<ExtensionModel> extensionModel = context.getParameter(MULE_SDK_EXTENSION_MODEL_PROPERTY_NAME);
    return extensionModel.orElseGet(() -> super.loadExtensionModel(context, artifactCoordinates));
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

  private ArtifactAst parseAst(ExtensionLoadingContext context) {
    Set<ExtensionModel> dependencies = context.getDslResolvingContext().getExtensions();

    String version = getRequiredLoadingParameter(context, VERSION_PROPERTY_NAME);
    String[] resources = {getRequiredLoadingParameter(context, MULE_SDK_RESOURCE_PROPERTY_NAME)};

    try {
      // Parses the full AST of the artifact by providing a helper for loading the ExtensionModel that represents the artifact
      // itself, this is so that schema validations can be performed properly.
      // Note also that the helper is receiving a consumer which will take care of recording the loaded ExtensionModel in the
      // current context so that we don't load it again when we finish. The reason we put it in the context and not in an instance
      // field is because we want to keep the loader stateless.
      ArtifactAst artifactAst = parseArtifact(resources,
                                              this::createAstParser,
                                              dependencies,
                                              false,
                                              context.getExtensionClassLoader(),
                                              new MuleSdkPluginExtensionModelLoadingHelper(version,
                                                                                           new ExtensionModelRecorder(context)));

      // Applies the AST validators and throws if there was any error
      logWarningsAndThrowIfContainsErrors(validatorBuilder().build().validate(artifactAst), LOGGER);
      return artifactAst;
    } catch (ConfigurationException e) {
      // ExtensionModelParserFactory can't throw checked exceptions, hence the wrapping
      throw new MuleRuntimeException(e);
    }
  }

  private static class ExtensionModelRecorder implements Consumer<ExtensionModel> {

    private final ExtensionLoadingContext context;

    private ExtensionModelRecorder(ExtensionLoadingContext context) {
      this.context = context;
    }

    public void accept(ExtensionModel extensionModel) {
      context.addParameter(MULE_SDK_EXTENSION_MODEL_PROPERTY_NAME, extensionModel);
    }
  }
}
