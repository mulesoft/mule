/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.config;

import java.util.Map;

/**
 * <code>PropertyFactory</code> is used to create objects from the property file.
 * PropertyFactories map to <factory-property> elements in the MuleXml config.
 */
public interface PropertyFactory
{
    /**
     * Creates a property using code execution.
     * 
     * @param properties The map of properties preceeding this <factory-property>
     * @return an object that will become the value of a property with a name that
     *         matches the 'name' attribute on the <factory-property> element.
     * @throws Exception
     */
    Object create(Map<?, ?> properties) throws Exception;
}
