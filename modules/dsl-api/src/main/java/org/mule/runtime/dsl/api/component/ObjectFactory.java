/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.dsl.api.component;

import org.mule.runtime.api.component.Component;

/**
 * Interface that must be implemented by those classes that are meant to be used as a factory to create complex domain objects.
 *
 * This object may have a complex construction with setters and constructor parameters which are going to be defined by the
 * {@link ComponentBuildingDefinition} that use them.
 * <p>
 * Implementations of ObjectFactory can be annotated with @Inject at the field level to get dependency injection that can later be
 * used for the purpose of creating the object with {@code getObject} method. Objects created with the ObjectFactory will not have
 * dependency injection support.
 * <p>
 * ObjectFactories do not support any lifecycle method but the object created through {@code getObject} may implement lifecycle
 * interfaces.
 *
 * @param <T> the type of the object to be created. The type parameter will be used to find out if the object implements
 *        interfaces related to mule like lifecycle interfaces. If the type could not be known in advance and can only be know at
 *        runtime then implement {@link ObjectTypeProvider} to inform the actual type.
 *
 * @since 4.0
 */
public interface ObjectFactory<T> extends Component {

  /**
   * @return the created object
   * 
   * @throws Exception any failure that may occur building the object
   */
  T getObject() throws Exception;

}
