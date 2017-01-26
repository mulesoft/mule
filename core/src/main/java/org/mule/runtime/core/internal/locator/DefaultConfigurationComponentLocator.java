/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.locator;

import static java.lang.String.format;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.exception.ObjectNotFoundException;
import org.mule.runtime.core.api.locator.ConfigurationComponentLocator;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.util.ArrayUtils;

import java.util.List;

import javax.inject.Inject;

/**
 * Default implementation of {@link ConfigurationComponentLocator}
 *
 * @since 4.0
 */
public class DefaultConfigurationComponentLocator implements ConfigurationComponentLocator {

  private final MuleContext muleContext;

  @Inject
  public DefaultConfigurationComponentLocator(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public Object findByName(String name) throws ObjectNotFoundException {
    Object object = muleContext.getRegistry().lookupObject(name);
    validateObjectIsPresent(name, object);
    return object;
  }

  private void validateObjectIsPresent(String name, Object object) {
    if (object == null) {
      throw new ObjectNotFoundException(name);
    }
  }

  //TODO MULE-10751 - add complete support for componentPath
  @Override
  public Object findByPath(String componentPath) {
    String[] parts = componentPath.split("/");
    if (parts.length == 0) {
      throw new IllegalArgumentException("Incomplete component path " + componentPath);
    }
    String type = parts[0];
    if (!"flow".equals(type)) {
      throw new IllegalArgumentException("Unsupported component type " + type);
    }
    if (parts.length == 1) {
      throw new IllegalArgumentException("Missing component path type element name in " + componentPath);
    }
    String typeElementName = parts[1];
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(typeElementName);
    validateObjectIsPresent(typeElementName, flow);
    if (parts.length == 2) {
      return flow;
    }
    String flowPartType = parts[2];
    if ("source".equals(flowPartType)) {
      return flow.getMessageSource();
    }
    if ("processors".equals(flowPartType)) {
      return resolveMessageProcessor(flow.getMessageProcessors(), ArrayUtils.subarray(parts, 4, parts.length - 1));
    }
    if ("errorHandler".equals(flowPartType)) {
      throw new IllegalArgumentException("Cannot resolve processors inside error-handler yet");
    }
    throw new IllegalArgumentException(format("Flow part %s is invalid", flowPartType));
  }

  private Object resolveMessageProcessor(List<Processor> messageProcessors, Object[] processorIndexes) {
    if (processorIndexes.length > 1) {
      throw new IllegalArgumentException("Only one level of processor indexes is supported");
    }
    return messageProcessors.get(Integer.valueOf((String) processorIndexes[0]));
  }
}
