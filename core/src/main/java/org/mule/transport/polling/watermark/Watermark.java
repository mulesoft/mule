/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark;

import org.mule.api.MuleEvent;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.polling.MessageProcessorPollingOverride;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Poll override that provides watermarking functionality: It enriches the event
 * passed to the polling message processor to have a flow variable fetched from the
 * object store defined, ensures the variable is carried over to the flow event if it
 * is going to be executed, and saves the value of the variable back to the object
 * store at the end of the flow.
 * <p>
 * A limitation of this implementation is that the poll must be embedded into a
 * synchronous flow. An exception will be thrown if this is not the case.
 * </p>
 * 
 * @since 3.5.0
 */
public abstract class Watermark extends MessageProcessorPollingOverride
{
    /**
     * Logger to notify errors.
     */
    private static final Logger logger = LoggerFactory.getLogger(Watermark.class);

    /**
     * The watermark variable that will end up being the object store key. This
     * variable is also the name of the flow variable in the flow construct.
     */
    private final String variable;

    /**
     * The default expression to update the flow variable in case it is not in the
     * object store or it fails to retrieve it.
     */
    private final String defaultExpression;

    /**
     * The object store instance. The default value is the persistent user object
     * store.
     */
    private final ObjectStore<Serializable> objectStore;

    /**
     * The watermark annotations added to the definition
     */
    protected Map<QName, Object> annotations = new HashMap<QName, Object>();

    public Watermark(ObjectStore<Serializable> objectStore, String variable, String defaultExpression)
    {
        this.objectStore = objectStore;
        this.variable = variable;
        this.defaultExpression = defaultExpression;
    }

    protected String resolveVariable(MuleEvent event)
    {
        try
        {
            return WatermarkUtils.evaluate(variable, event).toString();
        }
        catch (NotSerializableException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Retrieves the watermark value from the underlying peristent store and enriches
     * the event.If there is no value stored, a default expression will be used to
     * create a new one.
     */
    public void putInto(MuleEvent event) throws ObjectStoreException
    {
        String resolvedVariable = resolveVariable(event);
        Serializable watermarkValue = null;

        try
        {
            watermarkValue = objectStore.retrieve(resolvedVariable);
        }
        catch (ObjectDoesNotExistException ex)
        {
            try
            {
                watermarkValue = WatermarkUtils.evaluate(defaultExpression, event);
            }
            catch (NotSerializableException nse)
            {
                logger.warn(String.format(
                    "Default watermark expression '%s' returned not serializable value",
                    this.defaultExpression), nse);
            }
        }
        if (watermarkValue != null)
        {
            event.setFlowVariable(resolvedVariable, watermarkValue);
        }
        else
        {
            logger.warn(CoreMessages.nullWatermark().getMessage());
        }
    }

    public final void updateWith(MuleEvent event, Serializable newValue) throws ObjectStoreException
    {
        if (!this.validateNewWatermarkValue(newValue))
        {
            return;
        }

        String variableName = this.resolveVariable(event);

        synchronized (objectStore)
        {
            if (objectStore.contains(variableName))
            {
                objectStore.remove(variableName);
            }
            if (newValue != null)
            {
                objectStore.store(variableName, newValue);
            }
        }
    }

    /**
     * Updates the watermark in persistent storage based on the flow variable defined
     * in the event
     *
     * @param event The event containing the watermark as a flow variable
     */
    public final void updateFrom(MuleEvent event) throws ObjectStoreException
    {
        try
        {
            Object watermarkValue = this.getUpdatedValue(event);

            this.validateNewWatermarkValue(watermarkValue);

            if (watermarkValue instanceof Serializable)
            {
                this.updateWith(event, (Serializable) watermarkValue);
            }
            else
            {
                throw new IllegalArgumentException(CoreMessages.notSerializableWatermark(this.resolveVariable(event))
                                                           .getMessage());
            }
        }
        catch (Exception e)
        {
            logger.error("Exception found updating watermark", e);
        }
    }

    /**
     * This method is executed once the flow containing the poll has been executed.
     * This method must return the watermark's new value
     * 
     * @param event the {@link MuleEvent} that was returned by the owning flow
     * @return the new watermark value
     */
    protected abstract Object getUpdatedValue(MuleEvent event);

    private boolean validateNewWatermarkValue(Object value)
    {
        if (value == null)
        {
            if (logger.isInfoEnabled())
            {
                logger.info(CoreMessages.nullWatermark().getMessage());
            }
            return false;
        }

        return true;
    }

}
