/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax.container;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.Connector;
import org.mule.module.json.transformers.ObjectToJson;
import org.mule.transport.ajax.BayeuxAware;
import org.mule.transport.ajax.i18n.AjaxMessages;
import org.mule.transport.service.TransportFactory;
import org.mule.transport.servlet.MuleServletContextListener;
import org.mule.util.annotation.AnnotationUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cometd.Message;
import org.mortbay.cometd.AbstractBayeux;
import org.mortbay.cometd.MessageImpl;
import org.mortbay.cometd.MessagePool;
import org.mortbay.cometd.continuation.ContinuationBayeux;
import org.mortbay.cometd.continuation.ContinuationCometdServlet;

/**
 * Wraps the {@link ContinuationCometdServlet} servlet and binds the Bayeux object to
 * the Mule {@link AjaxServletConnector}.
 */
public class MuleAjaxServlet extends ContinuationCometdServlet
{
    protected Connector connector = null;

    private ObjectToJson jsonTransformer;

    private Set<Class<?>> ignoreClasses = new HashSet<Class<?>>();
    private Set<Class<?>> jsonBindings = new HashSet<Class<?>>();
    private MuleContext muleContext;

    @Override
    public void init() throws ServletException
    {
        super.init();
        this.muleContext = (MuleContext)getServletContext().getAttribute(MuleProperties.MULE_CONTEXT_PROPERTY);
        if(muleContext==null)
        {
            throw new ServletException("Attribute " + MuleProperties.MULE_CONTEXT_PROPERTY + " not set on ServletContext");
        }
        String servletConnectorName = getServletConfig().getInitParameter(MuleServletContextListener.CONNECTOR_NAME);
        if (servletConnectorName == null)
        {
            servletConnectorName = (String)getServletContext().getAttribute(MuleServletContextListener.CONNECTOR_NAME);
        }

        if (servletConnectorName == null)
        {
            connector = new TransportFactory(muleContext).getConnectorByProtocol(getConnectorProtocol());
            if (connector == null)
            {
                connector = new AjaxServletConnector(muleContext);
                connector.setName("ajax.servlet." + getServletContext().getServerInfo());
                try
                {
                    muleContext.getRegistry().registerConnector(connector);
                }
                catch (MuleException e)
                {
                    throw new ServletException("Failed to register the AjaxServletConnector", e);
                }
            }
        }
        else
        {
            connector = muleContext.getRegistry().lookupConnector(servletConnectorName);
            if (connector == null)
            {
                throw new ServletException(AjaxMessages.noAjaxConnectorWithName(servletConnectorName, MuleServletContextListener.CONNECTOR_NAME).toString());
            }
        }

        try
        {
            ((BayeuxAware)connector).setBayeux(getBayeux());
            jsonTransformer = new ObjectToJson();
            muleContext.getRegistry().applyProcessorsAndLifecycle(jsonTransformer);
        }
        catch (MuleException e)
        {
            throw new ServletException(e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        super.service(request, response);
    }

    protected String getConnectorProtocol()
    {
        return AjaxServletConnector.PROTOCOL;
    }

    @Override
    protected AbstractBayeux newBayeux()
    {
        return new MuleContinuationBayeux();
    }

    /**
     * We subclass the {@link org.mortbay.cometd.continuation.ContinuationBayeux} so tat we can insert a different
     * message implementation that allows us to have better control over the message going across the wire. Right now this
     * means that we use Jackson for Json serialization.
     */
    protected class MuleContinuationBayeux extends ContinuationBayeux
    {
        @Override
        public MessageImpl newMessage()
        {
            //TODO no access to the message pool, need to have a fork of ContinuationBayeux to mimic exact behaviour
            MessageImpl message;//_messagePool.poll();
//            if (message == null)
//            {
                message=new MuleMessageImpl(this);
           // }
            message.incRef();
            return message;
        }

        @Override
        public MessageImpl newMessage(Message associated)
        {
            //TODO no access to the message pool, need to have a fork of ContinuationBayeux to mimic exact behaviour
            MessageImpl message;//_messagePool.poll();
//            if (message == null)
//            {
                message=new MuleMessageImpl(this);
            //}
            message.incRef();
            if (associated != null)
                message.setAssociated(associated);
            return message;
        }
    }


    public class MuleMessageImpl extends MessageImpl
    {
        public MuleMessageImpl(MessagePool bayeux)
        {
            super(bayeux);
        }

        @Override
        public String getJSON()
        {
            Object data = getData();
            try
            {
                if (data!=null && !ignoreClasses.contains(data.getClass()))
                {
                    if(jsonBindings.contains(data.getClass()))
                    {
                        return (String) jsonTransformer.transform(this);
                    }
                    else if(AnnotationUtils.hasAnnotationWithPackage("org.codehaus.jackson", data.getClass()))
                    {
                        //Tell the transformer to accept this type next time
                        jsonBindings.add(data.getClass());
                        return (String) jsonTransformer.transform(this);
                    }
                    else
                    {
                        //We can ignore objects of this type and delegate to the super class
                        ignoreClasses.add(data.getClass());
                    }
                }
                return super.getJSON();
            }
            catch (Exception e)
            {
                throw new RuntimeException("Failed to convert message to JSON", e);

            }
        }
    }
}
