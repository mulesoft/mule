/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.locator;

/**
 * Locator to access runtime objects created from the configuration of the artifact.
 * 
 * @since 4.0
 */
public interface ConfigurationComponentLocator {

  /**
   * Finds the component created from the configuration with the provided global name.
   *
   * @param componentName name configured in the global component.
   * @return the component with the provided global name.
   * @throws org.mule.runtime.core.api.exception.ObjectNotFoundException if the component is not present in the configuration.
   */
  Object findByName(String componentName);

  /**
   * Finds the component created from the configuration with the given path.
   *
   * The path has the following syntax containerType/containerName/[containerPart]/[componentIndexLocation] where:
   * <ul>
   *     <li>
   *         containerType: can by one of flow or batch
   *         containerName: name of the container component in the configuration
   *         containerPart: part of the container. For flow it can be source, processors or errorHandler. For batch it can be inputPhase, processingPhase or onComplete.
   *         componentIndexLocation: index of the component within the container. When there's a component that can have other components as child then all the indexes must be provided
   *         separated by a '/' character
   *     </li>
   * </ul>
   * 
   * @param componentPath path to the component
   * @return component with the given path.
   * @throws org.mule.runtime.core.api.exception.ObjectNotFoundException if the component is not present in the configuration.
   */
  Object findByPath(String componentPath);

}
