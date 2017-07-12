/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.exception.ErrorTypeRepository;
import org.mule.runtime.core.api.exception.TypedException;
import org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.tck.size.SmallTest;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.TRANSFORMATION_ERROR_IDENTIFIER;
import static org.mule.runtime.extension.api.error.MuleErrors.CONNECTIVITY;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ModuleExceptionHandlerTestCase {

  private static final String ERROR_NAMESPACE = "TEST-EXTENSION";

  @Mock
  private OperationModel operationModel;

  @Mock
  private ExtensionModel extensionModel;

  private ErrorTypeRepository typeRepository = ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository();

  @Before
  public void setUp() {
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("test-extension").build());
    when(extensionModel.getName()).thenReturn("Test Extension");
    when(operationModel.getName()).thenReturn("testOperation");

  }

  @Test
  public void handleThrowingOfNotDeclaredErrorType() {
    when(operationModel.getErrorModels())
        .thenReturn(singleton(newError(TRANSFORMATION_ERROR_IDENTIFIER, ERROR_NAMESPACE).build()));
    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository);
    ModuleException moduleException =
        new ModuleException(CONNECTIVITY, new RuntimeException());

    assertThatThrownBy(() -> handler.processException(moduleException))
        .isInstanceOf(MuleRuntimeException.class)
        .hasMessage("The component 'testOperation' from the connector 'Test Extension' attempted to throw 'TEST-EXTENSION:CONNECTIVITY', "
            +
            "but only [TEST-EXTENSION:TRANSFORMATION] errors are allowed.");
  }

  @Test
  public void handleThrowingOfNotRegisteredErrorType() {
    when(operationModel.getErrorModels()).thenReturn(singleton(newError(CONNECTIVITY_ERROR_IDENTIFIER, ERROR_NAMESPACE).build()));
    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository);
    ModuleException moduleException =
        new ModuleException(CONNECTIVITY, new RuntimeException());

    assertThatThrownBy(() -> handler.processException(moduleException))
        .isInstanceOf(MuleRuntimeException.class)
        .hasMessage("The component 'testOperation' from the connector 'Test Extension' attempted to throw 'TEST-EXTENSION:CONNECTIVITY',"
            +
            " but it was not registered in the Error Repository");
  }

  @Test
  public void handleTypedException() {
    when(operationModel.getErrorModels()).thenReturn(singleton(newError(CONNECTIVITY_ERROR_IDENTIFIER, ERROR_NAMESPACE).build()));
    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository);
    typeRepository.addErrorType(builder()
        .withName(CONNECTIVITY_ERROR_IDENTIFIER)
        .withNamespace(ERROR_NAMESPACE)
        .build(),
                                typeRepository.getAnyErrorType());

    ModuleException moduleException =
        new ModuleException(CONNECTIVITY, new RuntimeException());
    Throwable exception = handler.processException(moduleException);

    assertThat(exception, is(instanceOf(TypedException.class)));
    ErrorType errorType = ((TypedException) exception).getErrorType();
    assertThat(errorType.getIdentifier(), is(CONNECTIVITY_ERROR_IDENTIFIER));
    assertThat(errorType.getNamespace(), is(ERROR_NAMESPACE));
    assertThat(exception.getMessage(), is(moduleException.getMessage()));
  }
}
