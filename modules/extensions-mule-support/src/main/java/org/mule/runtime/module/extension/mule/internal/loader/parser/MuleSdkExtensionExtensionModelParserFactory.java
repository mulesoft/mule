/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.xml.AstXmlParser.builder;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_ARTIFACT_AST_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_EXTENSION_NAME_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.MULE_SDK_RESOURCE_PROPERTY_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.VERSION_PROPERTY_NAME;
import static org.mule.runtime.module.artifact.activation.api.ast.ArtifactAstUtils.parseArtifact;
import static org.mule.runtime.module.extension.mule.api.extension.MuleSdkExtensionExtensionModelDeclarer.EXTENSION_CONSTRUCT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.processor.xml.MuleSdkExtensionDslNamespaceInfoProvider.MULE_EXTENSION_DSL_NAMESPACE;
import static org.mule.runtime.module.extension.mule.internal.dsl.processor.xml.MuleSdkExtensionDslNamespaceInfoProvider.MULE_EXTENSION_DSL_NAMESPACE_URI;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParserFactory;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkApplicationExtensionModelMetadataParser;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkExtensionExtensionModelMetadataParser;
import org.mule.runtime.module.extension.mule.internal.loader.parser.metadata.MuleSdkExtensionModelMetadataParser;

import java.util.List;
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

  private static final ComponentIdentifier EXTENSION_ROOT_IDENTIFIER = ComponentIdentifier.builder()
      .namespace(MULE_EXTENSION_DSL_NAMESPACE)
      .namespaceUri(MULE_EXTENSION_DSL_NAMESPACE_URI)
      .name(EXTENSION_CONSTRUCT_NAME)
      .build();

  private ArtifactAst cachedArtifactAst;

  private static boolean containsExtension(ArtifactAst artifactAst) {
    List<ComponentAst> topLevelComponents = artifactAst.topLevelComponents();
    return topLevelComponents.size() == 1 && topLevelComponents.get(0).getIdentifier().equals(EXTENSION_ROOT_IDENTIFIER);
  }

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
        .map(MuleSdkExtensionExtensionModelParserFactory::containsExtension)
        .orElse(true);
  }

  @Override
  protected MuleSdkExtensionModelMetadataParser createMetadataParser(ExtensionLoadingContext context) {
    Optional<String> extensionName = context.getParameter(MULE_SDK_EXTENSION_NAME_PROPERTY_NAME);
    if (extensionName.isPresent()) {
      return new MuleSdkApplicationExtensionModelMetadataParser(extensionName.get());
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
        .withExtensionModels(dependencies);

    if (disableValidations) {
      astBuilder.withSchemaValidationsDisabled();
    }

    return astBuilder.build();
  }

  private ArtifactAst parseAst(ExtensionLoadingContext context) {
    Set<ExtensionModel> dependencies = context.getDslResolvingContext().getExtensions();

    String[] resources = {getMandatoryParameter(context, MULE_SDK_RESOURCE_PROPERTY_NAME)};
    ArtifactAst artifactAst = parseArtifact(resources,
                                            MuleSdkExtensionExtensionModelParserFactory::containsExtension,
                                            this::createAstParser,
                                            dependencies,
                                            APPLICATION,
                                            false,
                                            context.getExtensionClassLoader(),
                                            "this", // should be irrelevant as long as it doesn't collide
                                            context.getParameter(VERSION_PROPERTY_NAME));

    // TODO: check if we can achieve this using AST validators
    validateContainsExtension(artifactAst);

    return artifactAst;
  }

  private void validateContainsExtension(ArtifactAst artifactAst) {
    List<ComponentAst> topLevelComponents = artifactAst.topLevelComponents();
    if (topLevelComponents.size() != 1) {
      throw new MuleRuntimeException(createStaticMessage("Expected only one top level component"));
    }

    ComponentAst rootComponent = topLevelComponents.get(0);
    if (!rootComponent.getIdentifier().equals(EXTENSION_ROOT_IDENTIFIER)) {
      throw new MuleRuntimeException(createStaticMessage("Expected a single top level component matching identifier [%s], but got: [%s]",
                                                         EXTENSION_ROOT_IDENTIFIER, rootComponent.getIdentifier()));
    }
  }

  private ArtifactAst getArtifactAst(ExtensionLoadingContext context) {
    if (cachedArtifactAst == null) {
      // The AST may be given already parsed. If not, we need to parse it from the resource file.
      cachedArtifactAst =
          context.<ArtifactAst>getParameter(MULE_SDK_ARTIFACT_AST_PROPERTY_NAME).orElseGet(() -> parseAst(context));
    }

    return cachedArtifactAst;
  }
}
