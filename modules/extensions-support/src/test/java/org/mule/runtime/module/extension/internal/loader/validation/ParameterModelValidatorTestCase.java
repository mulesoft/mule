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
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.arrayOf;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.dictionaryOf;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockSubTypes;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.objectTypeBuilder;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.internal.loader.validator.ParameterModelValidator;
import org.mule.runtime.extension.internal.property.InfrastructureParameterModelProperty;
import org.mule.runtime.extension.internal.property.QNameModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ParameterModelValidatorTestCase extends AbstractMuleTestCase {

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
  public void before() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    mockSubTypes(extensionModel);
    when(extensionModel.getName()).thenReturn("extensionModel");
    when(extensionModel.getImportedTypes()).thenReturn(emptySet());
    when(extensionModel.getXmlDslModel())
        .thenReturn(XmlDslModel.builder().setPrefix("ns").setNamespace("http://www.mulesoft.org/schema/mule/ns")
            .setSchemaLocation("http://www.mulesoft.org/schema/mule/heisenberg/current/mule-ns.xsd").build());

    when(operationModel.getName()).thenReturn("dummyOperation");

    when(validParameterModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(Optional.empty());
    when(validParameterModel.getModelProperty(QNameModelProperty.class)).thenReturn(Optional.empty());
    when(validParameterModel.getModelProperty(InfrastructureParameterModelProperty.class)).thenReturn(Optional.empty());
    when(validParameterModel.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(validParameterModel.getRole()).thenReturn(BEHAVIOUR);
    when(validParameterModel.getLayoutModel()).thenReturn(Optional.empty());

    when(invalidParameterModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(Optional.empty());
    when(invalidParameterModel.getModelProperty(QNameModelProperty.class)).thenReturn(Optional.empty());
    when(invalidParameterModel.getModelProperty(InfrastructureParameterModelProperty.class)).thenReturn(Optional.empty());
    when(invalidParameterModel.getDslConfiguration()).thenReturn(ParameterDslConfiguration.getDefaultInstance());
    when(invalidParameterModel.getRole()).thenReturn(BEHAVIOUR);
    when(invalidParameterModel.getLayoutModel()).thenReturn(Optional.empty());
  }

  @Test
  public void validModel() {
    when(validParameterModel.getType()).thenReturn(toMetadataType(String.class));
    when(validParameterModel.getName()).thenReturn("url");
    mockParameters(operationModel, validParameterModel);

    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueToReservedName() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(String.class));
    when(invalidParameterModel.getName()).thenReturn("name");
    mockParameters(operationModel, invalidParameterModel);
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalParameterModelDefinitionException.class)
  public void invalidParameterDueToReservedName() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(InvalidPojo.class));
    when(invalidParameterModel.getName()).thenReturn("pojo");
    mockParameters(operationModel, invalidParameterModel);
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidParameterCollectionDueToReservedName() {
    when(invalidParameterModel.getType())
        .thenReturn(ExtensionsTestUtils.arrayOf(List.class, objectTypeBuilder(InvalidPojo.class)));
    when(invalidParameterModel.getType()).thenReturn(arrayOf(List.class, objectTypeBuilder(InvalidPojo.class)));
    when(invalidParameterModel.getName()).thenReturn("pojos");
    mockParameters(operationModel, invalidParameterModel);
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidParameterDictionaryDueToReservedName() {
    when(invalidParameterModel.getType())
        .thenReturn(dictionaryOf(Map.class, objectTypeBuilder(InvalidPojo.class)));
    when(invalidParameterModel.getModelProperty(InfrastructureParameterModelProperty.class)).thenReturn(Optional.empty());
    when(invalidParameterModel.getName()).thenReturn("pojos");
    mockParameters(operationModel, invalidParameterModel);
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidRecursiveParameterDueToReservedName() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(RecursivePojo.class));
    when(invalidParameterModel.getName()).thenReturn("pojo");
    mockParameters(operationModel, invalidParameterModel);
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidNestedParameterDueToReservedName() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(NestedInvalidPojo.class));
    when(invalidParameterModel.getName()).thenReturn("pojo");
    mockParameters(operationModel, invalidParameterModel);
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueToDefaultValueWhenRequired() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(String.class));
    when(invalidParameterModel.isRequired()).thenReturn(true);
    when(invalidParameterModel.getName()).thenReturn("url");
    when(invalidParameterModel.getDefaultValue()).thenReturn("default");
    mockParameters(operationModel, invalidParameterModel);
    validate(extensionModel, validator);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidModelDueToListWithoutPluralName() {
    when(invalidParameterModel.getType()).thenReturn(toMetadataType(List.class));
    when(invalidParameterModel.getName()).thenReturn("thing");
    mockParameters(operationModel, invalidParameterModel);
    validate(extensionModel, validator);
  }

  @XmlHints(allowTopLevelDefinition = true)
  private static class InvalidPojo {

    public InvalidPojo() {
      // needs to be instantiable
    }

    @Parameter
    private String name;

    public String getName() {
      return name;
    }
  }

  public static class NestedInvalidPojo {

    @Parameter
    private InvalidPojo invalidPojo;

    public InvalidPojo getInvalidPojo() {
      return invalidPojo;
    }
  }

  public static class RecursivePojo {

    @Parameter
    private RecursivePojo pojo;

    @Parameter
    private InvalidPojo invalidPojo;

    public RecursivePojo getPojo() {
      return pojo;
    }

    public InvalidPojo getInvalidPojo() {
      return invalidPojo;
    }
  }

  public static class InvalidPojoParameterGroup {

    @ParameterGroup(name = "exclusion")
    private Serializable nonInstantiableField;
  }

}
