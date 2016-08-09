/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.util.Arrays.asList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.property.ImportedTypesModelProperty;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SubtypesModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  private SubtypesModelValidator validator = new SubtypesModelValidator();


  @Test
  public void validSubtypes() {
    Map<MetadataType, List<MetadataType>> subtypes = new HashMap<>();
    subtypes.put(toMetadataType(BaseAbstractPojo.class), asList(toMetadataType(Pojo.class)));
    subtypes.put(toMetadataType(BaseCustomInterface.class), asList(toMetadataType(Pojo.class)));

    when(extensionModel.getModelProperty(SubTypesModelProperty.class))
        .thenReturn(Optional.of(new SubTypesModelProperty(subtypes)));
    when(extensionModel.getModelProperty(ImportedTypesModelProperty.class)).thenReturn(Optional.empty());

    validator.validate(extensionModel);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidAbstractSubtypes() {
    Map<MetadataType, List<MetadataType>> subtypes = new HashMap<>();
    subtypes.put(toMetadataType(BaseAbstractPojo.class), asList(toMetadataType(AbstractPojo.class)));
    subtypes.put(toMetadataType(BaseCustomInterface.class), asList(toMetadataType(CustomInterface.class)));

    when(extensionModel.getModelProperty(SubTypesModelProperty.class))
        .thenReturn(Optional.of(new SubTypesModelProperty(subtypes)));
    when(extensionModel.getModelProperty(ImportedTypesModelProperty.class)).thenReturn(Optional.empty());

    validator.validate(extensionModel);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void invalidNotSubtypesOfBaseType() {
    Map<MetadataType, List<MetadataType>> subtypes = new HashMap<>();
    subtypes.put(toMetadataType(BaseCustomInterface.class),
                 asList(toMetadataType(AbstractPojo.class), toMetadataType(CustomInterface.class)));

    when(extensionModel.getModelProperty(SubTypesModelProperty.class))
        .thenReturn(Optional.of(new SubTypesModelProperty(subtypes)));
    when(extensionModel.getModelProperty(ImportedTypesModelProperty.class)).thenReturn(Optional.empty());

    validator.validate(extensionModel);
  }

  private static abstract class BaseAbstractPojo {

    protected String basefield;

    public String getBaseField() {
      return basefield;
    }
  }

  private static abstract class AbstractPojo extends BaseAbstractPojo {

    protected String field;

    public String getField() {
      return field;
    }
  }

  public static class Pojo extends AbstractPojo implements CustomInterface {

    protected String childField;

    public String getChildField() {
      return childField;
    }

    public String getBaseField() {
      return "";
    }

    public String getField() {
      return "";
    }
  }

  private interface BaseCustomInterface {

    String getBaseField();
  }

  private interface CustomInterface extends BaseCustomInterface {

    String getField();
  }
}
