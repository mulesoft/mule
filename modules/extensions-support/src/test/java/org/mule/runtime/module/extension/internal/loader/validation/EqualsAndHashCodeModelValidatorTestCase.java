/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.*;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class EqualsAndHashCodeModelValidatorTestCase extends AbstractMuleTestCase {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
    private ExtensionModel extensionModel;

    @Mock(lenient = true)
    private OperationModel operationModel;

    @Mock(lenient = true)
    private ParameterModel parameter;

    @Mock
    private ExtensionParameter extensionParameter;

    private EqualsAndHashCodeModelValidator validator = new EqualsAndHashCodeModelValidator();

    @Before
    public void before() {
        when(extensionModel.getModelProperty(CompileTimeModelProperty.class)).thenReturn(Optional.of(new CompileTimeModelProperty()));
        when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));

        when(parameter.getModelProperty(ExtensionParameterDescriptorModelProperty.class))
                .thenReturn(Optional.of(new ExtensionParameterDescriptorModelProperty(extensionParameter)));

        mockParameters(operationModel, parameter);
    }

    @Test
    public void pojoMustOverrideEqualsAndHashCode() {
        expectedException.expect(IllegalModelDefinitionException.class);
        expectedException
                .expectMessage("Type 'InvalidPojoMustOverrideEqualsAndHashCode' must override equals and hashCode");
        when(parameter.getType()).thenReturn(TYPE_LOADER.load(InvalidPojoMustOverrideEqualsAndHashCode.class));
        when(extensionParameter.getType()).thenReturn(new TypeWrapper(InvalidPojoMustOverrideEqualsAndHashCode.class, TYPE_LOADER));
        validate(extensionModel, validator);
    }

    @Test
    public void pojoImplementsEqualsAndHashCode() {
        when(parameter.getType()).thenReturn(TYPE_LOADER.load(PojoImplementsEqualsAndHashCode.class));
        when(extensionParameter.getType()).thenReturn(new TypeWrapper(PojoImplementsEqualsAndHashCode.class, TYPE_LOADER));
        validate(extensionModel, validator);
    }

    private static class InvalidPojoMustOverrideEqualsAndHashCode {

        @Parameter
        private String bar;

        @Parameter
        private String id;
    }

    private static class PojoImplementsEqualsAndHashCode {

        @Parameter
        private String bar;

        @Parameter
        private String id;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PojoImplementsEqualsAndHashCode that = (PojoImplementsEqualsAndHashCode) o;
            return Objects.equals(bar, that.bar) &&
                    Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(bar, id);
        }
    }
}
