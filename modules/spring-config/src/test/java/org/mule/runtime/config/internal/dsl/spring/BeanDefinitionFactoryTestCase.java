/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.ast.api.ComponentMetadataAst.EMPTY_METADATA;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromUndefinedSimpleAttributes;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import static java.util.Collections.singletonList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.ioc.ConfigurableObjectProvider;
import org.mule.runtime.api.ioc.ObjectProviderConfiguration;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.internal.context.SpringConfigurationComponentLocator;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.SetterAttributeDefinition;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public class BeanDefinitionFactoryTestCase extends AbstractMuleTestCase {

  private static Field descriptorsCacheField;
  private static PropertyUtilsBean propertyUtilsBean;

  // A test class extending ConfigurableObjectProvider
  private Class<?> clazz = TestObjectProvider.class;

  private ComponentBuildingDefinitionRegistry definitionRegistry;
  private BeanDefinitionRegistry registry;
  private Map<ComponentAst, SpringComponentModel> springComponentModels;
  private ComponentAst ast;

  @BeforeClass
  public static void setUpClass() throws Exception {
    descriptorsCacheField = PropertyUtilsBean.class.getDeclaredField("descriptorsCache");
    descriptorsCacheField.setAccessible(true);
    propertyUtilsBean = BeanUtilsBean.getInstance().getPropertyUtils();
  }

  @Before
  public void setUp() throws Exception {
    // A mock identifier
    ComponentIdentifier componentIdentifier = buildFromStringRepresentation("test:component");

    // To trigger the EagerObjectCreator usage, we need a setter for an undefined simple attribute
    SetterAttributeDefinition setterDef =
        new SetterAttributeDefinition("undefinedSimpleAttribute", fromUndefinedSimpleAttributes().build());

    // The mock building definition
    ComponentBuildingDefinition<?> buildingDefinition = mock(ComponentBuildingDefinition.class);
    when(buildingDefinition.getComponentIdentifier()).thenReturn(componentIdentifier);
    when(buildingDefinition.getSetterParameterDefinitions()).thenReturn(singletonList(setterDef));
    when(buildingDefinition.getTypeDefinition()).thenReturn(fromType(clazz));

    // Create the registry containing the above definition
    definitionRegistry = new ComponentBuildingDefinitionRegistry();
    definitionRegistry.register(buildingDefinition);

    // Create Spring component model
    SpringComponentModel componentModel = new SpringComponentModel();
    componentModel.setType(clazz);

    // Create mock AST
    ast = mock(ComponentAst.class);
    ComponentLocation location = mock(ComponentLocation.class);
    when(ast.getIdentifier()).thenReturn(componentIdentifier);
    when(ast.getMetadata()).thenReturn(EMPTY_METADATA);
    when(ast.getLocation()).thenReturn(location);
    componentModel.setComponent(ast);

    registry = mock(BeanDefinitionRegistry.class);
    springComponentModels = new HashMap<>();
  }

  @Test
  public void testEagerObjectCreation() throws Exception {
    // Create the factory
    BeanDefinitionFactory factory =
        new BeanDefinitionFactory("test-artifact", definitionRegistry, false, false);

    // Process the request
    factory.resolveComponent(springComponentModels,
                             Collections.emptyList(),
                             ast,
                             registry,
                             new SpringConfigurationComponentLocator());

    // Check that the descriptorsCache has one entry
    Map<?, ?> descriptorsCache = getCachedDescriptors();
    assertThat("descriptorsCache should have the test entry", descriptorsCache, hasKey(clazz));

    factory.close();
    assertThat("descriptorsCache should be empty", descriptorsCache, is(anEmptyMap()));
  }

  private static Map<?, ?> getCachedDescriptors() throws IllegalAccessException {
    return (Map<?, ?>) descriptorsCacheField.get(propertyUtilsBean);
  }

  public static class TestObjectProvider implements ConfigurableObjectProvider {

    @Override
    public Optional<Object> getObject(String s) {
      return Optional.empty();
    }

    @Override
    public Optional<Object> getObjectByType(Class<?> type) {
      return Optional.empty();
    }

    @Override
    public <T> Map<String, T> getObjectsByType(Class<T> type) {
      return Collections.emptyMap();
    }

    @Override
    public boolean containsObject(String name) {
      return false;
    }

    @Override
    public Optional<Boolean> isObjectSingleton(String name) {
      return Optional.of(false);
    }

    @Override
    public ComponentLocation getLocation() {
      return null;
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return Collections.emptyMap();
    }

    @Override
    public Object getAnnotation(QName name) {
      return null;
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations) {}

    @Override
    public Location getRootContainerLocation() {
      return null;
    }

    @Override
    public void configure(ObjectProviderConfiguration objectProviderConfiguration) {}
  }
}
