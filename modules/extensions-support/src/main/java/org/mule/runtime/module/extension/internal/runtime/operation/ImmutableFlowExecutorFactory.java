/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.extension.api.runtime.process.FlowExecutor;
import org.mule.runtime.extension.api.runtime.process.FlowExecutorFactory;
import org.mule.runtime.extension.api.runtime.process.FlowNotFoundException;
import org.mule.runtime.extension.api.runtime.process.InvalidFlowReferenceException;

/**
 * Immutable default implementation of a {@link FlowExecutorFactory}
 *
 * @since 4.1
 */
public final class ImmutableFlowExecutorFactory implements FlowExecutorFactory {

  private final CoreEvent event;
  private final ConfigurationComponentLocator locator;

  public ImmutableFlowExecutorFactory(CoreEvent event, ConfigurationComponentLocator locator) {
    this.event = event;
    this.locator = locator;
  }

  @Override
  public FlowExecutor newExecutor(String flowName) throws FlowNotFoundException, InvalidFlowReferenceException {
    checkArgument(!StringUtils.isBlank(flowName), "Empty string is not a valid flow name");

    Component component = locator.find(Location.builder().globalName(flowName).build())
        .orElseThrow(() -> new FlowNotFoundException(createStaticMessage("No Flow found with name [%s]", flowName)));

    if (component instanceof Flow) {
      return new ImmutableFlowExecutor(this.event, (Flow) component);
    }

    ComponentLocation location = component.getLocation();
    if (location == null || location.getComponentIdentifier() == null || location.getComponentIdentifier().getType() == null) {
      throw new InvalidFlowReferenceException(createStaticMessage("Component with name [%s] is not a Flow", flowName), null);
    }

    TypedComponentIdentifier.ComponentType type = location.getComponentIdentifier().getType();
    throw new InvalidFlowReferenceException(createStaticMessage("Component with name [%s] expected to be a Flow but was [%s]",
                                                                flowName, type.name()),
                                            type);
  }

}
