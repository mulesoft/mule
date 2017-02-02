/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.runtime.InterceptorFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.InterceptorsModelProperty;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ConnectionInterceptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConnectionDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private static final String CONNECTED_OPERATION = "connectedOperation";
  private static final String NOT_CONNECTED_OPERATION = "notConnectedOperation";

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionLoadingContext extensionLoadingContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclarer extensionDeclarer;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclaration extensionDeclaration;

  private OperationDeclaration connectedOperation;

  private OperationDeclaration notConnectedOperation;

  private DeclarationEnricher enricher = new ConnectionDeclarationEnricher();

  @Before
  public void before() throws Exception {
    connectedOperation = spy(new ExtensionDeclarer()
        .withOperation(CONNECTED_OPERATION)
        .requiresConnection(true)
        .getDeclaration());

    notConnectedOperation = spy(new ExtensionDeclarer()
        .withOperation(NOT_CONNECTED_OPERATION)
        .requiresConnection(false)
        .getDeclaration());

    when(extensionLoadingContext.getExtensionDeclarer()).thenReturn(extensionDeclarer);
    when(extensionDeclarer.getDeclaration()).thenReturn(extensionDeclaration);
    when(extensionDeclaration.getOperations()).thenReturn(asList(connectedOperation, notConnectedOperation));
  }


  @Test
  public void enrichConnectedOperation() throws Exception {
    enricher.enrich(extensionLoadingContext);
    verify(connectedOperation).addModelProperty(isA(InterceptorsModelProperty.class));
    InterceptorsModelProperty interceptors = connectedOperation.getModelProperty(InterceptorsModelProperty.class).get();
    assertThat(interceptors.getInterceptorFactories(), hasSize(1));
    InterceptorFactory factory = interceptors.getInterceptorFactories().get(0);
    assertThat(factory, is(notNullValue()));
    assertThat(factory.createInterceptor(), is(instanceOf(ConnectionInterceptor.class)));
  }

  @Test
  public void enrichOnlyOnceWhenFlyweight() throws Exception {
    when(extensionDeclaration.getOperations()).thenReturn(asList(connectedOperation, connectedOperation, notConnectedOperation));
    enricher.enrich(extensionLoadingContext);
    verify(connectedOperation, times(1)).addModelProperty(isA(InterceptorsModelProperty.class));
  }

  @Test
  public void skipNotConnectedOperation() throws Exception {
    enricher.enrich(extensionLoadingContext);
    verify(notConnectedOperation, never()).addModelProperty(isA(InterceptorsModelProperty.class));
  }
}
