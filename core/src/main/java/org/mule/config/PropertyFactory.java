/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config;

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
    Object create(Map properties) throws Exception;
}
