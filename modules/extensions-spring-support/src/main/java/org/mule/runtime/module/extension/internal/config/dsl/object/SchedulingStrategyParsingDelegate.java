/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.config.dsl.object;

import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.internal.dsl.DslConstants.SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER;

import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.scheduler.SchedulingStrategy;
import org.mule.runtime.core.api.source.scheduler.PeriodicScheduler;
import org.mule.runtime.core.api.source.scheduler.Scheduler;
import org.mule.runtime.dsl.api.component.AttributeDefinition;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;

/**
 * A {@link ObjectParsingDelegate} which only parses {@link ObjectType} which represent one specific java type.
 *
 * @since 4.0
 */
public class SchedulingStrategyParsingDelegate implements ObjectParsingDelegate {

  /**
   * Creates a new instance
   *
   * @param type the type that {@code this} delegate accepts
   */
  public SchedulingStrategyParsingDelegate() {}

  /**
   * @param objectType an {@link ObjectType}
   * @return {@code true} if the {@code objectType} represents the {@link #type}
   */
  @Override
  public boolean accepts(ObjectType objectType) {
    return getType(objectType)
        .map(type -> SchedulingStrategy.class.isAssignableFrom(type) || Scheduler.class.isAssignableFrom(type))
        .orElse(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AttributeDefinition.Builder parse(String name, ObjectType objectType, DslElementSyntax elementDsl) {
    return fromChildConfiguration(PeriodicScheduler.class)
        .withWrapperIdentifier(SCHEDULING_STRATEGY_ELEMENT_IDENTIFIER);
  }
}
