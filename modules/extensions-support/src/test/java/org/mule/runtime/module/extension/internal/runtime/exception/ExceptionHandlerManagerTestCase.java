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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.error.ErrorModelBuilder;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.extension.api.runtime.exception.SdkExceptionHandlerFactory;
import org.mule.sdk.api.runtime.exception.ExceptionHandler;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExceptionHandlerManagerTestCase {

  private static final String EXTENSION_NAME = "Extension";
  private static final String EXTENSION_NAMESPACE = "EXTENSION";
  private static final String MULE_NAMESPACE = "MULE";
  private static final String CONNECTIVITY_ERROR_TYPE = "CONNECTIVITY";
  private static final String ERROR_MESSAGE = "ERROR MESSAGE";
  private static final String EXTENSION_NAME_WITH_SPACES = "extension With Spaces";

  @Mock(lenient = true)
  private ExtensionModel extensionModel;

  @Mock(lenient = true)
  private SourceModel sourceModel;

  @Mock
  private SdkExceptionHandlerFactory extensionFactory;

  @Mock
  private ExceptionHandler extensionEnricher;

  @Mock
  private SdkExceptionHandlerFactory sourceFactory;

  @Mock
  private ExceptionHandler sourceEnricher;

  @Mock
  private ErrorTypeRepository errorTypeRepository;

  private ExceptionHandlerManager manager;

  @Before
  public void beforeTest() {
    when(extensionFactory.createHandler()).thenReturn(extensionEnricher);
    mockExceptionEnricher(extensionModel, extensionFactory);
    mockExceptionEnricher(sourceModel, sourceFactory);
    when(sourceEnricher.enrichException(any(Exception.class))).thenReturn(new HeisenbergException(ERROR_MESSAGE));
    when(sourceFactory.createHandler()).thenReturn(sourceEnricher);
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("test-extension").build());
    when(extensionModel.getName()).thenReturn(EXTENSION_NAME);
    when(extensionModel.getErrorModels()).thenReturn(Collections.EMPTY_SET);
    when(sourceModel.getName()).thenReturn("Test Source");

    manager = new ExceptionHandlerManager(extensionModel, sourceModel);

    mockErrorTypesRepository();
  }

  @Test
  public void processAndEnrich() {
    ConnectionException connectionException = new ConnectionException("Connection Error");
    Throwable throwable = manager.process(connectionException);
    assertThat(throwable, is(not(sameInstance(connectionException))));
    assertThat(throwable, is(instanceOf(HeisenbergException.class)));
    assertThat(throwable.getMessage(), is(ERROR_MESSAGE));
  }

  @Test
  public void handleConnectionException() {
    ConnectionException rootCause = new ConnectionException(ERROR_MESSAGE, new Exception());
    Throwable throwable = manager.handleThrowable(new Throwable(new RuntimeException(new ExecutionException(rootCause))));
    assertThat(throwable, is(instanceOf(ConnectionException.class)));
    assertThat(throwable.getMessage(), is(ERROR_MESSAGE));
    assertThat(throwable, is(sameInstance(rootCause)));
  }

  @Test
  public void handleSdkMethodInvocationExceptionCause() {
    IOException rootCause = new IOException(ERROR_MESSAGE, new Exception());
    // The root cause is contained in a reflective UndeclaredThrowableException exception
    Throwable throwable = manager.handleThrowable(new SdkMethodInvocationException(rootCause));
    assertThat(throwable, is(instanceOf(IOException.class)));
    assertThat(throwable.getMessage(), is(ERROR_MESSAGE));
    assertThat(throwable, is(sameInstance(rootCause)));
  }

  @Test
  public void findCorrectEnricher() {
    assertThat(manager.getExceptionHandler(), is(sourceEnricher));
    mockExceptionEnricher(sourceModel, null);
    ExceptionHandlerManager manager = new ExceptionHandlerManager(extensionModel, sourceModel);
    assertThat(manager.getExceptionHandler(), is(extensionEnricher));
  }

  @Test
  public void processError() {
    Error rootCause = new Error(ERROR_MESSAGE, new Exception());
    Throwable throwable = manager.process(rootCause);
    assertThat(throwable.getMessage(), is(ERROR_MESSAGE));
    assertThat(throwable, is(sameInstance(rootCause)));
  }

  @Test
  public void handleConnectionExceptionExtensionWithCustomConnectivityError() {
    Set<ErrorModel> errorModels = new HashSet<>();
    errorModels.add(ErrorModelBuilder.newError(CONNECTIVITY_ERROR_TYPE, EXTENSION_NAMESPACE).build());
    errorModels.add(ErrorModelBuilder.newError(CONNECTIVITY_ERROR_TYPE, MULE_NAMESPACE).build());
    when(extensionModel.getErrorModels()).thenReturn(errorModels);

    ExceptionHandlerManager exceptionHandlerManager =
        new ExceptionHandlerManager(extensionModel, sourceModel, errorTypeRepository);
    ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE, new Exception());

    Throwable throwable = exceptionHandlerManager.handleThrowable(new Throwable(connectionException));
    assertThat(throwable, is(instanceOf(ConnectionException.class)));
    assertThat(throwable.getMessage(), is(ERROR_MESSAGE));
    assertThat(((ConnectionException) throwable).getErrorType().isPresent(), is(true));
    assertThat(((ConnectionException) throwable).getErrorType().get().getIdentifier(), is(CONNECTIVITY_ERROR_TYPE));
    assertThat(((ConnectionException) throwable).getErrorType().get().getNamespace(), is(EXTENSION_NAMESPACE));
  }

  @Test
  @Issue("W-10617943")
  @Description("This test checks for extension names with spaces and verifies that correct error type is picked based on namespace")
  public void handleConnectionExceptionExtensionWithSpacesAndCustomConnectivityError() {
    Set<ErrorModel> errorModels = new HashSet<>();
    errorModels.add(ErrorModelBuilder.newError(CONNECTIVITY_ERROR_TYPE, EXTENSION_NAMESPACE).build());
    errorModels.add(ErrorModelBuilder.newError(CONNECTIVITY_ERROR_TYPE, MULE_NAMESPACE).build());

    when(extensionModel.getName()).thenReturn(EXTENSION_NAME_WITH_SPACES);
    when(extensionModel.getErrorModels()).thenReturn(errorModels);
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix(EXTENSION_NAMESPACE).build());

    ExceptionHandlerManager exceptionHandlerManager =
        new ExceptionHandlerManager(extensionModel, sourceModel, errorTypeRepository);
    ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE, new Exception());

    Throwable throwable = exceptionHandlerManager.handleThrowable(new Throwable(connectionException));
    assertThat(throwable, is(instanceOf(ConnectionException.class)));
    assertThat(throwable.getMessage(), is(ERROR_MESSAGE));
    assertThat(((ConnectionException) throwable).getErrorType().isPresent(), is(true));
    assertThat(((ConnectionException) throwable).getErrorType().get().getIdentifier(), is(CONNECTIVITY_ERROR_TYPE));
    assertThat(((ConnectionException) throwable).getErrorType().get().getNamespace(), is(EXTENSION_NAMESPACE));
  }

  @Test
  public void handleConnectionExceptionExtensionWithoutCustomConnectivityError() {
    Set<ErrorModel> errorModels = new HashSet<>();
    errorModels.add(ErrorModelBuilder.newError(CONNECTIVITY_ERROR_TYPE, MULE_NAMESPACE).build());
    when(extensionModel.getErrorModels()).thenReturn(errorModels);

    ExceptionHandlerManager exceptionHandlerManager =
        new ExceptionHandlerManager(extensionModel, sourceModel, errorTypeRepository);
    ConnectionException connectionException = new ConnectionException(ERROR_MESSAGE, new Exception());

    Throwable throwable = exceptionHandlerManager.handleThrowable(new Throwable(connectionException));
    assertThat(throwable, is(instanceOf(ConnectionException.class)));
    assertThat(throwable.getMessage(), is(ERROR_MESSAGE));
    assertThat(((ConnectionException) throwable).getErrorType().isPresent(), is(true));
    assertThat(((ConnectionException) throwable).getErrorType().get().getIdentifier(), is(CONNECTIVITY_ERROR_TYPE));
    assertThat(((ConnectionException) throwable).getErrorType().get().getNamespace(), is(MULE_NAMESPACE));
  }

  private void mockErrorTypesRepository() {
    ErrorType extensionConnectivityErrorType = createErrorType(CONNECTIVITY_ERROR_TYPE, EXTENSION_NAMESPACE);
    ErrorType muleConnectivityErrorType = createErrorType(CONNECTIVITY_ERROR_TYPE, MULE_NAMESPACE);

    when(errorTypeRepository.getErrorType(ComponentIdentifier.builder()
        .namespace(EXTENSION_NAMESPACE)
        .name(CONNECTIVITY_ERROR_TYPE)
        .build())).thenReturn(Optional.of(extensionConnectivityErrorType));

    when(errorTypeRepository.getErrorType(ComponentIdentifier.builder()
        .namespace(MULE_NAMESPACE)
        .name(CONNECTIVITY_ERROR_TYPE)
        .build())).thenReturn(Optional.of(muleConnectivityErrorType));
  }

  private ErrorType createErrorType(String identifier, String namespace) {
    return new ErrorType() {

      @Override
      public String getIdentifier() {
        return identifier;
      }

      @Override
      public String getNamespace() {
        return namespace;
      }

      @Override
      public ErrorType getParentErrorType() {
        return null;
      }
    };
  }
}
