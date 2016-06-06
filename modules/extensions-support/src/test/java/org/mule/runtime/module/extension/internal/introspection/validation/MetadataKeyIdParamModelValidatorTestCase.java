/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.JavaTypeLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.metadata.extension.LocationKey;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MetadataKeyIdParamModelValidatorTestCase extends AbstractMuleTestCase
{

    private static final String INVALID_MULTILEVEL_KEY_ERROR_MESSAGE = "The parameter 'metadataKey' of the component '%s' of type 'InvalidMultiLevelKey' is an invalid metadata key id. Metadata keys of one part partshould be of String type.";
    private static final String SOURCE_WITH_INVALID_KEY = "sourceWithInvalidKey";
    private static final String OPERATION_WITH_INVALID_KEY = "operationWithInvalidKey";

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ExtensionModel extensionModel;

    @Mock
    private RuntimeOperationModel operationModel;

    @Mock
    private RuntimeSourceModel sourceModel;

    @Mock
    private ParameterModel keyParameterModel;

    @Mock
    private ParameterModel secondKeyParameterModel;

    private MetadataKeyIdModelProperty invalidMetadataKeyIdModel = new MetadataKeyIdModelProperty(new JavaTypeLoader(this.getClass().getClassLoader()).load(InvalidMultiLevelKey.class));
    private MetadataKeyIdModelProperty validMetadataKeyIdModel = new MetadataKeyIdModelProperty(new JavaTypeLoader(this.getClass().getClassLoader()).load(LocationKey.class));
    private MetadataKeyIdModelProperty stringMetadataKeyIdModel = new MetadataKeyIdModelProperty(new JavaTypeLoader(this.getClass().getClassLoader()).load(String.class));

    private MetadataKeyIdParamModelValidator validator = new MetadataKeyIdParamModelValidator();

    public static class SimpleOutputResolver implements MetadataOutputResolver<String>
    {
        @Override
        public MetadataType getOutputMetadata(MetadataContext context, String key) throws MetadataResolvingException, ConnectionException
        {
            return null;
        }
    }

    @Before
    public void before()
    {
        when(keyParameterModel.getModelProperty(MetadataKeyPartModelProperty.class)).thenReturn(Optional.of(new MetadataKeyPartModelProperty(1)));
        when(keyParameterModel.getName()).thenReturn("metadataKey");

        when(secondKeyParameterModel.getModelProperty(MetadataKeyPartModelProperty.class)).thenReturn(Optional.of(new MetadataKeyPartModelProperty(2)));
        when(secondKeyParameterModel.getName()).thenReturn("secondKey");

        when(extensionModel.getOperationModels()).thenReturn(singletonList(operationModel));
        when(extensionModel.getSourceModels()).thenReturn(singletonList(sourceModel));

        when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(Optional.of(stringMetadataKeyIdModel));
        when(sourceModel.getName()).thenReturn(SOURCE_WITH_INVALID_KEY);
        when(sourceModel.getParameterModels()).thenReturn(singletonList(keyParameterModel));

        when(operationModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(Optional.of(stringMetadataKeyIdModel));
        when(operationModel.getParameterModels()).thenReturn(singletonList(keyParameterModel));
        when(operationModel.getName()).thenReturn(OPERATION_WITH_INVALID_KEY);
    }

    @Test
    public void invalidSourceMetadataKeyIdParam()
    {
        exception.expect(IllegalModelDefinitionException.class);
        exception.expectMessage(String.format(INVALID_MULTILEVEL_KEY_ERROR_MESSAGE, SOURCE_WITH_INVALID_KEY));

        when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(Optional.of(invalidMetadataKeyIdModel));
        validator.validate(extensionModel);
    }

    @Test
    public void invalidOperationMetadataKeyIdParam()
    {
        exception.expect(IllegalModelDefinitionException.class);
        exception.expectMessage(String.format(INVALID_MULTILEVEL_KEY_ERROR_MESSAGE, OPERATION_WITH_INVALID_KEY));

        when(operationModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(Optional.of(invalidMetadataKeyIdModel));
        validator.validate(extensionModel);
    }

    @Test
    public void operationWithValidMultiLevelKey(){
        when(operationModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(Optional.of(validMetadataKeyIdModel));
        when(operationModel.getParameterModels()).thenReturn(asList(keyParameterModel, secondKeyParameterModel));
        validator.validate(extensionModel);
    }

    @Test
    public void sourceWithValidMultiLevelKey(){
        when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(Optional.of(validMetadataKeyIdModel));
        when(sourceModel.getParameterModels()).thenReturn(asList(keyParameterModel, secondKeyParameterModel));
        validator.validate(extensionModel);
    }

    @Test
    public void operationAndSourceWithSimpleKey(){
        validator.validate(extensionModel);
    }

    public static class InvalidMultiLevelKey
    {

        @MetadataKeyPart(order = 1)
        private String type;

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }
    }

}
