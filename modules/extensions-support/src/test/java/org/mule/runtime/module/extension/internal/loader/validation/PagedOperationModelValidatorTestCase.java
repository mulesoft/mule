/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getApiMethods;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.runtime.extension.internal.property.PagedOperationModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
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
public class PagedOperationModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  private ExtensionModelValidator validator = new PagedOperationModelValidator();

  @Before
  public void before() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(operationModel.getModelProperty(PagedOperationModelProperty.class))
        .thenReturn(Optional.of(new PagedOperationModelProperty()));
    when(extensionModel.getImportedTypes()).thenReturn(emptySet());
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void connectedPagingProvider() {
    mockOperationModel("connectedPagingProvider");
    validate(extensionModel, validator);
  }

  @Test
  public void notConnectedPagingProvider() {
    mockOperationModel("notConnectedPagingProvider");
    validate(extensionModel, validator);
  }

  private void mockOperationModel(String method) {
    when(operationModel.getModelProperty(ImplementingMethodModelProperty.class))
        .thenReturn(getApiMethods(PagedOperations.class).stream()
            .filter(m -> m.getName().equals(method))
            .findFirst().map(ImplementingMethodModelProperty::new));
  }

  private static class PagedOperations {

    public PagingProvider<Object, Object> connectedPagingProvider(@Connection Object connection) {
      return null;
    }

    public PagingProvider<Object, Object> notConnectedPagingProvider() {
      return null;
    }
  }

}
