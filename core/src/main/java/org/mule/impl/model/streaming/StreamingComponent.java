/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.streaming;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.RequestContext;
import org.mule.impl.model.AbstractComponent;
import org.mule.impl.model.ComponentFactory;
import org.mule.transformers.TransformerUtils;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.model.UMOEntryPointResolverSet;
import org.mule.umo.model.UMOModel;

import java.util.Iterator;

/** TODO */
public class StreamingComponent extends AbstractComponent
{

    private static final long serialVersionUID = 2967438446264425730L;

    protected Object component;

    protected UMOEntryPointResolverSet entryPointResolverSet;

    public StreamingComponent(MuleDescriptor descriptor, UMOModel model)
    {
        super(descriptor, model);
    }

    protected void doInitialise() throws InitialisationException
    {
        try
        {
            component = ComponentFactory.createService(getDescriptor());
        }
        catch (UMOException e)
        {
            throw new InitialisationException(e, this);
        }

        // Validate the component
        // Right now we do not support transformers on the Streaming model
        for (Iterator iterator = descriptor.getInboundRouter().getEndpoints().iterator(); iterator.hasNext();)
        {
            // Ensure streaming type is correct
            UMOEndpoint ep = (UMOEndpoint) iterator.next();
            if (!ep.isStreaming())
            {
                throw new InitialisationException(CoreMessages.streamingEndpointsMustBeUsedWithStreamingModel(), this);
            }
            // TODO RM*: This restriction could be lifted in future
            if (TransformerUtils.isDefined(ep.getTransformers()))
            {
                throw new InitialisationException(
                        CoreMessages.streamingEndpointsDoNotSupportTransformers(), this);
            }
        }
        if (component instanceof Initialisable)
        {
            ((Initialisable) component).initialise();
        }

        if (entryPointResolverSet == null)
        {
            entryPointResolverSet = model.getEntryPointResolverSet();
        }
    }

    protected UMOMessage doSend(UMOEvent event) throws UMOException
    {
        doDispatch(event);
        return null;
    }

    protected void doDispatch(UMOEvent event) throws UMOException
    {

        try
        {
            entryPointResolverSet.invoke(component, RequestContext.getEventContext());
        }
        catch (UMOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ComponentException(event.getMessage(), this, e);
        }
    }

    protected void doDispose()
    {
        if (component instanceof Disposable)
        {
            ((Disposable) component).dispose();
        }
    }

}
