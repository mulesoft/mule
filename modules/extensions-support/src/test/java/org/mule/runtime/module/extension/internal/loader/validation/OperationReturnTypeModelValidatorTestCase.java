/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.visitableMock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

import static org.springframework.core.ResolvableType.forType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.validation.OperationReturnTypeModelValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.Optional;

import com.google.common.reflect.TypeToken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@SmallTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class OperationReturnTypeModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private OperationElement operationElement;

  private OperationReturnTypeModelValidator validator = new OperationReturnTypeModelValidator();
  private ClassTypeLoader typeLoader = new DefaultExtensionsTypeLoaderFactory().createTypeLoader();

  @BeforeEach
  public void before() {
    ExtensionOperationDescriptorModelProperty modelProperty = new ExtensionOperationDescriptorModelProperty(operationElement);

    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(operationModel.getOutput())
        .thenReturn(new ImmutableOutputModel("Message.Payload", toMetadataType(String.class), false, emptySet()));
    when(operationModel.getName()).thenReturn("operation");
    when(operationModel.getModelProperty(ExtensionOperationDescriptorModelProperty.class)).thenReturn(Optional.of(modelProperty));
    when(operationElement.getReturnType()).thenReturn(new TypeWrapper(String.class, typeLoader));
    visitableMock(operationModel);
  }

  @Test
  void valid() {
    validate(extensionModel, validator);
    validate(extensionModel, validator);
  }

  @Test
  void resultWithoutGenerics() {
    when(operationElement.getReturnType()).thenReturn(new TypeWrapper(forType(new TypeToken<Result>() {}.getType()), typeLoader));
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void resultWithGenerics() {
    when(operationElement.getReturnType())
        .thenReturn(new TypeWrapper(forType(new TypeToken<Result<?, ?>>() {}.getType()), typeLoader));
    validate(extensionModel, validator);
  }

  @Test
  void resultListWithoutGenerics() {
    when(operationElement.getReturnType())
        .thenReturn(new TypeWrapper(forType(new TypeToken<List<Result>>() {}.getType()), typeLoader));
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void completionCallbackWithoutGenerics() {
    when(operationElement.getReturnType()).thenReturn(new TypeWrapper(forType(new TypeToken<Void>() {}.getType()), typeLoader));
    ExtensionParameter completionCallbackParam = mock(ExtensionParameter.class);
    when(completionCallbackParam.getType())
        .thenReturn(new TypeWrapper(forType(new TypeToken<CompletionCallback>() {}.getType()), typeLoader));
    when(operationElement.getParameters()).thenReturn(singletonList(completionCallbackParam));
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void completionCallbackWithInvalidGenerics() {
    when(operationElement.getReturnType()).thenReturn(new TypeWrapper(forType(new TypeToken<Void>() {}.getType()), typeLoader));
    ExtensionParameter completionCallbackParam = mock(ExtensionParameter.class);
    when(completionCallbackParam.getType())
        .thenReturn(new TypeWrapper(forType(new TypeToken<CompletionCallback<Void, String>>() {}.getType()), typeLoader));
    when(operationElement.getParameters()).thenReturn(singletonList(completionCallbackParam));
    ProblemsReporter problemsReporter = validate(extensionModel, validator);
    assertThat(problemsReporter.getWarnings(), hasSize(1));
  }

  @Test
  void resultWithInvalidGenerics() {
    when(operationElement.getReturnType())
        .thenReturn(new TypeWrapper(forType(new TypeToken<Result<Void, String>>() {}.getType()), typeLoader));
    ProblemsReporter problemsReporter = validate(extensionModel, validator);
    assertThat(problemsReporter.getWarnings(), hasSize(1));
  }

  @Test
  void resultCollectionWithInvalidGenerics() {
    when(operationElement.getReturnType())
        .thenReturn(new TypeWrapper(forType(new TypeToken<List<Result<Void, String>>>() {}.getType()), typeLoader));
    ProblemsReporter problemsReporter = validate(extensionModel, validator);
    assertThat(problemsReporter.getWarnings(), hasSize(1));
  }

  @Test
  void muleEventReturnType() {
    when(operationElement.getReturnType())
        .thenReturn(new TypeWrapper(forType(new TypeToken<CoreEvent>() {}.getType()), typeLoader));
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void muleMessageReturnType() {
    when(operationElement.getReturnType())
        .thenReturn(new TypeWrapper(forType(new TypeToken<Message>() {}.getType()), typeLoader));
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

}
