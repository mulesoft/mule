package org.mule.runtime.modules.schedulers.config;

import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.modules.schedulers.cron.CronScheduler;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link ComponentBuildingDefinition} definitions for the components provided by the schedulers module.
 *
 * @since 4.0
 */
public class SchedulersComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  public static final String SCHEDULERS_NAMESPACE = "scheduler";
  private static final String CRON = "cron-scheduler";

  private static ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withNamespace(SCHEDULERS_NAMESPACE);

  @Override public void init() {

  }

  @Override public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    LinkedList<ComponentBuildingDefinition> componentBuildingDefinitions = new LinkedList<>();

    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(CRON).withTypeDefinition(fromType(CronScheduler.class))
             .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build())
             .withSetterParameterDefinition("timeZone", fromSimpleParameter("timeZone").build()).build());

    return componentBuildingDefinitions;
  }
}
