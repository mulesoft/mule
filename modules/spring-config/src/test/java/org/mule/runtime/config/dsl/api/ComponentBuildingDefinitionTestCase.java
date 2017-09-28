/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.api;

import static java.lang.String.format;
import static org.hamcrest.core.Is.is;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.KEY_TYPE_CONVERTER_AND_NO_MAP_TYPE;
import static org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.TYPE_CONVERTER_AND_NO_SIMPLE_TYPE_MESSAGE_TEMPLATE;
import static org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.TYPE_CONVERTER_AND_UNKNOWN_TYPE_MESSAGE;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromConfigurationAttribute;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.TypeConverter;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class ComponentBuildingDefinitionTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectException = none();
  private ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withIdentifier("test").withNamespace("namespace");

  @Test
  public void simpleTypeWithTypeConverter() {
    baseDefinition.withTypeDefinition(fromType(Integer.class)).withTypeConverter(getFakeTypeConverter()).build();
  }

  @Test
  public void typeFromConfigAndTypeConverter() {
    expectException.expectMessage(is(TYPE_CONVERTER_AND_UNKNOWN_TYPE_MESSAGE));
    baseDefinition.withTypeDefinition(fromConfigurationAttribute("class")).withTypeConverter(getFakeTypeConverter())
        .build();
  }

  @Test
  public void noSimpleTypeWithTypeConverter() {
    expectException.expectMessage(is(format(TYPE_CONVERTER_AND_NO_SIMPLE_TYPE_MESSAGE_TEMPLATE, Object.class.getName())));
    baseDefinition.withTypeDefinition(fromType(Object.class)).withTypeConverter(getFakeTypeConverter()).build();
  }

  @Test
  public void keyTypeConverterAndNoMapType() {
    expectException.expectMessage(is(KEY_TYPE_CONVERTER_AND_NO_MAP_TYPE));
    baseDefinition.withTypeDefinition(fromType(Object.class)).withKeyTypeConverter(getFakeTypeConverter()).build();
  }

  private TypeConverter getFakeTypeConverter() {
    return o -> null;
  }

}
