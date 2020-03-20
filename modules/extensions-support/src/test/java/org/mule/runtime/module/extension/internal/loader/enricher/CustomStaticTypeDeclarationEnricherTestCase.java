/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Arrays.asList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputXmlType;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SmallTest
public class CustomStaticTypeDeclarationEnricherTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionLoadingContext extensionLoadingContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclarer extensionDeclarer;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclaration extensionDeclaration;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private OperationDeclaration operationDeclaration;

  private Method schemaNotFoundMethod;

  private final CustomStaticTypeDeclarationEnricher enricher = new CustomStaticTypeDeclarationEnricher();

  @Before
  public void before() throws Exception {
    when(extensionLoadingContext.getExtensionDeclarer()).thenReturn(extensionDeclarer);
    when(extensionDeclarer.getDeclaration()).thenReturn(extensionDeclaration);
    when(extensionDeclaration.getOperations()).thenReturn(asList(operationDeclaration));
    // Keep a reference to avoid GC from taking this method and null the WeakReference of ImplementingMethodModelProperty
    schemaNotFoundMethod = TestOperations.class.getMethod("schemaNotFound");

    when(operationDeclaration.getModelProperty(ImplementingMethodModelProperty.class))
        .thenReturn(Optional.of(new ImplementingMethodModelProperty(schemaNotFoundMethod)));
  }

  @Test
  public void schemaNotFound() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Can't load schema [invalid.xsd]. It was not found in the resources.");
    enricher.enrich(extensionLoadingContext);
  }

  public class TestOperations {

    @OutputXmlType(schema = "invalid.xsd", qname = "name")
    public String schemaNotFound() {
      return "";
    }
  }
}
