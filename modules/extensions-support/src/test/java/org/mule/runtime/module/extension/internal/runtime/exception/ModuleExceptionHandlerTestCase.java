/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider.getCoreErrorTypeRepo;
import static org.mule.runtime.core.api.error.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.TRANSFORMATION_ERROR_IDENTIFIER;
import static org.mule.runtime.extension.api.error.MuleErrors.CONNECTIVITY;
import static org.mule.runtime.module.extension.internal.runtime.exception.TestError.CHILD;
import static org.mule.runtime.module.extension.internal.runtime.exception.TestError.PARENT;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.ast.internal.error.DefaultErrorTypeRepository;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@SmallTest
@RunWith(Parameterized.class)
public class ModuleExceptionHandlerTestCase extends AbstractMuleTestCase {

  private static final String SPECIFIED_ERROR_MESSAGE = "This is the module exception message";
  private static final String SPECIFIED_CAUSE_ERROR_MESSAGE = "This is the cause message";
  private static final String ERROR_NAMESPACE = "TEST-EXTENSION";

  private OperationModel operationModel;

  private ExtensionModel extensionModel;

  private CoreEvent event;

  private final ErrorTypeRepository typeRepository = new DefaultErrorTypeRepository();

  @Parameter
  public boolean suppressErrors;

  @Parameters(name = "Suppress errors: {0}")
  public static Collection<Boolean> parameters() {
    return asList(true, false);
  }

  @Before
  public void setUp() {
    operationModel = mock(OperationModel.class);
    extensionModel = mock(ExtensionModel.class);
    event = mock(CoreEvent.class);
    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("test-extension").build());
    when(extensionModel.getName()).thenReturn("Test Extension");
    when(operationModel.getName()).thenReturn("testOperation");
  }

  @Test
  public void handleThrowingOfNotDeclaredErrorType() {
    typeRepository.addErrorType(buildFromStringRepresentation(ERROR_NAMESPACE + ":" + CONNECTIVITY_ERROR_IDENTIFIER),
                                getCoreErrorTypeRepo().getAnyErrorType());
    when(operationModel.getErrorModels())
        .thenReturn(singleton(newError(TRANSFORMATION_ERROR_IDENTIFIER, ERROR_NAMESPACE).build()));
    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository, suppressErrors);
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

    ErrorType parentErrorType = typeRepository.addErrorType(getIdentifier(parent), getCoreErrorTypeRepo().getAnyErrorType());
    typeRepository.addErrorType(getIdentifier(child), parentErrorType);

    when(operationModel.getErrorModels()).thenReturn(errors);
    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository, suppressErrors);
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
    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository, suppressErrors);
    ModuleException moduleException =
        new ModuleException(CONNECTIVITY, new RuntimeException());

    assertThatThrownBy(() -> handler.processException(moduleException))
        .isInstanceOf(MuleRuntimeException.class)
        .hasMessage("The component 'testOperation' from the connector 'Test Extension' attempted to throw 'TEST-EXTENSION:CONNECTIVITY',"
            +
            " but it was not registered in the Error Repository");
  }

  @Test
  public void handleLegacyModuleExceptionAndCreateTypedException() {
    when(operationModel.getErrorModels()).thenReturn(singleton(newError(CONNECTIVITY_ERROR_IDENTIFIER, ERROR_NAMESPACE).build()));
    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository, suppressErrors);
    typeRepository.addErrorType(builder()
        .name(CONNECTIVITY_ERROR_IDENTIFIER)
        .namespace(ERROR_NAMESPACE)
        .build(),
                                getCoreErrorTypeRepo().getAnyErrorType());

    ModuleException moduleException =
        new ModuleException(CONNECTIVITY, new RuntimeException());
    Throwable exception = handler.processException(moduleException);

    assertThat(exception, is(instanceOf(TypedException.class)));
    ErrorType errorType = ((TypedException) exception).getErrorType();
    assertThat(errorType.getIdentifier(), is(CONNECTIVITY_ERROR_IDENTIFIER));
    assertThat(errorType.getNamespace(), is(ERROR_NAMESPACE));
  }

  @Test
  public void handleSdkModuleExceptionAndCreateTypedException() {
    when(operationModel.getErrorModels()).thenReturn(singleton(newError(CONNECTIVITY_ERROR_IDENTIFIER, ERROR_NAMESPACE).build()));
    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository, suppressErrors);
    typeRepository.addErrorType(builder()
        .name(CONNECTIVITY_ERROR_IDENTIFIER)
        .namespace(ERROR_NAMESPACE)
        .build(),
                                getCoreErrorTypeRepo().getAnyErrorType());

    org.mule.sdk.api.exception.ModuleException moduleException =
        new org.mule.sdk.api.exception.ModuleException(org.mule.sdk.api.error.MuleErrors.CONNECTIVITY, new RuntimeException());
    Throwable exception = handler.processException(moduleException);

    assertThat(exception, is(instanceOf(TypedException.class)));
    ErrorType errorType = ((TypedException) exception).getErrorType();
    assertThat(errorType.getIdentifier(), is(CONNECTIVITY_ERROR_IDENTIFIER));
    assertThat(errorType.getNamespace(), is(ERROR_NAMESPACE));
  }

  @Test
  @Issue("MULE-18041")
  @Story(ERROR_HANDLING)
  public void suppressMessagingException() {
    when(event.getError()).thenReturn(Optional.empty());
    when(operationModel.getErrorModels()).thenReturn(singleton(newError(CONNECTIVITY_ERROR_IDENTIFIER, ERROR_NAMESPACE).build()));

    MessagingException messagingException = new MessagingException(
                                                                   createStaticMessage("Suppressed exception"),
                                                                   event);
    ModuleException moduleException =
        new ModuleException(CONNECTIVITY, messagingException);

    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository, suppressErrors);
    typeRepository.addErrorType(builder()
        .name(CONNECTIVITY_ERROR_IDENTIFIER)
        .namespace(ERROR_NAMESPACE)
        .build(), getCoreErrorTypeRepo().getAnyErrorType());

    Throwable exception = handler.processException(moduleException);

    assertThat(exception, is(instanceOf(TypedException.class)));
    if (suppressErrors) {
      assertThat(exception.getCause(), is(instanceOf(MuleException.class)));
      assertThat(((MuleException) exception.getCause()).getExceptionInfo().getSuppressedCauses(),
                 hasItem(isA(MessagingException.class)));
    } else {
      assertThat(exception.getCause(), is(messagingException));
    }
  }

  @Test
  @Issue("W-11192984")
  @Story(ERROR_HANDLING)
  public void useModuleExceptionMessageWhenCauseHasNoMessage() {
    Set<ErrorModel> errors = new HashSet<>();
    ErrorModel parent = newError(PARENT.getType(), ERROR_NAMESPACE).build();
    errors.add(parent);

    typeRepository.addErrorType(getIdentifier(parent), getCoreErrorTypeRepo().getAnyErrorType());

    when(operationModel.getErrorModels()).thenReturn(errors);
    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository, suppressErrors);
    ModuleException moduleException =
        new ModuleException(SPECIFIED_ERROR_MESSAGE, PARENT, new SpecificRuntimeException());

    Throwable throwable = handler.processException(moduleException);
    assertThat(throwable, is(instanceOf(TypedException.class)));
    assertThat(throwable.getMessage(), containsString(SPECIFIED_ERROR_MESSAGE));
    assertThat(throwable.getCause().getCause(), instanceOf(SpecificRuntimeException.class));
  }

  @Test
  @Issue("W-11192984")
  @Story(ERROR_HANDLING)
  public void useCauseMessageWhenCauseHasMessage() {
    Set<ErrorModel> errors = new HashSet<>();
    ErrorModel parent = newError(PARENT.getType(), ERROR_NAMESPACE).build();
    errors.add(parent);

    typeRepository.addErrorType(getIdentifier(parent), getCoreErrorTypeRepo().getAnyErrorType());

    when(operationModel.getErrorModels()).thenReturn(errors);
    ModuleExceptionHandler handler = new ModuleExceptionHandler(operationModel, extensionModel, typeRepository, suppressErrors);
    ModuleException moduleException =
        new ModuleException(SPECIFIED_ERROR_MESSAGE, PARENT, new SpecificRuntimeException(SPECIFIED_CAUSE_ERROR_MESSAGE));

    Throwable throwable = handler.processException(moduleException);
    assertThat(throwable, is(instanceOf(TypedException.class)));
    assertThat(throwable.getMessage(), containsString(SPECIFIED_CAUSE_ERROR_MESSAGE));
    assertThat(throwable.getCause(), instanceOf(SpecificRuntimeException.class));
  }

  private ComponentIdentifier getIdentifier(ErrorModel parent) {
    return buildFromStringRepresentation(parent.getNamespace() + ":" + parent.getType());
  }

  public static class SpecificRuntimeException extends RuntimeException {

    public SpecificRuntimeException() {}

    public SpecificRuntimeException(String errorMessage) {
      super(errorMessage);
    }
  }
}
