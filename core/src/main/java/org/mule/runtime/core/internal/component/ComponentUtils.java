/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.component;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.construct.FlowConstruct;

import java.util.Optional;

/**
 * Utility methods for components within the runtime.
 *
 * 4.0
 */
public interface ComponentUtils {

  /**
   * Returns the {@link FlowConstruct} of the root container element if there is one. Otherwise return {@link Optional#empty()}
   *
   * @param componentLocator the locator for the {@link FlowConstruct}
   * @param component the component that may be configured within a {@link FlowConstruct}
   * @return the {@link FlowConstruct} of the root container element if there is one. Otherwise return {@link Optional#empty()}
   */
  static Optional<FlowConstruct> getFromAnnotatedObject(ConfigurationComponentLocator componentLocator,
                                                        Component component) {
    Optional<Component> objectFoundOptional = componentLocator.find(component.getRootContainerLocation());
    Optional<FlowConstruct> flowConstruct = objectFoundOptional.flatMap(objectFound -> objectFound instanceof FlowConstruct
        ? of((FlowConstruct) objectFound) : empty()).filter(object -> object != null);

    return flowConstruct;
  }

  /**
   * Returns the {@link FlowConstruct} of the root container element. If the root container is not a {@link FlowConstruct} then it
   * fails.
   *
   * @param componentLocator the locator for the {@link FlowConstruct}
   * @param component the component that may be configured within a {@link FlowConstruct}
   * @return the {@link FlowConstruct} of the root container element. If the root container is not a {@link FlowConstruct} then it
   *         fails.
   */
  static FlowConstruct getFromAnnotatedObjectOrFail(ConfigurationComponentLocator componentLocator,
                                                    Component component) {
    return getFromAnnotatedObject(componentLocator, component)
        .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format(
                                                                               "Couldn't find FlowConstruct with global name %s or it was not an instance of FlowConstruct",
                                                                               component.getRootContainerLocation()
                                                                                   .toString()))));
  }

}
