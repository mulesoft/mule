/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockSubTypes;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthParameterModelProperty;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;
import org.mule.runtime.extension.api.property.QNameModelProperty;
import org.mule.runtime.extension.internal.loader.validator.ParameterModelValidator;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.model.HealthStatus;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ParameterModelValidatorTestCase extends AbstractMuleTestCase {

  private static final String COMPONENT_ID_ERROR_PREFIX =
      "Parameter 'url' in the operation 'dummyOperation' is declared as a Component ID, but ";

  @Mock(answer = RETURNS_DEEP_STUBS, lenient = true)
  private ExtensionModel extensionModel;

  @Mock(lenient = true)
  private OperationModel operationModel;

  @Mock(lenient = true)
  private ParameterModel validParameterModel;

  @Mock(lenient = true)
  private ParameterModel invalidParameterModel;

  @Rule
  public ExpectedException expectedException = none();

  private ParameterModelValidator validator = new ParameterModelValidator();

  @Before
  public void before() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    mockSubTypes(extensionModel);
    when(extensionModel.getName()).thenReturn("extensionModel");
    when(extensionModel.getImportedTypes()).thenReturn(emptySet());
    when(extensionModel.getXmlDslModel())
        .thenReturn(XmlDslModel.builder().setPrefix("ns").setNamespace("http://www.mulesoft.org/schema/mule/ns")
            .setSchemaLocation("http://www.mulesoft.org/schema/mule/heisenberg/current/mule-ns.xsd").build());

    when(operationModel.getName()).thenReturn("dummyOperation");

    when(validParameterModel.getName()).thenReturn("url");
    when(validParameterModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(Optional.empty());
    when(validParameterModel.getModelProperty(QNameModelProperty.class)).thenReturn(Optional.empty());
    when(validParameterModel.getModelProperty(InfrastructureParameterModelProperty.class)).thenReturn(Optional.empty());
    when(validParameterModel.getModelProperty(OAuthParameterModelProperty.class)).thenReturn(Optional.empty());
    when(validParameterModel.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(validParameterModel.getRole()).thenReturn(BEHAVIOUR);
    when(validParameterModel.getLayoutModel()).thenReturn(Optional.empty());
    when(validParameterModel.getType()).thenReturn(toMetadataType(String.class));
    when(validParameterModel.getExpressionSupport()).thenReturn(SUPPORTED);

    when(invalidParameterModel.getName()).thenReturn("url");
    when(invalidParameterModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(Optional.empty());
    when(invalidParameterModel.getModelProperty(QNameModelProperty.class)).thenReturn(Optional.empty());
    when(invalidParameterModel.getModelProperty(InfrastructureParameterModelProperty.class)).thenReturn(Optional.empty());
    when(invalidParameterModel.getModelProperty(OAuthParameterModelProperty.class)).thenReturn(Optional.empty());
    when(invalidParameterModel.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(invalidParameterModel.getRole()).thenReturn(BEHAVIOUR);
    when(invalidParameterModel.getLayoutModel()).thenReturn(Optional.empty());
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(String.class));
    when(invalidParameterModel.getExpressionSupport()).thenReturn(SUPPORTED);
    when(invalidParameterModel.isRequired()).thenReturn(true);
  }

  @Test
  public void validModel() {
    mockParameters(operationModel, validParameterModel);

    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueToDefaultValueWhenRequired() {
    when(invalidParameterModel.isRequired()).thenReturn(true);
    when(invalidParameterModel.getDefaultValue()).thenReturn("default");
    mockParameters(operationModel, invalidParameterModel);
    validate(extensionModel, validator);
  }

  @Test
  public void invalidType() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage("Parameter 'url' in the operation 'dummyOperation' must provide a type");

    when(invalidParameterModel.getType()).thenReturn(null);
    mockParameters(operationModel, invalidParameterModel);

    validate(extensionModel, validator);
  }

  @Test
  public void invalidConfigOverrideWithDefault() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage("Parameter 'url' in the operation 'dummyOperation' is declared as a config override");

    when(invalidParameterModel.getDefaultValue()).thenReturn("default");
    when(invalidParameterModel.isOverrideFromConfig()).thenReturn(true);
    mockParameters(operationModel, invalidParameterModel);

    validate(extensionModel, validator);
  }


  @Test
  public void invalidEnumDefaultValue() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException
        .expectMessage("Parameter 'url' in the operation 'dummyOperation' has 'default' as default value"
            + " which is not listed as an available option (i.e.: HEALTHY, CANCER, DEAD)");

    when(invalidParameterModel.isRequired()).thenReturn(false);
    when(invalidParameterModel.getDefaultValue()).thenReturn("default");
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(HealthStatus.class));
    mockParameters(operationModel, invalidParameterModel);

    validate(extensionModel, validator);
  }

  @Test
  public void validExpressionEnumDefaultValue() {
    when(invalidParameterModel.isRequired()).thenReturn(false);
    when(invalidParameterModel.getDefaultValue()).thenReturn("#[payload]");
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(HealthStatus.class));
    mockParameters(operationModel, invalidParameterModel);

    validate(extensionModel, validator);
  }

  @Test
  public void invalidOptionalComponentId() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage(COMPONENT_ID_ERROR_PREFIX + "is also marked as Optional.");

    when(invalidParameterModel.isRequired()).thenReturn(false);
    when(invalidParameterModel.isComponentId()).thenReturn(true);
    mockParameters(operationModel, invalidParameterModel);

    validate(extensionModel, validator);
  }

  @Test
  public void invalidTypeForComponentId() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage(COMPONENT_ID_ERROR_PREFIX + "is of type 'java.lang.Integer'");

    when(invalidParameterModel.isComponentId()).thenReturn(true);
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(Integer.class));
    mockParameters(operationModel, invalidParameterModel);

    validate(extensionModel, validator);
  }

  @Test
  public void invalidComponentIdAsOverride() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage(COMPONENT_ID_ERROR_PREFIX
        + "is also declared as a ConfigOverride.");

    when(invalidParameterModel.isComponentId()).thenReturn(true);
    when(invalidParameterModel.isOverrideFromConfig()).thenReturn(true);
    mockParameters(operationModel, invalidParameterModel);

    validate(extensionModel, validator);
  }

  @Test
  public void invalidExpressionSupportForComponentIdA() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage(COMPONENT_ID_ERROR_PREFIX
        + "declares its expression support as 'SUPPORTED'");

    when(invalidParameterModel.isComponentId()).thenReturn(true);
    mockParameters(operationModel, invalidParameterModel);

    validate(extensionModel, validator);
  }

  @Test
  public void invalidLayoutTextForComponentId() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage(COMPONENT_ID_ERROR_PREFIX + "is also declared as 'Text'");

    when(invalidParameterModel.isComponentId()).thenReturn(true);
    when(invalidParameterModel.getLayoutModel()).thenReturn(Optional.of(LayoutModel.builder().asText().build()));
    mockParameters(operationModel, invalidParameterModel);

    validate(extensionModel, validator);
  }

  @Test
  public void invalidLayoutQueryForComponentId() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage(COMPONENT_ID_ERROR_PREFIX + "is also declared as 'Query'");

    when(invalidParameterModel.isComponentId()).thenReturn(true);
    when(invalidParameterModel.getLayoutModel()).thenReturn(Optional.of(LayoutModel.builder().asQuery().build()));
    mockParameters(operationModel, invalidParameterModel);

    validate(extensionModel, validator);
  }

  @Test
  public void invalidLayoutPasswordForComponentId() {
    expectedException.expect(IllegalModelDefinitionException.class);
    expectedException.expectMessage(COMPONENT_ID_ERROR_PREFIX + "is also declared as 'Password'");

    when(invalidParameterModel.isComponentId()).thenReturn(true);
    when(invalidParameterModel.getLayoutModel()).thenReturn(Optional.of(LayoutModel.builder().asPassword().build()));
    mockParameters(operationModel, invalidParameterModel);

    validate(extensionModel, validator);
  }

}
