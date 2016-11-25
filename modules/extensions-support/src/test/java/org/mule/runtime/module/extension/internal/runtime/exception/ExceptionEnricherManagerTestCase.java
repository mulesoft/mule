/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.exception.Errors.Identifiers.TRANSFORMATION_ERROR_IDENTIFIER;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.exception.ErrorTypeRepository;
import org.mule.runtime.core.exception.ErrorTypeRepositoryFactory;
import org.mule.runtime.core.exception.TypedException;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.exception.ExceptionEnricher;
import org.mule.runtime.extension.api.runtime.exception.ExceptionEnricherFactory;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExceptionEnricherManagerTestCase {

  private static final String ERROR_MESSAGE = "ERROR MESSAGE";
  private static final String ERROR_NAMESPACE = "TEST-EXTENSION";

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private SourceModel sourceModel;

  @Mock
  private ExceptionEnricherFactory extensionFactory;

  @Mock
  private ExceptionEnricher extensionEnricher;

  @Mock
  private ExceptionEnricherFactory sourceFactory;

  @Mock
  private ExceptionEnricher sourceEnricher;

  private ErrorTypeRepository typeRepository = ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository();

  private ExceptionEnricherManager manager;

  @Before
  public void beforeTest() {
    when(extensionFactory.createEnricher()).thenReturn(extensionEnricher);
    mockExceptionEnricher(extensionModel, extensionFactory);
    mockExceptionEnricher(sourceModel, sourceFactory);
    when(sourceEnricher.enrichException(any(Exception.class))).thenReturn(new HeisenbergException(ERROR_MESSAGE));
    when(sourceFactory.createEnricher()).thenReturn(sourceEnricher);
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setNamespace("test-extension").build());
    when(extensionModel.getName()).thenReturn("Test Extension");
    when(sourceModel.getName()).thenReturn("Test Source");

    manager = new ExceptionEnricherManager(extensionModel, sourceModel, typeRepository);
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
    assertThat(manager.getExceptionEnricher(), is(sourceEnricher));
    mockExceptionEnricher(sourceModel, null);
    ExceptionEnricherManager manager = new ExceptionEnricherManager(extensionModel, sourceModel, typeRepository);
    assertThat(manager.getExceptionEnricher(), is(extensionEnricher));
  }

  @Test
  public void handleThrowingOfNotDeclaredErrorType() {
    when(sourceModel.getErrorModels()).thenReturn(singleton(newError(TRANSFORMATION_ERROR_IDENTIFIER, ERROR_NAMESPACE).build()));
    manager = new ExceptionEnricherManager(extensionModel, sourceModel, typeRepository);
    ModuleException moduleException =
        new ModuleException(new RuntimeException(), MuleErrors.CONNECTIVITY);
    when(sourceEnricher.enrichException(any(Exception.class))).thenReturn(moduleException);

    assertThatThrownBy(() -> manager.processException(moduleException))
        .isInstanceOf(MuleRuntimeException.class)
        .hasMessageContaining("The component 'Test Source' from the connector 'Test Extension' attempted to throw 'TEST-EXTENSION:CONNECTIVITY'");
  }

  @Test
  public void handleTypedException() {
    when(sourceModel.getErrorModels()).thenReturn(singleton(newError(CONNECTIVITY_ERROR_IDENTIFIER, ERROR_NAMESPACE).build()));
    manager = new ExceptionEnricherManager(extensionModel, sourceModel, typeRepository);
    typeRepository.addErrorType(new ComponentIdentifier.Builder()
        .withName(CONNECTIVITY_ERROR_IDENTIFIER)
        .withNamespace(ERROR_NAMESPACE)
        .build(),
                                typeRepository.getAnyErrorType());

    ModuleException moduleException =
        new ModuleException(new RuntimeException(), MuleErrors.CONNECTIVITY);
    when(sourceEnricher.enrichException(any(Exception.class))).thenReturn(moduleException);
    Exception exception = manager.processException(moduleException);

    assertThat(exception, is(instanceOf(TypedException.class)));
    ErrorType errorType = ((TypedException) exception).getErrorType();
    assertThat(errorType.getIdentifier(), is(CONNECTIVITY_ERROR_IDENTIFIER));
    assertThat(errorType.getNamespace(), is(ERROR_NAMESPACE));
  }
}
