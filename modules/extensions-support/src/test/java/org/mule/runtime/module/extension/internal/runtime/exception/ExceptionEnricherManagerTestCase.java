/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandlerFactory;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExceptionEnricherManagerTestCase {

  private static final String ERROR_MESSAGE = "ERROR MESSAGE";

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private SourceModel sourceModel;

  @Mock
  private ExceptionHandlerFactory extensionFactory;

  @Mock
  private ExceptionHandler extensionEnricher;

  @Mock
  private ExceptionHandlerFactory sourceFactory;

  @Mock
  private ExceptionHandler sourceEnricher;

  private ExceptionHandlerManager manager;

  @Before
  public void beforeTest() {
    when(extensionFactory.createHandler()).thenReturn(extensionEnricher);
    mockExceptionEnricher(extensionModel, extensionFactory);
    mockExceptionEnricher(sourceModel, sourceFactory);
    when(sourceEnricher.enrichException(any(Exception.class))).thenReturn(new HeisenbergException(ERROR_MESSAGE));
    when(sourceFactory.createHandler()).thenReturn(sourceEnricher);
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("test-extension").build());
    when(extensionModel.getName()).thenReturn("Test Extension");
    when(sourceModel.getName()).thenReturn("Test Source");

    manager = new ExceptionHandlerManager(extensionModel, sourceModel);
  }

  @Test
  public void processAndEnrich() {
    ConnectionException connectionException = new ConnectionException("Connection Error");
    Exception exception = manager.processException(connectionException);
    assertThat(exception, is(not(sameInstance(connectionException))));
    assertThat(exception, is(instanceOf(HeisenbergException.class)));
    assertThat(exception.getMessage(), is(ERROR_MESSAGE));
  }

  @Test
  public void handleConnectionException() {
    Throwable e =
        new Throwable(new RuntimeException(new ExecutionException(new ConnectionException(ERROR_MESSAGE, new Exception()))));
    Exception resultException = manager.handleException(e);
    assertThat(resultException, is(instanceOf(ConnectionException.class)));
    assertThat(resultException.getMessage(), is(ERROR_MESSAGE));
  }

  @Test
  public void handleInvocationTargetExceptionCause() {
    Throwable e =
        new Throwable(new RuntimeException(new UndeclaredThrowableException(new IOException(ERROR_MESSAGE, new Exception()))));
    Exception resultException = manager.handleException(e);
    assertThat(resultException, is(instanceOf(IOException.class)));
    assertThat(resultException.getMessage(), is(ERROR_MESSAGE));
  }

  @Test
  public void findCorrectEnricher() {
    assertThat(manager.getExceptionHandler(), is(sourceEnricher));
    mockExceptionEnricher(sourceModel, null);
    ExceptionHandlerManager manager = new ExceptionHandlerManager(extensionModel, sourceModel);
    assertThat(manager.getExceptionHandler(), is(extensionEnricher));
  }
}
