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
import org.mule.test.config.spring.parsers.beans.SimpleCollectionObject;
import org.mule.test.config.spring.parsers.beans.SimplePojo;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
    SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("onlySimpleParametersObject");
    Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(3));
    assertPabloChildParameters(simpleParameters);
  }

  @Test
  public void firstComplexChildUsingWrapper() {
    SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("onlyComplexFirstChildParameterObject");
    Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertPabloChildParameters(((SimpleCollectionObject) simpleParameters.get("first-child")).getSimpleParameters());
  }

  @Test
  public void secondComplexChildUsingWrapper() {
    SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("onlyComplexSecondChildParameterObject");
    Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertMarianoChildParameters(((SimpleCollectionObject) simpleParameters.get("second-child")).getSimpleParameters());
  }

  @Test
  public void complexChildrenListUsingWrapper() {
    SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("onlyComplexChildrenListParameterObject");
    Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertCollectionChildrenContent((List<SimpleCollectionObject>) simpleParameters.get("other-children"));
  }

  @Test
  public void completeParametersObject() {
    SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("completeParametersObject");
    Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(6));
    assertPabloChildParameters(simpleParameters);
    assertPabloChildParameters(((SimpleCollectionObject) simpleParameters.get("first-child")).getSimpleParameters());
    assertMarianoChildParameters(((SimpleCollectionObject) simpleParameters.get("second-child")).getSimpleParameters());
    assertCollectionChildrenContent((List<SimpleCollectionObject>) simpleParameters.get("other-children"));
  }

  @Test
  public void customCollectionTypeObject() {
    SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("customCollectionTypeObject");
    Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    List<SimpleCollectionObject> collectionObject =
        (List<SimpleCollectionObject>) simpleParameters.get("other-children-custom-collection-type");
    assertThat(collectionObject, instanceOf(LinkedList.class));
    assertCollectionChildrenContent(collectionObject);
  }

  @Test
  public void simpleTypeObject() {
    SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("simpleTypeObject");
    assertSimpleTypeCollectionValues(simpleCollectionObject.getSimpleTypeList());
    assertThat(simpleCollectionObject.getSimpleTypeSet(), instanceOf(TreeSet.class));
    assertSimpleTypeCollectionValues(simpleCollectionObject.getSimpleTypeSet());
    Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
    assertThat(simpleParameters.size(), is(1));
    assertSimpleTypeCollectionValues((List<String>) simpleParameters.get("other-simple-type-child-list-custom-key"));
  }

  @Test
  public void simpleTypeChildListWithConverter() {
    SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("simpleTypeObjectWithConverter");
    List<String> simpleTypeListWithConverter = simpleCollectionObject.getSimpleTypeListWithConverter();
    assertThat(simpleTypeListWithConverter.size(), is(2));
    assertThat(simpleTypeListWithConverter, hasItems("value1-with-converter", "value2-with-converter"));
  }

  @Test
  public void simpleTypeMapObject() {
    SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("simpleTypeMapObject");
    Map<String, Integer> simpleTypeMap = simpleCollectionObject.getSimpleTypeMap();
    assertThat(simpleTypeMap.size(), is(2));
  }

  @Test
  public void simpleListTypeMapObject() {
    SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("simpleTypeCollectionMapObject");
    Map<String, List<String>> simpleListTypeMap = simpleCollectionObject.getSimpleListTypeMap();
    assertThat(simpleListTypeMap.size(), is(2));
    List<String> firstCollection = simpleListTypeMap.get("1");
    assertThat(firstCollection, hasItems("value1", "value2"));
    List<String> secondCollection = simpleListTypeMap.get("2");
    assertThat(secondCollection, hasItem("#['some expression']"));
  }

  @Test
  public void complexTypeMapObject() {
    SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("complexTypeMapObject");
    Map<Long, SimpleCollectionObject> simpleTypeMap = simpleCollectionObject.getComplexTypeMap();
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

  private void assertSimpleTypeCollectionValues(Collection<String> simpleTypeCollectionValues) {
    assertThat(simpleTypeCollectionValues.size(), is(2));
    assertThat(simpleTypeCollectionValues, hasItems("value1", "value2"));
  }

  private void assertCollectionChildrenContent(List<SimpleCollectionObject> collectionObjects) {
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
