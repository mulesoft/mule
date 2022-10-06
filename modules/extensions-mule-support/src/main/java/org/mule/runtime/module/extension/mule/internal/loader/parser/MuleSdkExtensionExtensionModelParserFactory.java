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
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_RESOURCE_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.module.artifact.activation.internal.ast.validation.AstValidationUtils.handleValidationResult;
import static org.mule.runtime.module.artifact.activation.internal.ast.ArtifactAstUtils.parseArtifactWithExtensionParser;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.ast.MuleSdkExtensionArtifactExtensionModelParser;

import java.util.Set;

import org.slf4j.Logger;

/**
 * {@link ExtensionModelParserFactory} implementation for Mule SDK in the context of extensions.
 *
 * Takes a resource file as parameter of the {@link ExtensionLoadingContext} using {@link MULE_SDK_RESOURCE_PROPERTY_NAME}.
 *
 * @since 4.5.0
 */
public class MuleSdkExtensionExtensionModelParserFactory extends BaseMuleSdkExtensionModelParserFactory
    implements ExtensionModelParserFactory {

  private static final Logger LOGGER = getLogger(MuleSdkExtensionExtensionModelParserFactory.class);

  private ArtifactAst cachedArtifactAst;

  @Override
  public ExtensionModelParser createParser(ExtensionLoadingContext context) {
    return new MuleSdkExtensionExtensionModelParser(getArtifactAst(context), createTypeLoader(context),
                                                    createExtensionModelHelper(context));
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

    String version = getRequiredLoadingParameter(context, VERSION_PROPERTY_NAME);
    String[] resources = {getRequiredLoadingParameter(context, MULE_SDK_RESOURCE_PROPERTY_NAME)};
    ArtifactAst artifactAst = parseArtifactWithExtensionParser(resources,
                                                               this::createAstParser,
                                                               dependencies,
                                                               false,
                                                               context.getExtensionClassLoader(),
                                                               new MuleSdkExtensionArtifactExtensionModelParser(version));

    // Applies the AST validators and throws if there was any error
    handleValidationResult(validatorBuilder().build().validate(artifactAst), LOGGER);

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
