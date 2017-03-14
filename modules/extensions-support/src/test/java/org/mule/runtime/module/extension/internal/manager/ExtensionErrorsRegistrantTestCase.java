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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.runtime.core.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.exception.ErrorTypeLocator;
import org.mule.runtime.core.exception.ErrorTypeLocatorFactory;
import org.mule.runtime.core.exception.ErrorTypeRepository;
import org.mule.runtime.core.exception.ErrorTypeRepositoryFactory;
import org.mule.runtime.extension.api.util.NameUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExtensionErrorsRegistrantTestCase extends AbstractMuleTestCase {

  private static final String EXTENSION_PREFIX = "test-namespace";
  private static final String ERROR_PREFIX = EXTENSION_PREFIX.toUpperCase();
  private static final String OPERATION_NAME = "operationName";
  private static final String TEST_CONNECTIVITY_ERROR_TYPE = "TEST_CONNECTIVITY";
  private static final String OAUTH_TEST_CONNECTIVITY_ERROR_TYPE = "OAUTH_CONNECTIVITY";
  private static final String MULE = "MULE";

  private static final ComponentIdentifier OPERATION_IDENTIFIER = builder()
      .withName(NameUtils.hyphenize(OPERATION_NAME))
      .withNamespace(EXTENSION_PREFIX)
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

  public static final String ANY = "ANY";

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationWithError;

  @Mock
  private OperationModel operationWithoutErrors;

  private ExtensionErrorsRegistrant errorsRegistrant;
  private MuleContext muleContext = mockContextWithServices();
  private ErrorTypeRepository typeRepository;
  private ErrorTypeLocator typeLocator;

  @Before
  public void setUp() {
    XmlDslModel.XmlDslModelBuilder builder = XmlDslModel.builder();
    builder.setPrefix(EXTENSION_PREFIX);
    XmlDslModel xmlDslModel = builder.build();

    typeRepository = ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository();
    typeLocator = ErrorTypeLocatorFactory.createDefaultErrorTypeLocator(typeRepository);

    when(muleContext.getErrorTypeRepository()).thenReturn(typeRepository);
    when(muleContext.getErrorTypeLocator()).thenReturn(typeLocator);
    errorsRegistrant = new ExtensionErrorsRegistrant(muleContext.getErrorTypeRepository(), muleContext.getErrorTypeLocator());

    when(extensionModel.getOperationModels()).thenReturn(asList(operationWithError, operationWithoutErrors));
    when(extensionModel.getXmlDslModel()).thenReturn(xmlDslModel);

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
        .withName(OAUTH_TEST_CONNECTIVITY_ERROR_TYPE).withNamespace(EXTENSION_PREFIX).build());
    Optional<ErrorType> optionalConnectivityType = typeRepository.lookupErrorType(builder()
        .withName(TEST_CONNECTIVITY_ERROR_TYPE).withNamespace(EXTENSION_PREFIX).build());

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
}
