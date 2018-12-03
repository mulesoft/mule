/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.extension.api.annotation.metadata.RequiredForMetadata;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequiredForMetadataDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private static final String REQUIRED_PARAM = "RequiredParam";
  private static final RequiredForMetadataDeclarationEnricher ENRICHER = new RequiredForMetadataDeclarationEnricher();

  @Mock
  ExtensionDeclaration declaration;

  @Mock(answer = RETURNS_DEEP_STUBS)
  ExtensionLoadingContext loadingContext;

  @Mock
  ConnectionProviderDeclaration connectionProviderDeclaration;

  @Mock
  ConfigurationDeclaration configurationDeclaration;

  @Mock
  ParameterDeclaration parameterDeclaration;

  @Mock
  ExtensionParameter extensionParameter;

  private ArgumentCaptor<RequiredForMetadataModelProperty> argumentCaptor = forClass(RequiredForMetadataModelProperty.class);


  @Before
  public void setUp() {
    when(loadingContext.getExtensionDeclarer().getDeclaration()).thenReturn(declaration);
    when(declaration.getConnectionProviders()).thenReturn(singletonList(connectionProviderDeclaration));
    when(declaration.getConfigurations()).thenReturn(singletonList(configurationDeclaration));
    ExtensionParameterDescriptorModelProperty descriptorModelProperty = mock(ExtensionParameterDescriptorModelProperty.class);
    when(parameterDeclaration.getModelProperty(ExtensionParameterDescriptorModelProperty.class))
        .thenReturn(Optional.of(descriptorModelProperty));
    when(parameterDeclaration.getName()).thenReturn(REQUIRED_PARAM);
    when(descriptorModelProperty.getExtensionParameter()).thenReturn(extensionParameter);
  }

  @Test
  public void connectionProviderWithRequiredForMetadataParameterGetsEnriched() {
    enrichDeclaration(connectionProviderDeclaration, true);

    verify(connectionProviderDeclaration).addModelProperty(argumentCaptor.capture());

    RequiredForMetadataModelProperty value = argumentCaptor.getValue();
    assertThat(value.getRequiredParameters(), is(hasItem(REQUIRED_PARAM)));
  }

  @Test
  public void connectionWithOutRequiredForMetadataParameterDontGetsEnriched() {
    enrichDeclaration(connectionProviderDeclaration, false);

    verify(connectionProviderDeclaration, never()).addModelProperty(any(RequiredForMetadataModelProperty.class));
  }

  @Test
  public void configWithRequiredForMetadataParameterGetsEnriched() {
    enrichDeclaration(configurationDeclaration, true);

    verify(configurationDeclaration).addModelProperty(argumentCaptor.capture());

    RequiredForMetadataModelProperty value = argumentCaptor.getValue();
    assertThat(value.getRequiredParameters(), is(hasItem(REQUIRED_PARAM)));
  }

  @Test
  public void configWithOutRequiredForMetadataParameterDontGetsEnriched() {
    enrichDeclaration(configurationDeclaration, false);

    verify(configurationDeclaration, never()).addModelProperty(any(RequiredForMetadataModelProperty.class));
  }

  private void enrichDeclaration(ParameterizedDeclaration declaration, boolean requiredParameter) {
    when(extensionParameter.isAnnotatedWith(RequiredForMetadata.class)).thenReturn(requiredParameter);
    ArrayList<ParameterDeclaration> params = new ArrayList<>();
    params.add(parameterDeclaration);
    when(declaration.getAllParameters()).thenReturn(params);

    ENRICHER.enrich(loadingContext);
  }
}
