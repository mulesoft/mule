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

import static java.util.Collections.emptyList;
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

import io.qameta.allure.Issue;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public class BeanDefinitionFactoryTestCase extends AbstractMuleTestCase {

  private static Map<?, ?> descriptorsCache;

  private ComponentBuildingDefinition<?> buildingDefinition;
  private BeanDefinitionRegistry beanDefinitionRegistry;
  private ComponentAst componentAst;

  @BeforeClass
  public static void setUpClass() throws Exception {
    Field descriptorsCacheField = PropertyUtilsBean.class.getDeclaredField("descriptorsCache");
    descriptorsCacheField.setAccessible(true);
    PropertyUtilsBean propertyUtilsBean = BeanUtilsBean.getInstance().getPropertyUtils();

    descriptorsCache = (Map<?, ?>) descriptorsCacheField.get(propertyUtilsBean);
  }

  @Before
  public void setUp() {
    // A mock identifier.
    ComponentIdentifier componentIdentifier = buildFromStringRepresentation("mock:component");

    // To trigger the EagerObjectCreator usage, we need a setter for an undefined simple attribute.
    SetterAttributeDefinition setterDef = new SetterAttributeDefinition("attr", fromUndefinedSimpleAttributes().build());

    // The mock building definition.
    buildingDefinition = mock(ComponentBuildingDefinition.class);
    when(buildingDefinition.getComponentIdentifier()).thenReturn(componentIdentifier);
    when(buildingDefinition.getSetterParameterDefinitions()).thenReturn(singletonList(setterDef));
    when(buildingDefinition.getTypeDefinition()).thenReturn(fromType(TestObjectProvider.class));

    // Create a Spring component model with the test type.
    SpringComponentModel springComponentModel = new SpringComponentModel();
    springComponentModel.setType(TestObjectProvider.class);

    // Create mock AST
    componentAst = mock(ComponentAst.class);
    ComponentLocation location = mock(ComponentLocation.class);
    when(componentAst.getIdentifier()).thenReturn(componentIdentifier);
    when(componentAst.getMetadata()).thenReturn(EMPTY_METADATA);
    when(componentAst.getLocation()).thenReturn(location);
    springComponentModel.setComponent(componentAst);

    beanDefinitionRegistry = mock(BeanDefinitionRegistry.class);
  }

  @Test
  @Issue("W-18350339")
  public void testEagerObjectCreation() {
    // Registry with only the test building definition.
    ComponentBuildingDefinitionRegistry definitionRegistry = new ComponentBuildingDefinitionRegistry();
    definitionRegistry.register(buildingDefinition);

    // Create a factory.
    BeanDefinitionFactory factory =
        new BeanDefinitionFactory("test-artifact", definitionRegistry, false, false);

    // Resolve the component for the test bean's AST.
    factory.resolveComponent(new HashMap<>(), emptyList(), componentAst, beanDefinitionRegistry,
                             new SpringConfigurationComponentLocator());

    // Check that the descriptorsCache has the entry for our test class
    assertThat("descriptorsCache should have the test entry", descriptorsCache, hasKey(TestObjectProvider.class));

    factory.close();
    assertThat("descriptorsCache should be empty", descriptorsCache, is(anEmptyMap()));
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
