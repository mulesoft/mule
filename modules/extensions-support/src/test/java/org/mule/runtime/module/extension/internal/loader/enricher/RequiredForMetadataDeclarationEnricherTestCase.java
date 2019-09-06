/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.StereotypedDeclaration;
import org.mule.runtime.extension.api.annotation.metadata.RequiredForMetadata;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;

@RunWith(Parameterized.class)
public class RequiredForMetadataDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private static final String TESTING_CONNECTION_NAME = "CONNECTION";
  private static final String TESTING_CONFIGURATION_NAME = "CONFIGURATION";

  private static final String REQUIRED_PARAM = "RequiredParam";
  private static final String OTHER_REQUIRED_PARAM = "OtherRequiredParam";

  private static final RequiredForMetadataDeclarationEnricher ENRICHER = new RequiredForMetadataDeclarationEnricher();

  private ArgumentCaptor<RequiredForMetadataModelProperty> argumentCaptor = forClass(RequiredForMetadataModelProperty.class);

  @Parameterized.Parameter
  public String name;


  private StereotypedDeclaration stereotypedDeclaration;


  private ExtensionDeclaration declaration;
  private ExtensionLoadingContext loadingContext;
  private ConnectionProviderDeclaration connectionProviderDeclaration;
  private ConfigurationDeclaration configurationDeclaration;
  private ParameterDeclaration parameterDeclaration;
  private ParameterDeclaration otherParameterDeclaration;
  private ExtensionParameter extensionParameter;
  private ExtensionParameter otherExtensionParameter;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[] {TESTING_CONNECTION_NAME},
                  new Object[] {TESTING_CONFIGURATION_NAME});
  }


  @Before
  public void setUp() {
    declaration = mock(ExtensionDeclaration.class);
    loadingContext = mock(ExtensionLoadingContext.class, RETURNS_DEEP_STUBS);
    connectionProviderDeclaration = mock(ConnectionProviderDeclaration.class);
    configurationDeclaration = mock(ConfigurationDeclaration.class);
    parameterDeclaration = mock(ParameterDeclaration.class);
    otherParameterDeclaration = mock(ParameterDeclaration.class);
    extensionParameter = mock(ExtensionParameter.class);
    otherExtensionParameter = mock(ExtensionParameter.class);

    when(loadingContext.getExtensionDeclarer().getDeclaration()).thenReturn(declaration);
    when(declaration.getConnectionProviders()).thenReturn(singletonList(connectionProviderDeclaration));
    when(declaration.getConfigurations()).thenReturn(singletonList(configurationDeclaration));

    ExtensionParameterDescriptorModelProperty descriptorModelProperty = mock(ExtensionParameterDescriptorModelProperty.class);
    when(parameterDeclaration.getModelProperty(ExtensionParameterDescriptorModelProperty.class))
        .thenReturn(of(descriptorModelProperty));
    when(parameterDeclaration.getName()).thenReturn(REQUIRED_PARAM);
    when(descriptorModelProperty.getExtensionParameter()).thenReturn(extensionParameter);

    ExtensionParameterDescriptorModelProperty otherDescriptorModelProperty =
        mock(ExtensionParameterDescriptorModelProperty.class);
    when(otherParameterDeclaration.getModelProperty(ExtensionParameterDescriptorModelProperty.class))
        .thenReturn(of(otherDescriptorModelProperty));
    when(otherParameterDeclaration.getName()).thenReturn(OTHER_REQUIRED_PARAM);
    when(otherDescriptorModelProperty.getExtensionParameter()).thenReturn(otherExtensionParameter);

    if (TESTING_CONFIGURATION_NAME.equals(name)) {
      stereotypedDeclaration = configurationDeclaration;
    } else if (TESTING_CONNECTION_NAME.equals(name)) {
      stereotypedDeclaration = connectionProviderDeclaration;
    }
  }

  @Test
  public void infrastructureParametersAreNotIncludedWhenEnrichingWithRequiredForMetadata() {
    defineAsRequiredForMetadata(extensionParameter);
    defineAsInfrastructureParameter(otherParameterDeclaration);

    enrichDeclaration();

    verify(stereotypedDeclaration).addModelProperty(argumentCaptor.capture());

    RequiredForMetadataModelProperty value = argumentCaptor.getValue();
    assertThat(value.getRequiredParameters(), hasItem(REQUIRED_PARAM));
    assertThat(value.getRequiredParameters(), not(hasItem(OTHER_REQUIRED_PARAM)));
  }

  @Test
  public void ifNoParameterIsAnnotatedThenAllAreRequiredForMetadata() {
    enrichDeclaration();

    verify(stereotypedDeclaration).addModelProperty(argumentCaptor.capture());

    RequiredForMetadataModelProperty value = argumentCaptor.getValue();
    assertThat(value.getRequiredParameters(), hasItem(REQUIRED_PARAM));
    assertThat(value.getRequiredParameters(), hasItem(OTHER_REQUIRED_PARAM));
  }

  @Test
  public void ifNoParameterIsAnnotatedThenNonInfrastructureParametersAreRequiredForMetadata() {
    defineAsInfrastructureParameter(parameterDeclaration);
    enrichDeclaration();

    verify(stereotypedDeclaration).addModelProperty(argumentCaptor.capture());

    RequiredForMetadataModelProperty value = argumentCaptor.getValue();
    assertThat(value.getRequiredParameters(), not(hasItem(REQUIRED_PARAM)));
    assertThat(value.getRequiredParameters(), hasItem(OTHER_REQUIRED_PARAM));
  }

  @Test
  public void requiredForMetadataParameterGetsEnriched() {
    defineAsRequiredForMetadata(extensionParameter);
    enrichDeclaration();

    verify(stereotypedDeclaration).addModelProperty(argumentCaptor.capture());

    RequiredForMetadataModelProperty value = argumentCaptor.getValue();
    assertThat(value.getRequiredParameters(), hasItem(REQUIRED_PARAM));
    assertThat(value.getRequiredParameters(), not(hasItem(OTHER_REQUIRED_PARAM)));
  }

  private void defineAsRequiredForMetadata(ExtensionParameter extensionParameter) {
    when(extensionParameter.isAnnotatedWith(RequiredForMetadata.class)).thenReturn(true);
  }

  private void defineAsInfrastructureParameter(ParameterDeclaration parameterDeclaration) {
    InfrastructureParameterModelProperty infrastructureParameterModelProperty = mock(InfrastructureParameterModelProperty.class);
    when(parameterDeclaration.getModelProperty(InfrastructureParameterModelProperty.class))
        .thenReturn(of(infrastructureParameterModelProperty));
  }

  private void enrichDeclaration() {
    ArrayList<ParameterDeclaration> params = new ArrayList<>();
    params.add(parameterDeclaration);
    params.add(otherParameterDeclaration);

    when(stereotypedDeclaration.getAllParameters()).thenReturn(params);

    ENRICHER.enrich(loadingContext);
  }
}
