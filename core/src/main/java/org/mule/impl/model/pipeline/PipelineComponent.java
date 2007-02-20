/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.pipeline;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.impl.model.direct.DirectComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.DispatchException;

public class PipelineComponent extends DirectComponent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2788210157354765190L;

    private Callable callable;

    public PipelineComponent(MuleDescriptor descriptor, UMOModel model)
    {
        super(descriptor, model);
    }

    protected void doInitialise() throws InitialisationException
    {

        super.doInitialise();
        Object component = null;
        try
        {
            component = lookupComponent();
        }
        catch (UMOException e)
        {
            throw new InitialisationException(e, this);
        }
        if (component instanceof Callable)
        {
            callable = (Callable)component;
        }
        else
        {
            throw new InitialisationException(new Message(Messages.OBJECT_X_NOT_OF_CORRECT_TYPE_SHOULD_BE_X,
                component.getClass().getName(), Callable.class.getName()), this);
        }

        if (component instanceof Initialisable)
        {
            ((Initialisable)component).initialise(managementContext);
        }

    }

    protected UMOMessage doSend(UMOEvent event) throws UMOException
    {
        try
        {
            Object result = callable.onCall(RequestContext.getEventContext());
            UMOMessage returnMessage = null;
            if (result instanceof UMOMessage)
            {
                returnMessage = (UMOMessage)result;
            }
            else
            {
                returnMessage = new MuleMessage(result, event.getMessage());
            }
            if (!event.isStopFurtherProcessing())
            {
                // // TODO what about this code?
                // Map context = RequestContext.clearProperties();
                // if (context != null) {
                // returnMessage.addProperties(context);
                // }
                if (descriptor.getOutboundRouter().hasEndpoints())
                {
                    UMOMessage outboundReturnMessage = descriptor.getOutboundRouter().route(returnMessage,
                        event.getSession(), event.isSynchronous());
                    if (outboundReturnMessage != null)
                    {
                        returnMessage = outboundReturnMessage;
                    }
                }
                else
                {
                    logger.debug("Outbound router on component '" + descriptor.getName()
                                 + "' doesn't have any endpoints configured.");
                }
            }

            return returnMessage;
        }
        catch (Exception e)
        {
            throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
        }
    }

    protected void doDispatch(UMOEvent event) throws UMOException
    {
        sendEvent(event);
    }

    protected void doDispose()
    {
        if (callable instanceof Disposable)
        {
            ((Disposable)callable).dispose();
        }
    }
}
