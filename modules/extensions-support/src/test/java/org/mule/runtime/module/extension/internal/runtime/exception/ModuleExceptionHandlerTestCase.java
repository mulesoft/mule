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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.TRANSFORMATION_ERROR_IDENTIFIER;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.runtime.extension.api.error.MuleErrors.CONNECTIVITY;
import static org.mule.runtime.module.extension.internal.runtime.exception.TestError.CHILD;
import static org.mule.runtime.module.extension.internal.runtime.exception.TestError.PARENT;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ModuleExceptionHandlerTestCase extends AbstractMuleTestCase {

  private static final String ERROR_NAMESPACE = "TEST-EXTENSION";

  @Mock
  private OperationModel operationModel;

  @Mock
  private ExtensionModel extensionModel;

  private ErrorTypeRepository typeRepository = createDefaultErrorTypeRepository();

  @Before
  public void setUp() {
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("test-extension").build());
    when(extensionModel.getName()).thenReturn("Test Extension");
    when(operationModel.getName()).thenReturn("testOperation");
  }

  @Test
  public void handleThrowingOfNotDeclaredErrorType() {
    typeRepository.addErrorType(buildFromStringRepresentation(ERROR_NAMESPACE + ":" + CONNECTIVITY_ERROR_IDENTIFIER),
                                typeRepository.getAnyErrorType());
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
  public void handleThrowingChildErrorsFromTheOneDeclared() {
    Set<ErrorModel> errors = new HashSet<>();
    ErrorModel parent = newError(PARENT.getType(), ERROR_NAMESPACE).build();
    ErrorModel child = newError(CHILD.getType(), ERROR_NAMESPACE).withParent(parent).build();
    errors.add(parent);

    ErrorType parentErrorType = typeRepository.addErrorType(getIdentifier(parent), typeRepository.getAnyErrorType());
    typeRepository.addErrorType(getIdentifier(child), parentErrorType);

    when(operationModel.getErrorModels()).thenReturn(errors);
    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository);
    ModuleException moduleException = new ModuleException(CHILD, new RuntimeException());

    Throwable throwable = handler.processException(moduleException);
    assertThat(throwable, is(instanceOf(TypedException.class)));
    ErrorType errorType = ((TypedException) throwable).getErrorType();
    assertThat(errorType.getIdentifier(), is(CHILD.getType()));
    assertThat(errorType.getNamespace(), is(ERROR_NAMESPACE));
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
        .name(CONNECTIVITY_ERROR_IDENTIFIER)
        .namespace(ERROR_NAMESPACE)
        .build(),
                                typeRepository.getAnyErrorType());

    ModuleException moduleException =
        new ModuleException(CONNECTIVITY, new RuntimeException());
    Throwable exception = handler.processException(moduleException);

    assertThat(exception, is(instanceOf(TypedException.class)));
    ErrorType errorType = ((TypedException) exception).getErrorType();
    assertThat(errorType.getIdentifier(), is(CONNECTIVITY_ERROR_IDENTIFIER));
    assertThat(errorType.getNamespace(), is(ERROR_NAMESPACE));
  }

  private ComponentIdentifier getIdentifier(ErrorModel parent) {
    return buildFromStringRepresentation(parent.getNamespace() + ":" + parent.getType());
  }
}
