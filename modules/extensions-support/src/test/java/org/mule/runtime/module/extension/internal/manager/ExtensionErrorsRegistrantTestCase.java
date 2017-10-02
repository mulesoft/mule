/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.internal.exception.ErrorTypeLocatorFactory.createDefaultErrorTypeLocator;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.error.ErrorModelBuilder;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.extension.api.util.NameUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExtensionErrorsRegistrantTestCase extends AbstractMuleTestCase {

  private static final String TEST_EXTENSION_NAME = "Test Extension";
  private static final String EXTENSION_PREFIX = "test-namespace";
  private static final String ERROR_PREFIX = EXTENSION_PREFIX.toUpperCase();
  private static final String OPERATION_NAME = "operationName";
  private static final String TEST_CONNECTIVITY_ERROR_TYPE = "TEST_CONNECTIVITY";
  private static final String OAUTH_TEST_CONNECTIVITY_ERROR_TYPE = "OAUTH_CONNECTIVITY";
  private static final String MULE = "MULE";
  private static final String ANY = "ANY";

  private static final ComponentIdentifier OPERATION_IDENTIFIER = builder()
      .name(NameUtils.hyphenize(OPERATION_NAME))
      .namespace(EXTENSION_PREFIX)
      .build();

  private static final ErrorModel MULE_CONNECTIVITY_ERROR =
      newError(CONNECTIVITY_ERROR_IDENTIFIER, MULE)
          .build();

  private static final ErrorModel extensionConnectivityError =
      newError(TEST_CONNECTIVITY_ERROR_TYPE, ERROR_PREFIX)
          .withParent(MULE_CONNECTIVITY_ERROR)
          .build();

  private static final ErrorModel oauthExtensionConnectivityError =
      newError(OAUTH_TEST_CONNECTIVITY_ERROR_TYPE, ERROR_PREFIX)
          .withParent(extensionConnectivityError)
          .build();

  private static final ErrorModel customErrorModel =
      newError("CUSTOM", MULE)
          .withParent(newError(ANY, MULE).build()).build();

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationWithError;

  @Mock
  private OperationModel operationWithoutErrors;

  @Rule
  public ExpectedException exception = none();

  private ExtensionErrorsRegistrant errorsRegistrant;
  private MuleContext muleContext = mockContextWithServices();
  private ErrorTypeRepository typeRepository;
  private ErrorTypeLocator typeLocator;

  @Before
  public void setUp() {
    XmlDslModel.XmlDslModelBuilder builder = XmlDslModel.builder();
    builder.setPrefix(EXTENSION_PREFIX);
    XmlDslModel xmlDslModel = builder.build();

    typeRepository = createDefaultErrorTypeRepository();
    typeLocator = createDefaultErrorTypeLocator(typeRepository);

    when(muleContext.getErrorTypeRepository()).thenReturn(typeRepository);
    when(((PrivilegedMuleContext) muleContext).getErrorTypeLocator()).thenReturn(typeLocator);
    errorsRegistrant = new ExtensionErrorsRegistrant(muleContext.getErrorTypeRepository(),
                                                     ((PrivilegedMuleContext) muleContext).getErrorTypeLocator());

    when(extensionModel.getOperationModels()).thenReturn(asList(operationWithError, operationWithoutErrors));
    when(extensionModel.getXmlDslModel()).thenReturn(xmlDslModel);
    when(extensionModel.getName()).thenReturn(TEST_EXTENSION_NAME);

    when(operationWithError.getErrorModels()).thenReturn(singleton(extensionConnectivityError));
    when(operationWithError.getName()).thenReturn(OPERATION_NAME);
    when(operationWithError.getModelProperty(any())).thenReturn(empty());

    when(operationWithoutErrors.getName()).thenReturn("operationWithoutError");
    when(operationWithoutErrors.getErrorModels()).thenReturn(emptySet());
    when(operationWithoutErrors.getModelProperty(any())).thenReturn(empty());

    visitableMock(operationWithError, operationWithoutErrors);
  }

  @Test
  public void lookupErrorsForOperation() {
    when(extensionModel.getErrorModels()).thenReturn(singleton(extensionConnectivityError));
    errorsRegistrant.registerErrors(extensionModel);
    ErrorType errorType = typeLocator.lookupComponentErrorType(OPERATION_IDENTIFIER, ConnectionException.class);

    assertThat(errorType.getIdentifier(), is(CONNECTIVITY_ERROR_IDENTIFIER));
    assertThat(errorType.getNamespace(), is(EXTENSION_PREFIX.toUpperCase()));

    ErrorType muleConnectivityError = errorType.getParentErrorType();
    assertThat(muleConnectivityError.getNamespace(), is(MULE_CONNECTIVITY_ERROR.getNamespace()));
    assertThat(muleConnectivityError.getIdentifier(), is(MULE_CONNECTIVITY_ERROR.getType()));

    ErrorType anyErrorType = muleConnectivityError.getParentErrorType();
    assertThat(anyErrorType.getNamespace(), is(MULE));
    assertThat(anyErrorType.getIdentifier(), is(ANY));

    assertThat(anyErrorType.getParentErrorType(), is(nullValue()));
  }

  @Test
  public void registerErrorTypes() {
    when(extensionModel.getErrorModels()).thenReturn(singleton(oauthExtensionConnectivityError));
    errorsRegistrant.registerErrors(extensionModel);

    Optional<ErrorType> optionalOAuthType = typeRepository.lookupErrorType(builder()
        .name(OAUTH_TEST_CONNECTIVITY_ERROR_TYPE).namespace(EXTENSION_PREFIX).build());
    Optional<ErrorType> optionalConnectivityType = typeRepository.lookupErrorType(builder()
        .name(TEST_CONNECTIVITY_ERROR_TYPE).namespace(EXTENSION_PREFIX).build());

    assertThat(optionalOAuthType.isPresent(), is(true));
    assertThat(optionalConnectivityType.isPresent(), is(true));

    ErrorType parentErrorType = optionalOAuthType.get().getParentErrorType();
    assertThat(parentErrorType, is(optionalConnectivityType.get()));
  }

  @Test
  public void operationWithoutErrorsDoesntGenerateComponentMapper() {
    when(extensionModel.getOperationModels()).thenReturn(singletonList(operationWithoutErrors));
    ErrorTypeLocator mockTypeLocator = mock(ErrorTypeLocator.class);
    errorsRegistrant = new ExtensionErrorsRegistrant(typeRepository, mockTypeLocator);

    errorsRegistrant.registerErrors(extensionModel);
    verify(mockTypeLocator, times(0)).addComponentExceptionMapper(any(), any());
  }

  @Test
  public void operationTriesToAddInternalErrorType() {
    ErrorTypeRepository repository = mock(ErrorTypeRepository.class);
    when(repository.getErrorType(any())).then((e) -> typeRepository.getErrorType(((ComponentIdentifier) e.getArguments()[0])));
    ErrorModel internalRepeatedError = ErrorModelBuilder.newError(SOURCE_RESPONSE_GENERATE).build();
    when(operationWithError.getErrorModels()).thenReturn(singleton(internalRepeatedError));
    when(extensionModel.getOperationModels()).thenReturn(singletonList(operationWithError));
    when(extensionModel.getErrorModels()).thenReturn(singleton(internalRepeatedError));
    ErrorTypeLocator mockTypeLocator = mock(ErrorTypeLocator.class);
    errorsRegistrant = new ExtensionErrorsRegistrant(typeRepository, mockTypeLocator);
    errorsRegistrant.registerErrors(extensionModel);
    verify(repository, times(0)).addErrorType(any(), any());
  }

  @Test
  public void extensionCantRegisterAMuleErrorType() {
    exception.expect(MuleRuntimeException.class);
    exception.expectMessage("The extension [" + TEST_EXTENSION_NAME
        + "] tried to register the [MULE:CUSTOM] error with [MULE] namespace, which is not allowed");
    when(operationWithError.getErrorModels()).thenReturn(singleton(customErrorModel));
    when(extensionModel.getErrorModels()).thenReturn(singleton(customErrorModel));
    errorsRegistrant.registerErrors(extensionModel);
  }
}
