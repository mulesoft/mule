/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.metadata.DataType;

import java.io.Serializable;
import java.util.Map;

/**
 *
 */
public interface MutableMessageProperties extends MessageProperties
{

    /**
     * Set an outbound property on the message.
     *
     * @param key the key on which to associate the value
     * @param value the property value
     */
    void setOutboundProperty(String key, Serializable value);

    /**
     * Set an outbound property on the message with a given {@link DataType}
     *
     * @param key the key on which to associate the value
     * @param value the property value
     * @param dataType the dataType of the property being set
     */
    <T extends Serializable> void setOutboundProperty(String key, T value, DataType<T> dataType);

    /**
     * Adds a map of outbound properties to be associated with this message
     *
     * @param properties the outbund properties add to this message
     */
    void addOutboundProperties(Map<String, Serializable> properties);


    /**
     * Removes an outbound property on this message.
     *
     * @param key the outbound property key to remove
     * @return the removed property value or null if the property did not exist
     */
    <T extends Serializable> T removeOutboundProperty(String key);

    /**
     * Removes all outbound properties on this message.
     */
    void clearOutboundProperties();

    /**
     * Set an inbound property on the message.
     *
     * @param key the key on which to associate the value
     * @param value the property value
     */
    void setInboundProperty(String key, Serializable value);

    /**
     * Set an inbound property on the message with a given {@link DataType}
     *
     * @param key the key on which to associate the value
     * @param value the property value
     * @param dataType the dataType of the property being set
     */
    <T extends Serializable> void setInboundProperty(String key, T value, DataType<T> dataType);

    /**
     * Removes an inbound property on this message.
     *
     * @param key the inbound property key to remove
     * @return the removed property value or null if the property did not exist
     */
    <T extends Serializable> T removeInboundProperty(String key);

    /**
     * Copy property with the given key from inbound to outbound scope.
     *
     * @param key the inbound property key to copy to outbound
     */
    void copyProperty(String key);


}
