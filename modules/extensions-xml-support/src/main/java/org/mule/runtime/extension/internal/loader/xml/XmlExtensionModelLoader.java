/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.xml;

import static org.mule.runtime.extension.api.ExtensionConstants.XML_SDK_LOADER_ID;
import static org.mule.runtime.extension.api.ExtensionConstants.XML_SDK_RESOURCE_PROPERTY_NAME;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.internal.loader.xml.enricher.StereotypesDiscoveryDeclarationEnricher;
import org.mule.runtime.extension.internal.loader.xml.validator.CorrectPrefixesValidator;
import org.mule.runtime.extension.internal.loader.xml.validator.ForbiddenConfigurationPropertiesValidator;
import org.mule.runtime.extension.internal.loader.xml.validator.GlobalElementNamesValidator;
import org.mule.runtime.extension.internal.loader.xml.validator.InnerConnectionParametersAsConnectionParameters;
import org.mule.runtime.extension.internal.loader.xml.validator.TestConnectionValidator;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link ExtensionModelLoader} for those plugins that have an ID that matches with {@link #DESCRIBER_ID}, which
 * implies that are extensions built through XML.
 *
 * @since 4.0
 */
public class XmlExtensionModelLoader extends ExtensionModelLoader {

  private final List<DeclarationEnricher> customEnrichers =
      unmodifiableList(asList(new StereotypesDiscoveryDeclarationEnricher()));

  private final List<ExtensionModelValidator> customValidators = unmodifiableList(asList(new CorrectPrefixesValidator(),
                                                                                         new GlobalElementNamesValidator(),
                                                                                         new ForbiddenConfigurationPropertiesValidator(),
                                                                                         new TestConnectionValidator(),
                                                                                         new InnerConnectionParametersAsConnectionParameters()));

  /**
   * Attribute to look for in the parametrized attributes picked up from the descriptor.
   */
  public static final String RESOURCE_XML = XML_SDK_RESOURCE_PROPERTY_NAME;

  /**
   * Attribute to look for in the parametrized attributes picked up from the descriptor. If present, with a boolean value,
   * describes whether the XML of the connector should be valid (or not).
   */
  public static final String VALIDATE_XML = "validate-xml";

  /**
   * Attribute to look for in the parametrized attributes picked up from the descriptor. Points to a file which contains the
   * expected {@link MetadataType} of all <operation/>s, which will be used to describe the <output/>'s type. If absent, then it
   * defaults to the
   */
  public static final String RESOURCE_DECLARATION = "resource-declaration";

  /**
   * Attribute to determine a set of files that should be exported by the {@link ExtensionDeclarer}
   */
  public static final String RESOURCES_PATHS = "resources-paths";

  /**
   * The ID which represents {@code this} loader that will be used to execute the lookup when reading the descriptor file.
   *
   * @see MulePluginModel#getExtensionModelLoaderDescriptor()
   */
  public static final String DESCRIBER_ID = XML_SDK_LOADER_ID;

  @Override
  public String getId() {
    return DESCRIBER_ID;
  }

  @Override
  protected void declareExtension(ExtensionLoadingContext context) {
    context.addParameter(DONT_SET_DEFAULT_VALUE_TO_BOOLEAN_PARAMS, true);

    final String modulePath = context.<String>getParameter(RESOURCE_XML)
        .orElseThrow(() -> new IllegalArgumentException(format("The attribute '%s' is missing", RESOURCE_XML)));
    final boolean validateXml = context.<Boolean>getParameter(VALIDATE_XML).orElse(false);
    final Optional<String> declarationPath = context.getParameter(RESOURCE_DECLARATION);
    final List<String> resourcesPaths = context.<List<String>>getParameter(RESOURCES_PATHS).orElse(emptyList());
    final XmlExtensionLoaderDelegate delegate =
        new XmlExtensionLoaderDelegate(modulePath, validateXml, declarationPath, resourcesPaths);
    delegate.declare(context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configureContextBeforeDeclaration(ExtensionLoadingContext context) {
    context.addCustomValidators(customValidators);
    context.addCustomDeclarationEnrichers(customEnrichers);
  }
}
