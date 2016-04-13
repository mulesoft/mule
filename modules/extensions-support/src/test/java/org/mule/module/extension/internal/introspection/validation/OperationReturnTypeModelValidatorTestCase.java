/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.validation;

import static java.util.Arrays.asList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.api.MuleEvent;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.operation.OperationModel;
import org.mule.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OperationReturnTypeModelValidatorTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ExtensionModel extensionModel;

    @Mock
    private OperationModel operationModel;

    private OperationReturnTypeModelValidator validator = new OperationReturnTypeModelValidator();

    @Before
    public void before()
    {
        when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
        when(operationModel.getReturnType()).thenReturn(toMetadataType(String.class));
        when(operationModel.getName()).thenReturn("operation");
    }

    @Test
    public void valid()
    {
        validator.validate(extensionModel);
    }

    @Test(expected = IllegalOperationModelDefinitionException.class)
    public void nullReturnType()
    {
        when(operationModel.getReturnType()).thenReturn(null);
        validator.validate(extensionModel);
    }

    @Test(expected = IllegalOperationModelDefinitionException.class)
    public void muleEventReturnType()
    {
        when(operationModel.getReturnType()).thenReturn(toMetadataType(MuleEvent.class));
        validator.validate(extensionModel);
    }
}
