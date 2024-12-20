/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.TypeConverter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import io.qameta.allure.Issue;

public class ComponentBuildingDefinitionRegistryTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("W-17484538")
  public void lastRegisteredTakesPrecedence() {
    final ComponentBuildingDefinitionRegistry registry = new ComponentBuildingDefinitionRegistry();

    final ComponentIdentifier collidingComponentIdentifier = buildFromStringRepresentation("ext:comp");

    final ComponentBuildingDefinition definition1 = mock(ComponentBuildingDefinition.class);
    when(definition1.getComponentIdentifier()).thenReturn(collidingComponentIdentifier);
    registry.register(definition1);

    final ComponentBuildingDefinition definition2 = mock(ComponentBuildingDefinition.class);
    when(definition2.getComponentIdentifier()).thenReturn(collidingComponentIdentifier);
    registry.register(definition2);

    assertThat(registry.getBuildingDefinition(collidingComponentIdentifier).get(),
               sameInstance(definition2));
  }

  @Test
  @Issue("W-17484538")
  public void lastRegisteredTakesPrecedenceWithPredicate() {
    final ComponentBuildingDefinitionRegistry registry = new ComponentBuildingDefinitionRegistry();

    final ComponentIdentifier collidingComponentIdentifier = buildFromStringRepresentation("ext:comp");

    final ComponentBuildingDefinition definition1 = mock(ComponentBuildingDefinition.class);
    when(definition1.getComponentIdentifier()).thenReturn(collidingComponentIdentifier);
    registry.register(definition1);

    final ComponentBuildingDefinition definition2 = mock(ComponentBuildingDefinition.class);
    when(definition2.getComponentIdentifier()).thenReturn(collidingComponentIdentifier);
    registry.register(definition2);

    assertThat(registry.getBuildingDefinition(collidingComponentIdentifier, d -> true).get(),
               sameInstance(definition2));
  }

  @Test
  @Issue("W-17484538")
  public void getRegisteredWithPredicate() {
    final ComponentBuildingDefinitionRegistry registry = new ComponentBuildingDefinitionRegistry();

    final ComponentIdentifier collidingComponentIdentifier = buildFromStringRepresentation("ext:comp");

    final ComponentBuildingDefinition definition1 = mock(ComponentBuildingDefinition.class);
    when(definition1.getComponentIdentifier()).thenReturn(collidingComponentIdentifier);
    when(definition1.getTypeConverter()).thenReturn(of(mock(TypeConverter.class)));
    registry.register(definition1);

    final ComponentBuildingDefinition definition2 = mock(ComponentBuildingDefinition.class);
    when(definition2.getComponentIdentifier()).thenReturn(collidingComponentIdentifier);
    when(definition2.getTypeConverter()).thenReturn(empty());
    registry.register(definition2);

    assertThat(registry.getBuildingDefinition(collidingComponentIdentifier, d -> d.getTypeConverter().isPresent()).get(),
               sameInstance(definition1));
  }
}
