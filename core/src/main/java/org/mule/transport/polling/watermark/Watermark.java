/*
 * $Id\$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.polling.watermark;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.polling.MessageProcessorPollingInterceptor;
import org.mule.transport.polling.MessageProcessorPollingOverride;
import org.mule.util.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Poll override that provides watermarking functionality:
 * It enriches the event passed to the polling message processor to have a flow variable fetched from the object store defined,
 * ensures the variable is carried over to the flow event if it is going to be executed, and saves the value of the variable
 * back to the object store at the end of the flow.
 * <p>
 *     A limitation of this implementation is that the poll must be embedded into a synchronous flow. An exception will be thrown if
 * this is not the case.
 * </p>
 */
public class Watermark extends MessageProcessorPollingOverride
{
    /**
     * Logger to notify errors.
     */
    private static Log logger = LogFactory.getLog(Watermark.class);

    /**
     * The watermark variable that will end up being the object store key. This variable is also the name of the flow
     * variable in the flow construct.
     */
    private final String variable;


    /**
     * The default expression to update the flow variable in case it is not in the object store or it fails to retrieve
     * it.
     */
    private final String defaultExpression;

    /**
     * The update expression to update the watermark value in the object store.
     * It is optional so it can be null.
     */
    private final String updateExpression;

    /**
     * The object store instance.
     * The default value is the persistent user object store.
     */
    private final ObjectStore objectStore;

    /**
     * The watermark annotations added to the definition
     */
    protected Map<QName, Object> annotations = new HashMap<QName, Object>();


    public Watermark(ObjectStore objectStore, String variable, String defaultExpression, String updateExpression)
    {
        this.objectStore = objectStore;
        this.variable = variable;
        this.defaultExpression = defaultExpression;
        this.updateExpression = updateExpression;
    }


    final String resolveVariable(MuleEvent event) {
        return evaluate(variable, event).toString();
    }

    /**
     * Evaluates a mel expression. If the value is not an expression or if it is not valid then returns the same value.
     * The expression is expected to
     *
     * @param value The expression the user wrote in the xml. Can be an expression or not
     * @param event The mule event in which we need to evaluate the expression
     * @return The evaluated value
     */
    private Serializable evaluate(String value, MuleEvent event)
    {
        ExpressionManager expressionManager = event.getMuleContext().getExpressionManager();
        if (expressionManager.isExpression(value) && expressionManager.isValidExpression(value)) {
            Object evaluated = expressionManager.evaluate(value, event);
            if (! (evaluated instanceof Serializable)) {
                throw new IllegalArgumentException("Expression " + value + " resolves to an object that is not serializable. It can't be used as watermark.");
            }
            return (Serializable) evaluated;
        }
        return value;
    }

    /**
     * Retrieves the watermark value from the underlying peristent store and enriches the
     * event.If there is no value stored, a default expression will be used to create a new one.
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
            watermarkValue = evaluate(defaultExpression, event);
        }
        if (watermarkValue != null)
        {
            event.setFlowVariable(resolvedVariable, watermarkValue);
        }
    }

    /**
     * Updates the watermark in persistent storage based on the flow variable defined in the event
     * @param event Th event containing the watermark as a flow variable
     */
    public void updateFrom(MuleEvent event) throws ObjectStoreException
    {
        String resolvedVariable = resolveVariable(event);

        Object watermarkValue = StringUtils.isEmpty(updateExpression)
                ? event.getFlowVariable(resolvedVariable)
                : evaluate(updateExpression, event);

        // TODO: externalize serializable check
        if (watermarkValue instanceof Serializable)
        {
            synchronized (objectStore)
            {
                if (objectStore.contains(resolvedVariable))
                {
                    objectStore.remove(resolvedVariable);
                }
                if (watermarkValue != null)
                {
                    objectStore.store(resolvedVariable, (Serializable) watermarkValue);
                }
            }
        }
        else
        {
            logger.error("Value retrieved from event is not serializable and hence can't be saved to the object store");
        }
    }

    @Override
    public MessageProcessorPollingInterceptor interceptor()
    {
        return new MessageProcessorPollingInterceptor()
        {
            /**
             * Watermark source preprocessing puts the watermark value into a flow variable
             */
            @Override
            public MuleEvent prepareSourceEvent(MuleEvent event) throws MuleException {
                putInto(event);
                return event;
            }

            /**
             * Watermark route preparation carries the value from the source event to the flow event
             */
            @Override
            public MuleEvent prepareRouting(MuleEvent sourceEvent, MuleEvent event) throws ConfigurationException {
                if (!event.isSynchronous()) {
                    throw new ConfigurationException(CoreMessages.watermarkRequiresSynchronousProcessing());
                }
                String var = resolveVariable(event);
                event.setFlowVariable(var, sourceEvent.getFlowVariable(var));
                return event;
            }

            /**
             * Watermark post processing saves the flow variable to the object store
             */
            @Override
            public void postProcessRouting(MuleEvent event) throws ObjectStoreException {
                updateFrom(event);
            }
        };
    }
}