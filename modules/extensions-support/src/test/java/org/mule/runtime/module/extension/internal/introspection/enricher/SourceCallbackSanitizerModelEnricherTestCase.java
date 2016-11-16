/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.extension.internal.model.property.CallbackParameterModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SourceCallbackSanitizerModelEnricherTestCase extends AbstractMuleTestCase {


  @Mock
  private DescribingContext describingContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclaration extensionDeclaration;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SourceDeclaration sourceDeclaration;

  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  private ParameterDeclaration param1;
  private ParameterDeclaration param2;
  private ParameterDeclaration notACallback;

  private SourceCallbackSanitizerModelEnricher enricher = new SourceCallbackSanitizerModelEnricher();

  @Before
  public void before() {
    when(describingContext.getExtensionDeclarer()).thenReturn(mock(ExtensionDeclarer.class));
    when(describingContext.getExtensionDeclarer().getDeclaration()).thenReturn(extensionDeclaration);
    when(extensionDeclaration.getMessageSources()).thenReturn(asList(sourceDeclaration));

    param1 = mockParameter("param1", true);
    param2 = mockParameter("param2", true);
    notACallback = mockParameter("notACallback", true);
    mockParameters(sourceDeclaration, param1, param2, notACallback);
  }

  @Test
  public void unnecesarySanitization() {
    enricher.enrich(describingContext);
    assertThat(sourceDeclaration.getAllParameters(), hasSize(3));
    assertThat(sourceDeclaration.getAllParameters(), hasItems(param1, param2, notACallback));
  }

  @Test
  public void sanitization() {
    sourceDeclaration.getParameterGroups().get(0).getParameters().add(param1);
    enricher.enrich(describingContext);
    assertThat(sourceDeclaration.getAllParameters(), hasSize(3));
    assertThat(sourceDeclaration.getAllParameters(), hasItems(param1, param2, notACallback));
  }

  private ParameterDeclaration mockParameter(String name, boolean isCallback) {
    ParameterDeclaration parameter = mock(ParameterDeclaration.class);
    when(parameter.getType()).thenReturn(typeLoader.load(String.class));
    when(parameter.getName()).thenReturn(name);
    Optional<CallbackParameterModelProperty> modelProperty = isCallback
        ? of(mock(CallbackParameterModelProperty.class))
        : empty();

    when(parameter.getModelProperty(CallbackParameterModelProperty.class)).thenReturn(modelProperty);

    return parameter;
  }
}
