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
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.introspection.SubTypesModelProperty;
import org.mule.module.extension.internal.exception.IllegalParameterModelDefinitionException;
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
public class ParameterModelValidatorTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ExtensionModel extensionModel;

    @Mock
    private OperationModel operationModel;

    @Mock
    private ParameterModel validParameterModel;

    @Mock
    private ParameterModel invalidParameterModel;

    private ParameterModelValidator validator = new ParameterModelValidator();

    @Before
    public void before()
    {
        when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
        when(extensionModel.getModelProperty(SubTypesModelProperty.class)).thenReturn(Optional.empty());
        when(operationModel.getName()).thenReturn("dummyOperation");
    }

    @Test
    public void validModel()
    {
        when(validParameterModel.getType()).thenReturn(toMetadataType(String.class));
        when(validParameterModel.getName()).thenReturn("url");
        when(operationModel.getParameterModels()).thenReturn(asList(validParameterModel));

        validator.validate(extensionModel);
    }

    @Test(expected = IllegalParameterModelDefinitionException.class)
    public void invalidModelDueToReservedName()
    {
        when(invalidParameterModel.getType()).thenReturn(toMetadataType(String.class));
        when(invalidParameterModel.getName()).thenReturn("name");
        when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
        validator.validate(extensionModel);
    }

    @Test(expected = IllegalParameterModelDefinitionException.class)
    public void invalidModelDueToDefaultValueWhenRequired()
    {
        when(invalidParameterModel.getType()).thenReturn(toMetadataType(String.class));
        when(invalidParameterModel.isRequired()).thenReturn(true);
        when(invalidParameterModel.getName()).thenReturn("url");
        when(invalidParameterModel.getDefaultValue()).thenReturn("default");
        when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
        validator.validate(extensionModel);
    }

    @Test(expected = IllegalParameterModelDefinitionException.class)
    public void invalidModelDueToNoReturnType()
    {
        when(invalidParameterModel.getType()).thenReturn(null);
        when(invalidParameterModel.getName()).thenReturn("url");
        when(operationModel.getParameterModels()).thenReturn(asList(invalidParameterModel));
        validator.validate(extensionModel);
    }
}
