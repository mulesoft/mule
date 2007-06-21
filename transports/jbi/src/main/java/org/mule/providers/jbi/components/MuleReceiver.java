/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jbi.components;

import org.mule.RegistryContext;
import org.mule.config.converters.QNameConverter;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.InternalMessageListener;
import org.mule.providers.jbi.JbiMessageAdapter;
import org.mule.providers.jbi.JbiUtils;
import org.mule.providers.jbi.i18n.JbiMessages;
import org.mule.registry.DeregistrationException;
import org.mule.registry.RegistrationException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.util.SystemUtils;

import java.io.OutputStream;
import java.util.Arrays;

import javax.jbi.JBIException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

/**
 * Can receive events over Mule transports. Given an muleEndpoint (or endpoint string
 * i.e. jms://my.queue) This component will set up the necessary bindings with Mule
 * 
 */
public class MuleReceiver extends AbstractEndpointComponent implements InternalMessageListener
{

    private AbstractMessageReceiver receiver;

    protected QName targetService;

    protected String targetServiceName;

    public QName getTargetService()
    {
        return targetService;
    }

    public void setTargetService(QName targetService)
    {
        this.targetService = targetService;
    }

    public AbstractMessageReceiver getReceiver()
    {
        return receiver;
    }

    public void setReceiver(AbstractMessageReceiver receiver)
    {
        this.receiver = receiver;
    }

    public String getTargetServiceName()
    {
        return targetServiceName;
    }

    public void setTargetServiceName(String targetServiceName)
    {
        this.targetServiceName = targetServiceName;
    }

    protected void doInit() throws JBIException
    {
        super.doInit();
        try
        {
            if (targetService == null)
            {
                if (targetServiceName != null)
                {
                    targetService = (QName)new QNameConverter().convert(QName.class, targetServiceName);
                }
            }

            UMOMessageReceiver receiver = muleEndpoint.getConnector().registerListener(
                new NullUMOComponent(getName()), muleEndpoint);

            if (receiver == null)
            {
                throw new IllegalArgumentException(
                    JbiMessages.receiverMustBeSet(this.getName()).toString());
            }
            else if (receiver instanceof AbstractMessageReceiver)
            {
                this.receiver = (AbstractMessageReceiver)receiver;
            }
            else
            {
                throw new IllegalArgumentException(
                    JbiMessages.invalidReceiverType(this.getName(), AbstractMessageReceiver.class).toString());
            }

            this.receiver.setListener(this);
        }
        catch (Exception e)
        {
            throw new JBIException(e);
        }
    }

    public UMOMessage onMessage(UMOMessage message,
                                UMOTransaction trans,
                                boolean synchronous,
                                OutputStream outputStream) throws UMOException
    {
        MessageExchange me = null;
        try
        {
            if (synchronous)
            {
                me = exchangeFactory.createInOutExchange();
            }
            else
            {
                me = exchangeFactory.createInOnlyExchange();
            }
            if (targetService != null)
            {
                me.setService(targetService);
                ServiceEndpoint endpoint;
                ServiceEndpoint[] eps = context.getEndpointsForService(targetService);
                if (eps.length == 0)
                {
                    // container should handle this
                    throw new MessagingException("There are no endpoints registered for targetService: "
                                                 + targetService);
                }
                else
                {
                    endpoint = eps[0];
                }

                if (logger.isDebugEnabled())
                {
                    StringBuffer buf = new StringBuffer("Found the following endpoints for: ");
                    buf.append(targetService).append(SystemUtils.LINE_SEPARATOR);
                    for (int i = 0; i < eps.length; i++)
                    {
                        ServiceEndpoint ep = eps[i];
                        buf.append(ep.getEndpointName())
                            .append(";")
                            .append(ep.getServiceName())
                            .append(";")
                            .append(Arrays.asList(ep.getInterfaces()))
                            .append(SystemUtils.LINE_SEPARATOR);
                    }
                    logger.debug(buf.toString());
                }

                logger.debug("Using Jbi Endpoint for targetService: " + targetService + " is: " + endpoint);
                if (endpoint != null)
                {
                    me.setEndpoint(endpoint);
                }
            }
            else
            {
                logger.debug("Jbi target service is not set Container will need to resolve target");
            }

            NormalizedMessage nmessage = me.createMessage();
            JbiUtils.populateNormalizedMessage(message, nmessage);

            me.setMessage(nmessage, IN);
            if (synchronous)
            {
                deliveryChannel.sendSync(me, RegistryContext.getConfiguration().getDefaultSynchronousEventTimeout());
                NormalizedMessage result;

                result = me.getMessage(OUT);
                done(me);
                if (result != null)
                {
                    return new MuleMessage(new JbiMessageAdapter(result));
                }
                else
                {
                    return null;
                }
            }
            else
            {
                deliveryChannel.send(me);
                return null;
            }
        }
        catch (MessagingException e)
        {
            try
            {
                error(me, e);
                return null;
            }
            catch (MessagingException e1)
            {
                handleException(e);
                return null;
            }
        }
    }

    /**
     * A null component is used when interfacing with JBI components, the Null
     * component is a placeholder of the JBI component that isn't managed by mule
     */
    class NullUMOComponent implements UMOComponent
    {
        /**
         * Serial version
         */
        private static final long serialVersionUID = 6446394166371870045L;

        /*
         * Registry ID
         */
        private String registryId = null;

        private UMODescriptor descriptor;

        public NullUMOComponent(String name)
        {
            this.descriptor = new MuleDescriptor(name);
        }

        public UMODescriptor getDescriptor()
        {
            return descriptor;
        }

        public void dispatchEvent(UMOEvent event) throws UMOException
        {
            throw new UnsupportedOperationException("NullComponent:dispatchEvent");
        }

        public UMOMessage sendEvent(UMOEvent event) throws UMOException
        {
            throw new UnsupportedOperationException("NullComponent:sendEvent");
        }

        public void pause() throws UMOException
        {
            // nothing to do
        }

        public void resume() throws UMOException
        {
            // nothing to do
        }

        public boolean isPaused()
        {
            return false;
        }

        public void start() throws UMOException
        {
            // nothing to do
        }

        public void stop() throws UMOException
        {
            // nothing to do
        }

        public void dispose()
        {
            // nothing to do
        }

        public void initialise() throws InitialisationException {
            // nothing to do
        }

        public boolean isStarted()
        {
            return true;
        }

        public Object getInstance() throws UMOException
        {
            return null;
        }

        /*
         * (non-Javadoc)
         * 
        * @see org.mule.umo.lifecycle.Registerable#register()
        */
        public void register() throws RegistrationException
        {
            registryId = 
                RegistryContext.getRegistry().registerMuleObject(descriptor, this).getId();
        }

        /*
        * (non-Javadoc)
        * 
        * @see org.mule.umo.lifecycle.Registerable#deregister()
        */
        public void deregister() throws DeregistrationException
        {
            RegistryContext.getRegistry().deregisterComponent(registryId);
            registryId = null;
        }

        /**
         * Returns the registry id.
         *
         * @return the registry ID
         */
        public String getRegistryId()
        {
            return registryId;
        }
    }
}
