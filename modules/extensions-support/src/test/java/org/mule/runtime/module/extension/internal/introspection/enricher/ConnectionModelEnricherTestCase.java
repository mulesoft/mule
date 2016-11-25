/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TRANSACTIONAL_ACTION_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.introspection.enricher.EnricherTestUtils.getDeclaration;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.connectivity.TransactionalConnection;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.model.property.ConnectivityModelProperty;
import org.mule.runtime.extension.api.runtime.InterceptorFactory;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.InterceptorsModelProperty;
import org.mule.runtime.module.extension.internal.runtime.connectivity.ConnectionInterceptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConnectionModelEnricherTestCase extends AbstractMuleTestCase {

  private static final String CONNECTED_OPERATION = "connectedOperation";
  private static final String NOT_CONNECTED_OPERATION = "notConnectedOperation";
  private static final String CONNECTED_SOURCE = "connectedSource";
  private static final String NOT_CONNECTED_SOURCE = "notConnectedSource";

  @Mock(answer = RETURNS_DEEP_STUBS)
  private DescribingContext describingContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclarer extensionDeclarer;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionDeclaration extensionDeclaration;

  private OperationDeclaration connectedOperation;

  private OperationDeclaration notConnectedOperation;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SourceDeclaration connectedSource;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SourceDeclaration notConnectedSource;

  private ImplementingTypeModelProperty implementingTypeModelProperty = new ImplementingTypeModelProperty(Object.class);
  private ImplementingTypeModelProperty connectedSourceImplementingTypeModelProperty =
      new ImplementingTypeModelProperty(ConnectedSource.class);
  private ImplementingTypeModelProperty notConnectedSourceImplementingTypeModelProperty =
      new ImplementingTypeModelProperty(NotConnectedSource.class);
  private ImplementingMethodModelProperty connectedImplementingModelProperty =
      new ImplementingMethodModelProperty(Operations.class.getMethod(CONNECTED_OPERATION, TransactionalConnection.class));
  private ImplementingMethodModelProperty notConnectedImplementingModelProperty =
      new ImplementingMethodModelProperty(Operations.class.getMethod(NOT_CONNECTED_OPERATION));
  private ModelEnricher enricher = new ConnectionModelEnricher();
  private ClassTypeLoader typeLoader;

  public ConnectionModelEnricherTestCase() throws Exception {}

  @Before
  public void before() throws Exception {
    typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());

    connectedOperation = spy(new ExtensionDeclarer().withOperation(CONNECTED_OPERATION)
        .withModelProperty(connectedImplementingModelProperty)
        .getDeclaration());

    notConnectedOperation = spy(new ExtensionDeclarer().withOperation(NOT_CONNECTED_OPERATION)
        .withModelProperty(notConnectedImplementingModelProperty)
        .getDeclaration());

    when(describingContext.getExtensionDeclarer()).thenReturn(extensionDeclarer);
    when(extensionDeclarer.getDeclaration()).thenReturn(extensionDeclaration);
    when(extensionDeclaration.getOperations()).thenReturn(asList(connectedOperation, notConnectedOperation));
    when(extensionDeclaration.getMessageSources()).thenReturn(asList(connectedSource, notConnectedSource));
    when(extensionDeclaration.getModelProperty(ImplementingTypeModelProperty.class))
        .thenReturn(Optional.of(implementingTypeModelProperty));

    when(connectedSource.getModelProperty(ImplementingTypeModelProperty.class))
        .thenReturn(Optional.of(connectedSourceImplementingTypeModelProperty));
    when(connectedSource.getAllParameters()).thenReturn(emptyList());
    when(connectedSource.getName()).thenReturn(CONNECTED_SOURCE);
    when(notConnectedSource.getModelProperty(ImplementingTypeModelProperty.class))
        .thenReturn(Optional.of(notConnectedSourceImplementingTypeModelProperty));
    when(notConnectedSource.getAllParameters()).thenReturn(emptyList());
    when(notConnectedSource.getName()).thenReturn(NOT_CONNECTED_SOURCE);
  }


  @Test
  public void enrichConnectedOperation() throws Exception {
    enricher.enrich(describingContext);

    doAnswer(new Answer<BaseDeclaration>() {

      @Override
      public BaseDeclaration answer(InvocationOnMock invocationOnMock) throws Throwable {
        InterceptorsModelProperty property = (InterceptorsModelProperty) invocationOnMock.getArguments()[0];
        assertThat(property.getInterceptorFactories(), hasSize(1));
        InterceptorFactory factory = property.getInterceptorFactories().get(0);
        assertThat(factory, is(notNullValue()));
        assertThat(factory.createInterceptor(), is(instanceOf(ConnectionInterceptor.class)));

        return (BaseDeclaration) invocationOnMock.callRealMethod();
      }
    }).when(connectedOperation).addModelProperty(isA(InterceptorsModelProperty.class));

    verify(connectedOperation).addModelProperty(isA(InterceptorsModelProperty.class));
  }

  @Test
  public void enrichOnlyOnceWhenFlyweight() throws Exception {
    when(extensionDeclaration.getOperations()).thenReturn(asList(connectedOperation, connectedOperation, notConnectedOperation));
    enricher.enrich(describingContext);
    verify(connectedOperation, times(1)).addModelProperty(isA(InterceptorsModelProperty.class));
  }

  @Test
  public void skipNotConnectedOperation() throws Exception {
    enricher.enrich(describingContext);
    verify(notConnectedOperation, never()).addModelProperty(isA(InterceptorsModelProperty.class));
  }


  @Test(expected = IllegalOperationModelDefinitionException.class)
  public void transactionalActionParameter() {
    ParameterDeclaration offending = mock(ParameterDeclaration.class);
    when(offending.getName()).thenReturn(TRANSACTIONAL_ACTION_PARAMETER_NAME);
    when(connectedOperation.getAllParameters()).thenReturn(singletonList(offending));

    enricher.enrich(describingContext);
  }

  @Test
  public void verifyConnectivityModelPropertyOnConnectedOperation() {
    enricher.enrich(describingContext);
    OperationDeclaration operationDeclaration =
        getDeclaration(describingContext.getExtensionDeclarer().getDeclaration().getOperations(), CONNECTED_OPERATION);

    doAnswer(invocationOnMock -> {
      ConnectivityModelProperty property = (ConnectivityModelProperty) invocationOnMock.getArguments()[0];
      assertThat(property.getConnectionType(), is(typeLoader.load(TransactionalConnection.class)));
      return invocationOnMock.callRealMethod();
    }).when(operationDeclaration).addModelProperty(isA(ConnectivityModelProperty.class));

    verify(operationDeclaration, times(1)).addModelProperty(isA(ConnectivityModelProperty.class));
  }

  @Test
  public void verifyConnectivityModelPropertyOnNotConnectedOperation() {
    enricher.enrich(describingContext);
    OperationDeclaration operationDeclaration =
        getDeclaration(describingContext.getExtensionDeclarer().getDeclaration().getOperations(), NOT_CONNECTED_OPERATION);
    verify(operationDeclaration, never()).addModelProperty(any(ConnectivityModelProperty.class));
  }

  @Test
  public void verifyConnectivityModelPropertyOnConnectedSource() {
    enricher.enrich(describingContext);
    SourceDeclaration sourceDeclaration =
        getDeclaration(describingContext.getExtensionDeclarer().getDeclaration().getMessageSources(), CONNECTED_SOURCE);
    ArgumentCaptor<ConnectivityModelProperty> captor = forClass(ConnectivityModelProperty.class);
    verify(sourceDeclaration, times(1)).addModelProperty(captor.capture());
    assertThat(captor.getValue().getConnectionType(), is(typeLoader.load(TransactionalConnection.class)));
  }

  @Test
  public void verifyConnectivityModelPropertyOnNotConnectedSource() {
    enricher.enrich(describingContext);
    SourceDeclaration sourceDeclaration =
        getDeclaration(describingContext.getExtensionDeclarer().getDeclaration().getMessageSources(), NOT_CONNECTED_SOURCE);
    verify(sourceDeclaration, never()).addModelProperty(any(ConnectivityModelProperty.class));
  }

  private static class Operations {

    public void connectedOperation(@Connection TransactionalConnection conn) {

    }

    public void notConnectedOperation() {

    }
  }

  public static class ConnectedSource extends Source {

    @Connection
    public TransactionalConnection conn;

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {}
  }

  public static class NotConnectedSource extends Source {

    @Override
    public void onStart(SourceCallback sourceCallback) throws MuleException {

    }

    @Override
    public void onStop() {}
  }
}
