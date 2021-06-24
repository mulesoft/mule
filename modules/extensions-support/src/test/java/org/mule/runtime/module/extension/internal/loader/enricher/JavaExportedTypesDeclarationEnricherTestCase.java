/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.property.ExportedClassNamesModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class JavaExportedTypesDeclarationEnricherTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionLoadingContext context;

  @Mock
  private ExtensionDeclarer declarer;

  @Mock
  private ExtensionDeclaration declaration;

  @Mock
  private ExtensionTypeDescriptorModelProperty typeDescriptorModelProperty;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private Type type;

  @Mock
  private AnnotationValueFetcher annotationValueFetcher;

  private JavaExportedTypesDeclarationEnricher enricher = new JavaExportedTypesDeclarationEnricher();

  @Before
  public void before() {
    when(context.getExtensionDeclarer()).thenReturn(declarer);
    when(declarer.getDeclaration()).thenReturn(declaration);
    when(declaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class)).thenReturn(of(typeDescriptorModelProperty));
    when(typeDescriptorModelProperty.getType()).thenReturn(type);
    when(type.getValueFromAnnotation(Export.class)).thenReturn(of(annotationValueFetcher));
    when(annotationValueFetcher.getClassArrayValue(any())).thenReturn(asList(type));
    when(type.getClassInformation().getClassname()).thenReturn(getClass().getName());
  }

  @Test
  public void enrichWithExportedClassNameModelProperty() {
    enricher.enrich(context);
    ArgumentCaptor<ExportedClassNamesModelProperty> captor = forClass(ExportedClassNamesModelProperty.class);
    verify(declarer).withModelProperty(captor.capture());

    ExportedClassNamesModelProperty property = captor.getValue();
    assertThat(property.getExportedClassNames(), Matchers.hasSize(1));
    assertThat(property.getExportedClassNames().iterator().next(), equalTo(getClass().getName()));
  }
}
