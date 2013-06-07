
package org.mule.security.oauth.processor;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.config.ConfigurationException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.TransformerTemplate;
import org.mule.transport.NullPayload;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDevkitBasedMessageProcessor<O> extends AbstractConnectedProcessor
    implements FlowConstructAware, MuleContextAware
{

    /**
     * Module object
     */
    protected O moduleObject;
    /**
     * Mule Context
     */
    protected MuleContext muleContext;

    /**
     * Flow Construct
     */
    protected FlowConstruct flowConstruct;

    /**
     * Sets muleContext
     * 
     * @param value Value to set
     */
    public void setMuleContext(MuleContext value)
    {
        this.muleContext = value;
    }

    /**
     * Retrieves muleContext
     */
    public MuleContext getMuleContext()
    {
        return this.muleContext;
    }

    /**
     * Sets flowConstruct
     * 
     * @param value Value to set
     */
    public void setFlowConstruct(FlowConstruct value)
    {
        this.flowConstruct = value;
    }

    /**
     * Retrieves flowConstruct
     */
    public FlowConstruct getFlowConstruct()
    {
        return this.flowConstruct;
    }

    /**
     * Sets moduleObject
     * 
     * @param value Value to set
     */
    public void setModuleObject(O value)
    {
        this.moduleObject = value;
    }

    /**
     * Obtains the expression manager from the Mule context and initialises the
     * connector. If a target object has not been set already it will search the Mule
     * registry for a default one.
     * 
     * @throws InstantiationException
     * @throws ConfigurationException
     * @throws IllegalAccessException
     * @throws RegistrationException
     */
    @SuppressWarnings("unchecked")
    protected O findOrCreate(Class<O> moduleClass, boolean shouldAutoCreate, MuleEvent muleEvent)
        throws IllegalAccessException, InstantiationException, ConfigurationException, RegistrationException
    {
        Object temporaryObject = moduleObject;
        if (temporaryObject == null)
        {
            temporaryObject = ((O) muleContext.getRegistry().lookupObject(moduleClass));
            if (temporaryObject == null)
            {
                if (shouldAutoCreate)
                {
                    temporaryObject = ((O) moduleClass.newInstance());
                    muleContext.getRegistry().registerObject(moduleClass.getName(), temporaryObject);
                }
                else
                {
                    throw new ConfigurationException(MessageFactory.createStaticMessage("Cannot find object"));
                }
            }
        }
        if (temporaryObject instanceof String)
        {
            temporaryObject = ((O) muleContext.getExpressionManager().evaluate(((String) temporaryObject),
                muleEvent, true));
            if (temporaryObject == null)
            {
                throw new ConfigurationException(
                    MessageFactory.createStaticMessage("Cannot find object by config name"));
            }
        }
        return ((O) temporaryObject);
    }

    /**
     * Overwrites the event payload with the specified one
     */
    public void overwritePayload(MuleEvent event, Object resultPayload) throws Exception
    {
        TransformerTemplate.OverwitePayloadCallback overwritePayloadCallback = null;
        if (resultPayload == null)
        {
            overwritePayloadCallback = new TransformerTemplate.OverwitePayloadCallback(
                NullPayload.getInstance());
        }
        else
        {
            overwritePayloadCallback = new TransformerTemplate.OverwitePayloadCallback(resultPayload);
        }
        List<Transformer> transformerList;
        transformerList = new ArrayList<Transformer>();
        transformerList.add(new TransformerTemplate(overwritePayloadCallback));
        event.getMessage().applyTransformers(event, transformerList);
    }

}
