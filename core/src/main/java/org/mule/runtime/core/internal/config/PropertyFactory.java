/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.config;

import java.util.Map;

/**
 * <code>PropertyFactory</code> is used to create objects from the property file. PropertyFactories map to <factory-property>
 * elements in the MuleXml config.
 */
public interface PropertyFactory {

  /**
   * Creates a property using code execution.
   * 
   * @param properties The map of properties preceeding this <factory-property>
   * @return an object that will become the value of a property with a name that matches the 'name' attribute on the
   *         <factory-property> element.
   * @throws Exception
   */
  Object create(Map<?, ?> properties) throws Exception;
}
