/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_CLASSLOADER;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.property.ClassLoaderModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ClassLoaderModelEnricherTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private DescribingContext describingContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclarer extensionDeclarer;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclaration extensionDeclaration;

  @Mock
  private ClassLoader classLoader;

  private ClassLoaderModelEnricher enricher = new ClassLoaderModelEnricher();

  @Before
  public void before() {
    when(describingContext.getExtensionDeclarer()).thenReturn(extensionDeclarer);
    when(extensionDeclarer.getDeclaration()).thenReturn(extensionDeclaration);
  }

  @Test
  public void enrich() {
    setClassLoaderParameter(classLoader);
    enricher.enrich(describingContext);

    ArgumentCaptor<ClassLoaderModelProperty> captor = forClass(ClassLoaderModelProperty.class);
    verify(extensionDeclarer).withModelProperty(captor.capture());

    ClassLoaderModelProperty property = captor.getValue();
    assertThat(property, is(notNullValue()));
    assertThat(property.getClassLoader(), is(sameInstance(classLoader)));
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void noClassLoaderForEnrichment() {
    setClassLoaderParameter(null);
    enricher.enrich(describingContext);
  }

  private void setClassLoaderParameter(ClassLoader classLoader) {
    when(describingContext.getParameter(EXTENSION_CLASSLOADER, ClassLoader.class)).thenReturn(classLoader);
  }
}
