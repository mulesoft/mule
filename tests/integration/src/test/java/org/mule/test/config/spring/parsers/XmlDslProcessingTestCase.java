/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.test.config.spring.parsers.beans.ParameterAndChildElement;
import org.mule.test.config.spring.parsers.beans.PojoWithSameTypeChildren;
import org.mule.test.config.spring.parsers.beans.ParsersTestObject;
import org.mule.test.config.spring.parsers.beans.SimplePojo;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.core.Is;
import org.junit.Test;

public class XmlDslProcessingTestCase extends FunctionalTestCase {

  private static final String FIRST_NAME_ATTRIBUTE = "firstname";
  private static final String LAST_NAME_ATTRIBUTE = "lastname";
  private static final String AGE_ATTRIBUTE = "age";

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/xml-dsl-processing-config.xml";
  }

  @Test
  public void onlySimpleParametersInSingleAttribute() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("onlySimpleParametersObject");
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(3));
    assertPabloChildParameters(simpleParameters);
  }

  @Test
  public void firstComplexChildUsingWrapper() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("onlyComplexFirstChildParameterObject");
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertPabloChildParameters(((ParsersTestObject) simpleParameters.get("first-child")).getSimpleParameters());
  }

  @Test
  public void secondComplexChildUsingWrapper() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("onlyComplexSecondChildParameterObject");
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertMarianoChildParameters(((ParsersTestObject) simpleParameters.get("second-child")).getSimpleParameters());
  }

  @Test
  public void complexChildrenListUsingWrapper() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("onlyComplexChildrenListParameterObject");
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertCollectionChildrenContent((List<ParsersTestObject>) simpleParameters.get("other-children"));
  }

  @Test
  public void completeParametersObject() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("completeParametersObject");
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(6));
    assertPabloChildParameters(simpleParameters);
    assertPabloChildParameters(((ParsersTestObject) simpleParameters.get("first-child")).getSimpleParameters());
    assertMarianoChildParameters(((ParsersTestObject) simpleParameters.get("second-child")).getSimpleParameters());
    assertCollectionChildrenContent((List<ParsersTestObject>) simpleParameters.get("other-children"));
  }

  @Test
  public void customCollectionTypeObject() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("customCollectionTypeObject");
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    List<ParsersTestObject> collectionObject =
        (List<ParsersTestObject>) simpleParameters.get("other-children-custom-collection-type");
    assertThat(collectionObject, instanceOf(LinkedList.class));
    assertCollectionChildrenContent(collectionObject);
  }

  @Test
  public void simpleTypeObject() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("simpleTypeObject");
    assertSimpleTypeCollectionValues(parsersTestObject.getSimpleTypeList());
    assertThat(parsersTestObject.getSimpleTypeSet(), instanceOf(TreeSet.class));
    assertSimpleTypeCollectionValues(parsersTestObject.getSimpleTypeSet());
    Map<Object, Object> simpleParameters = parsersTestObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertSimpleTypeCollectionValues((List<String>) simpleParameters.get("other-simple-type-child-list-custom-key"));
  }

  @Test
  public void simpleTypeChildListWithConverter() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("simpleTypeObjectWithConverter");
    List<String> simpleTypeListWithConverter = parsersTestObject.getSimpleTypeListWithConverter();
    assertThat(simpleTypeListWithConverter.size(), is(2));
    assertThat(simpleTypeListWithConverter, hasItems("value1-with-converter", "value2-with-converter"));
  }

  @Test
  public void simpleTypeMapObject() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("simpleTypeMapObject");
    Map<String, Integer> simpleTypeMap = parsersTestObject.getSimpleTypeMap();
    assertThat(simpleTypeMap.size(), is(2));
  }

  @Test
  public void simpleListTypeMapObject() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("simpleTypeCollectionMapObject");
    Map<String, List<String>> simpleListTypeMap = parsersTestObject.getSimpleListTypeMap();
    assertThat(simpleListTypeMap.size(), is(2));
    List<String> firstCollection = simpleListTypeMap.get("1");
    assertThat(firstCollection, hasItems("value1", "value2"));
    List<String> secondCollection = simpleListTypeMap.get("2");
    assertThat(secondCollection, hasItem("#[mel:'some expression']"));
  }

  @Test
  public void complexTypeMapObject() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("complexTypeMapObject");
    Map<Long, ParsersTestObject> simpleTypeMap = parsersTestObject.getComplexTypeMap();
    assertThat(simpleTypeMap.size(), is(2));
    assertPabloChildParameters(simpleTypeMap.get(1l).getSimpleParameters());
    assertMarianoChildParameters(simpleTypeMap.get(2l).getSimpleParameters());
  }

  @Test
  public void pojoWithDefaultValue() {
    ParameterAndChildElement parameterAndChildElement = muleContext.getRegistry().get("pojoWithDefaultValue");
    assertThat(parameterAndChildElement.getSimplePojo().equals(new SimplePojo("jose")), is(true));
  }

  @Test
  public void pojoFromConfiguraitonParameter() {
    ParameterAndChildElement parameterAndChildElement = muleContext.getRegistry().get("pojoWithAttribute");
    assertThat(parameterAndChildElement.getSimplePojo().equals(new SimplePojo("pepe")), is(true));
  }

  @Test
  public void pojoFromChildConfiguration() {
    ParameterAndChildElement parameterAndChildElement = muleContext.getRegistry().get("pojoWithChild");
    assertThat(parameterAndChildElement.getSimplePojo().equals(new SimplePojo("pepe")), is(true));
  }

  @Test
  public void objectWithTwoChildrenOfSameTypeWithoutWrapper() {
    PojoWithSameTypeChildren pojoWithSameTypeChildren = muleContext.getRegistry().get("sameChildTypesObject");
    assertPabloChildParameters(pojoWithSameTypeChildren.getElementTypeA().getSimpleParameters());
    assertMarianoChildParameters(pojoWithSameTypeChildren.getAnotherElementTypeA().getSimpleParameters());
  }

  @Test
  public void textPojo() {
    SimplePojo pojo = muleContext.getRegistry().get("textPojo");
    assertThat(pojo, is(notNullValue()));
    assertThat(pojo.getSomeParameter(), is("select * from PLANET"));
  }

  @Test
  public void simpleTypeWithConverterObject() {
    ParsersTestObject parsersTestObject = muleContext.getRegistry().get("simpleTypeWithConverterObject");
    assertThat(parsersTestObject.getSimpleTypeWithConverter(), is(new SimplePojo("5")));
  }

  private void assertSimpleTypeCollectionValues(Collection<String> simpleTypeCollectionValues) {
    assertThat(simpleTypeCollectionValues.size(), is(2));
    assertThat(simpleTypeCollectionValues, hasItems("value1", "value2"));
  }

  private void assertCollectionChildrenContent(List<ParsersTestObject> collectionObjects) {
    assertPabloChildParameters(collectionObjects.get(0).getSimpleParameters());
    assertMarianoChildParameters(collectionObjects.get(1).getSimpleParameters());
  }

  private void assertPabloChildParameters(Map<Object, Object> simpleParameters) {
    assertThat(simpleParameters.get(FIRST_NAME_ATTRIBUTE), is("Pablo"));
    assertThat(simpleParameters.get(LAST_NAME_ATTRIBUTE), is("La Greca"));
    assertThat(simpleParameters.get(AGE_ATTRIBUTE), is("32"));
  }

  private void assertMarianoChildParameters(Map<Object, Object> simpleParameters) {
    assertThat(simpleParameters.get(FIRST_NAME_ATTRIBUTE), is("Mariano"));
    assertThat(simpleParameters.get(LAST_NAME_ATTRIBUTE), is("Gonzalez"));
    assertThat(simpleParameters.get(AGE_ATTRIBUTE), is("31"));
  }
}
