/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.error.ErrorModelBuilder.newError;
import static org.mule.runtime.api.util.NameUtils.hyphenize;
import static org.mule.runtime.core.api.error.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.runtime.core.api.error.Errors.Identifiers.RETRY_EXHAUSTED_ERROR_IDENTIFIER;
import static org.mule.runtime.core.internal.exception.ErrorTypeLocatorFactory.createDefaultErrorTypeLocator;
import static org.mule.runtime.module.extension.internal.manager.ExtensionErrorsRegistrant.registerErrorMappings;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.visitableMock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.message.ErrorTypeBuilder;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import io.qameta.allure.Issue;

@SmallTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class ExtensionErrorsRegistrantTestCase extends AbstractMuleTestCase {

  private static final String TEST_EXTENSION_NAME = "Test Extension";
  private static final String EXTENSION_PREFIX = "test-namespace";
  private static final String ERROR_PREFIX = EXTENSION_PREFIX.toUpperCase();
  private static final String OPERATION_NAME = "operationName";
  private static final String TEST_CONNECTIVITY_ERROR_TYPE = "TEST_CONNECTIVITY";
  private static final String MULE = "MULE";
  private static final String ANY = "ANY";

  private static final ComponentIdentifier OPERATION_IDENTIFIER = builder()
      .name(hyphenize(OPERATION_NAME))
      .namespace(EXTENSION_PREFIX)
      .build();

  private static final ErrorModel MULE_CONNECTIVITY_ERROR =
      newError(CONNECTIVITY_ERROR_IDENTIFIER, MULE)
          .build();

  private static final ErrorModel extensionConnectivityError =
      newError(TEST_CONNECTIVITY_ERROR_TYPE, ERROR_PREFIX)
          .withParent(MULE_CONNECTIVITY_ERROR)
          .build();

  @Mock
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationWithError;

  @Mock
  private OperationModel operationWithoutErrors;

  private final MuleContext muleContext = mockContextWithServices();
  private ErrorTypeRepository typeRepository;
  private ErrorTypeLocator typeLocator;

  @BeforeEach
  public void setUp() {
    XmlDslModel.XmlDslModelBuilder builder = XmlDslModel.builder();
    builder.setPrefix(EXTENSION_PREFIX);
    XmlDslModel xmlDslModel = builder.build();

    final ErrorType anyErrorType = ErrorTypeBuilder.builder()
        .namespace(MULE)
        .identifier(ANY)
        .build();

    typeRepository = mock(ErrorTypeRepository.class);
    when(typeRepository.getErrorType(any())).thenReturn(of(mock(ErrorType.class)));
    when(typeRepository.lookupErrorType(any())).thenReturn(of(mock(ErrorType.class)));
    when(typeRepository.getCriticalErrorType()).thenReturn(mock(ErrorType.class));
    when(typeRepository.getAnyErrorType()).thenReturn(anyErrorType);
    typeLocator = createDefaultErrorTypeLocator(typeRepository);

    when(muleContext.getErrorTypeRepository()).thenReturn(typeRepository);
    when(((PrivilegedMuleContext) muleContext).getErrorTypeLocator()).thenReturn(typeLocator);

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
  void lookupErrorsForOperation() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationWithError, operationWithoutErrors));

    doTest();
  }

  @Test
  @Issue("MULE-19293")
  void lookupErrorsForOperationFromConfig() {
    final ConfigurationModel configModel = mock(ConfigurationModel.class);
    when(configModel.getOperationModels()).thenReturn(asList(operationWithError, operationWithoutErrors));
    when(extensionModel.getConfigurationModels()).thenReturn(singletonList(configModel));

    doTest();
  }

  protected void doTest() {
    when(extensionModel.getErrorModels()).thenReturn(singleton(extensionConnectivityError));

    final ErrorType extConnectivityErrorType = ErrorTypeBuilder.builder()
        .namespace(ERROR_PREFIX)
        .identifier(CONNECTIVITY_ERROR_IDENTIFIER)
        .parentErrorType(ErrorTypeBuilder.builder()
            .namespace(MULE)
            .identifier(CONNECTIVITY_ERROR_IDENTIFIER)
            .parentErrorType(typeRepository.getAnyErrorType())
            .build())
        .build();

    when(typeRepository.lookupErrorType(builder().namespace(ERROR_PREFIX).name(CONNECTIVITY_ERROR_IDENTIFIER).build()))
        .thenReturn(of(extConnectivityErrorType));
    when(typeRepository.lookupErrorType(builder().namespace(ERROR_PREFIX).name(RETRY_EXHAUSTED_ERROR_IDENTIFIER).build()))
        .thenReturn(empty());

    registerErrorMappings(typeRepository, typeLocator, singleton(extensionModel), em -> empty());

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

}
