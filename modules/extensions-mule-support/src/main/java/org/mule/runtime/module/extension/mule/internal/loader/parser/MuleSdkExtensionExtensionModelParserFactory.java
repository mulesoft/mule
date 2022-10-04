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
import static org.mule.runtime.module.artifact.activation.internal.ast.validation.ValidationUtils.handleValidationResult;
import static org.mule.runtime.module.artifact.activation.internal.ast.ArtifactAstUtils.parseArtifactWithExtensionsEnricher;

import static java.util.Collections.singletonMap;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkExtensionExtensionModelMetadataParser;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkExtensionModelMetadataParser;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
  private static final String MULE_SDK_EXTENSION_MODEL_PARSER_FACTORY_PROPERTY_NAME = "muleParserFactory";

  /**
   * @param context the loading context
   * @return a {@link ExtensionModelParserFactory} instance (which may be cached in the {@code context}).
   */
  public static ExtensionModelParserFactory create(ExtensionLoadingContext context) {
    // The parser factory could be shared by multiple loading requests that are part of the same context.
    Optional<ExtensionModelParserFactory> parserFactory =
        context.getParameter(MULE_SDK_EXTENSION_MODEL_PARSER_FACTORY_PROPERTY_NAME);
    return parserFactory.orElseGet(MuleSdkExtensionExtensionModelParserFactory::new);
  }

  // For performance reasons we will be sharing the parser factory instance for the two-step model loading.
  // We use these fields to cache the artifact's AST and the resulting parser, so we don't create them more than once.
  private ArtifactAst cachedArtifactAst;
  private ExtensionModelParser cachedExtensionModelParser;

  @Override
  public ExtensionModelParser createParser(ExtensionLoadingContext context) {
    this.cachedArtifactAst = getArtifactAst(context);

    // As part of getting the artifact's AST, it is possible that we have already created the parser. We don't want to do it
    // again.
    if (cachedExtensionModelParser == null) {
      cachedExtensionModelParser = super.createParser(context);
    }
    return cachedExtensionModelParser;
  }

  @Override
  protected MuleSdkExtensionModelMetadataParser createMetadataParser(ExtensionLoadingContext context) {
    return new MuleSdkExtensionExtensionModelMetadataParser(cachedArtifactAst);
  }

  @Override
  protected Supplier<Stream<ComponentAst>> createTopLevelComponentsSupplier(ExtensionLoadingContext context) {
    // At this point we can assume there is only one top level component which is the extension:extension component
    // We don't need to check for this because it should be guaranteed by previous validations
    ComponentAst rootComponent = cachedArtifactAst.topLevelComponents().get(0);
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

    String version = getRequiredLoadingParameter(context, VERSION_PROPERTY_NAME);
    String[] resources = {getRequiredLoadingParameter(context, MULE_SDK_RESOURCE_PROPERTY_NAME)};
    ArtifactAst artifactAst = parseArtifactWithExtensionsEnricher(resources,
                                                                  this::createAstParser,
                                                                  dependencies,
                                                                  false,
                                                                  context.getExtensionClassLoader(),
                                                                  new MuleSdkLocalExtensionModelsEnricher(version,
                                                                                                          getLoadingRequestExtraParameters()));

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
    // The AST may be given already parsed. If not, we need to parse it from the resource file.
    return context.<ArtifactAst>getParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME).orElseGet(() -> parseAstChecked(context));
  }

  private Map<String, Object> getLoadingRequestExtraParameters() {
    return singletonMap(MULE_SDK_EXTENSION_MODEL_PARSER_FACTORY_PROPERTY_NAME, this);
  }
}
