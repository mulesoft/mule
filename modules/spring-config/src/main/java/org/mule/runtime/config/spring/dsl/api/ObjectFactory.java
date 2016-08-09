/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.dsl.api;

/**
 * Interface that must be implemented by those classes that are meant to be used as a factory to create complex domain objects.
 *
 * This object may have a complex construction with setters and constructor parameters which are going to be defined by the
 * {@link org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition} that use them.
 *
 * @param <T> the type of the object to be created.
 *
 * @since 4.0
 */
public interface ObjectFactory<T> {

  /**
   * @return the domain object
   * @throws Exception any failure that may occur building the object
   */
  T getObject() throws Exception;

}
