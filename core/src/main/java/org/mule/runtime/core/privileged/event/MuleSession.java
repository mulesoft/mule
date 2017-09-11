/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.event;

import org.mule.runtime.api.metadata.DataType;

import java.io.Serializable;
import java.util.Set;

/**
 * <code>MuleSession</code> is the context in which a request is processed by Mule. The scope of the MuleSession context includes
 * all Mule Flows and Services that the request is routed through on the same or different Mule instances. A MuleSession instance
 * has a unique id, session scope properties and an optional security context.
 * <p>
 * In order for the session to be propagated from one Flow or Service to the next a transports that support message properties
 * needs to be used.
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface MuleSession extends Serializable {

  /**
   * Will set a session scope property.
   * 
   * @param key the key for the object data being stored on the session
   * @param value the value of the session data
   */
  void setProperty(String key, Serializable value);

  /**
   * Will set a session scope property.
   *
   * @param key the key for the object data being stored on the session
   * @param value the value of the session data
   * @param dataType the data type for the property value
   */
  void setProperty(String key, Serializable value, DataType dataType);

  @Deprecated
  void setProperty(String key, Object value);

  @Deprecated
  void setProperty(String key, Object value, DataType dataType);


  /**
   * Will retrieve a session scope property.
   * 
   * @param key the key for the object data being stored on the session
   * @return the value of the session data or null if the property does not exist
   */
  Object getProperty(String key);

  /**
   * Will retrieve a session scope property and remove it from the session
   * 
   * @param key the key for the object data being stored on the session
   * @return the value of the session data or null if the property does not exist
   */
  Object removeProperty(String key);

  /**
   * @return property keys for all session properties
   */
  Set<String> getPropertyNamesAsSet();

  /**
   * Merge current session with an updated version Result session will contain all the properties from updatedSession plus those
   * properties in the current session that couldn't be serialized In case updatedSession is null, then no change will be applied.
   * 
   * @param updatedSession mule session with updated properties
   */
  void merge(MuleSession updatedSession);

  void clearProperties();

  /**
   * Retrieves a session scope property data type
   *
   * @param name the name for the session property
   * @return the property data type or null if the property does not exist
   */
  DataType getPropertyDataType(String name);
}
