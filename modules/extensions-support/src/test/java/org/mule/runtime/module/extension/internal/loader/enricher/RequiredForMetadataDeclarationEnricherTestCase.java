/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.tck.junit4.matcher.IsEmptyOptional.empty;

import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.property.MetadataImpactModelProperty;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.metadata.extension.MetadataExtension;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class RequiredForMetadataDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private ExtensionDeclaration declaration;

  @Before
  public void setUp() {
    DefaultJavaModelLoaderDelegate loader = new DefaultJavaModelLoaderDelegate(MetadataExtension.class, getProductVersion());
    ExtensionDeclarer declarer =
        loader.declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
    new RequiredForMetadataDeclarationEnricher()
        .enrich(new DefaultExtensionLoadingContext(declarer, this.getClass().getClassLoader(), getDefault(emptySet())));
    declaration = declarer.getDeclaration();
  }

  @Test
  public void connectionProviderWithRequiredForMetadataParameterGetsEnriched() {
    List<ConnectionProviderDeclaration> connectionProviders = declaration.getConnectionProviders();
    ConnectionProviderDeclaration connectionProviderDeclaration = connectionProviders.get(0);

    Optional<MetadataImpactModelProperty> optionalMetadataImpact =
        connectionProviderDeclaration.getModelProperty(MetadataImpactModelProperty.class);
    assertThat(optionalMetadataImpact, is(not(empty())));

    MetadataImpactModelProperty metadataImpact = optionalMetadataImpact.get();
    assertThat(metadataImpact.getRequiredParameters(), is(hasItems("user")));
  }

  @Test
  public void configWithOutRequiredForMetadataParameterDontGetsEnriched() {
    List<ConfigurationDeclaration> configs = declaration.getConfigurations();
    ConfigurationDeclaration configDeclaration = configs.get(0);

    Optional<MetadataImpactModelProperty> optionalMetadataImpact =
        configDeclaration.getModelProperty(MetadataImpactModelProperty.class);
    assertThat(optionalMetadataImpact, is(empty()));
  }
}
