/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.crafted.localisation.properties.extension;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.test.crafted.localisation.properties.extension.TestLocalisationPropertiesExtensionLoadingDelegate.EXTENSION_NAME;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.properties.api.ConfigurationPropertiesProviderFactory;
import org.mule.runtime.properties.api.ResourceProvider;

import java.util.function.UnaryOperator;

public class LocalisationConfigurationPropertiesProviderFactory implements ConfigurationPropertiesProviderFactory {

  public static final String LOCALISATION_CONFIGURATION_PROPERTIES_ELEMENT = "localisation-configuration-properties-config";
  public static final ComponentIdentifier LOCALISATION_CONFIGURATION_PROPERTIES =
      builder().namespace(EXTENSION_NAME).name(LOCALISATION_CONFIGURATION_PROPERTIES_ELEMENT).build();

  @Override
  public ComponentIdentifier getSupportedComponentIdentifier() {
    return LOCALISATION_CONFIGURATION_PROPERTIES;
  }

  @Override
  public LocalisationConfigurationPropertiesProvider createProvider(ComponentAst providerElementDeclaration,
                                                                    UnaryOperator<String> localResolver,
                                                                    ResourceProvider externalResourceProvider) {
    String file = providerElementDeclaration.getParameter("file").getResolvedRawValue();
    requireNonNull(file, "Required attribute 'file' of 'locale-configuration-properties' not found");

    ComponentIdentifier languageComponentIdentifier =
        ComponentIdentifier.builder().namespace(EXTENSION_NAME).name("language").build();

    ComponentAst language = providerElementDeclaration
        .directChildrenStream()
        .filter(c -> c.getIdentifier().equals(languageComponentIdentifier))
        .findFirst()
        .get();

    String locale = language.getRawParameterValue("locale").orElseThrow(() -> new RuntimeException("A locale must be specified"));

    return new LocalisationConfigurationPropertiesProvider(externalResourceProvider, file, locale);
  }

}
