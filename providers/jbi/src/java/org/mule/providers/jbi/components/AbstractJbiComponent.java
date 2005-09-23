/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jbi.components;

import java.util.HashMap;
import java.util.Map;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.converters.QNameConverter;
import org.mule.util.concurrent.WaitableBoolean;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

/**
 * A base Jbi component implementation.  This is agnostic to any particular Jbi container
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractJbiComponent implements Component, Work, ComponentLifeCycle {

    public static final String IN = "in";
    public static final String OUT = "out";
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected ComponentContext context;

    protected Map serviceDescriptions = new HashMap();

    protected QName service;

    protected String name;

    protected WorkManager workManager;

    protected DeliveryChannel deliveryChannel;

    protected ObjectName mbeanName;

    protected ServiceUnitManager serviceUnitManager;

    protected MessageExchangeFactory exchangeFactory;

    protected WaitableBoolean started = new WaitableBoolean(false);


    public ComponentLifeCycle getLifeCycle() {
        return this;
    }

    public ServiceUnitManager getServiceUnitManager() {
        return serviceUnitManager;
    }

    public Document getServiceDescription(ServiceEndpoint endpoint) {
        if (logger.isDebugEnabled()) {
            logger.debug("Querying service description for " + endpoint);
        }
        String key = getKey(endpoint);
        Document doc = (Document) this.serviceDescriptions.get(key);
        if (logger.isDebugEnabled()) {
            if (doc != null) {
                logger.debug("Description found");
            } else {
                logger.debug("Description not found");
            }
        }
        return doc;
    }

    public void setServiceDescription(ServiceEndpoint endpoint, Document doc) {
        if (logger.isDebugEnabled()) {
            logger.debug("Setting service description for " + endpoint);
        }
        String key = getKey(endpoint);
        this.serviceDescriptions.put(key, doc);
    }

    private String getKey(ServiceEndpoint endpoint) {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        sb.append(endpoint.getServiceName().getNamespaceURI());
        sb.append("}");
        sb.append(endpoint.getServiceName().getLocalPart());
        sb.append(":");
        sb.append(endpoint.getEndpointName());
        return sb.toString();
    }

    /* (non-Javadoc)
    * @see javax.jbi.component.Component#isExchangeWithConsumerOkay(javax.jbi.servicedesc.ServiceEndpoint, javax.jbi.messaging.MessageExchange)
    */
    public boolean isExchangeWithConsumerOkay(ServiceEndpoint endpoint, MessageExchange exchange) {
        return true;
    }

    /* (non-Javadoc)
      * @see javax.jbi.component.Component#isExchangeWithProviderOkay(javax.jbi.servicedesc.ServiceEndpoint, javax.jbi.messaging.MessageExchange)
      */
    public boolean isExchangeWithProviderOkay(ServiceEndpoint endpoint, MessageExchange exchange) {
        return true;
    }

    /* (non-Javadoc)
      * @see javax.jbi.component.Component#resolveEndpointReference(org.w3c.dom.DocumentFragment)
      */
    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return null;
    }

    protected ObjectName createExtensionMBeanName() throws Exception {
        return this.context.getMBeanNames().createCustomComponentMBeanName("extension");
    }

    public Object getExtensionMBean() {
        return null; //todo
    }

    public QName getService() {
        return service;
    }

    public void setService(QName service) {
        this.service = service;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WorkManager getWorkManager() {
        return workManager;
    }

    public void setWorkManager(WorkManager workManager) {
        this.workManager = workManager;
    }

    //----------Component Lifecycle methods ----------------//

    /* (non-Javadoc)
    * @see javax.jbi.component.ComponentLifeCycle#init(javax.jbi.component.ComponentContext)
    */
    public synchronized final void init(ComponentContext context) throws JBIException {
        try {
            if(context.getComponentName()!=null) {
                name = context.getComponentName();
            }
            if(name==null) {
                throw new NullPointerException("No name has been set for this component");
            }

            if(service==null) {
                service = (QName)new QNameConverter().convert(QName.class, name);
            }

            context.activateEndpoint(service, service.getLocalPart());

            if (logger.isDebugEnabled()) {
                logger.debug("Initializing component: " + name);
            }
            this.context = context;
            deliveryChannel = context.getDeliveryChannel();
            exchangeFactory = deliveryChannel.createExchangeFactory();
            Object mbean = getExtensionMBean();
            if (serviceUnitManager == null) {
                serviceUnitManager = new DefaultServiceUnitManager();
            }

            if (workManager == null) {
                workManager = MuleManager.getInstance().getWorkManager();
            }

            if (mbean != null) {
                if (mbeanName == null) {
                    this.mbeanName = createExtensionMBeanName();
                }
                MBeanServer server = AbstractJbiComponent.this.context.getMBeanServer();
                if (server == null) {
                    throw new JBIException("null mBeanServer");
                }

                if (server.isRegistered(this.mbeanName)) {
                    server.unregisterMBean(this.mbeanName);
                }
                server.registerMBean(mbean, this.mbeanName);
            }

            doInit();
            if (logger.isDebugEnabled()) {
                logger.debug("Jbi Reciever Component initialized: " + name);
            }
        } catch (JBIException e) {
            throw e;
        } catch (Exception e) {
            throw new JBIException("Error calling init on " + name, e);
        }
    }

    public final void shutDown() throws JBIException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Shutting down component: " + getName());
            }
            started.set(false);
            doShutdown();
            if (this.mbeanName != null) {
                MBeanServer server = context.getMBeanServer();
                if (server == null) {
                    throw new JBIException("null mBeanServer");
                }
                if (server.isRegistered(this.mbeanName)) {
                    server.unregisterMBean(this.mbeanName);
                }
            }
            this.context = null;
            if (logger.isDebugEnabled()) {
                logger.debug("Component shut down: " + getName());
            }
        } catch (JBIException e) {
            throw e;
        } catch (Exception e) {
            throw new JBIException("Error calling shutdown on " + getName(), e);
        }
    }

    public final void start() throws JBIException {
        logger.debug("Starting Mule Jbi component: " + name);
        started.set(true);
        if (this instanceof MessageExchangeListener) {
            try {
                logger.debug("Starting ME thread for: " + name);
                getWorkManager().scheduleWork(this);
            } catch (WorkException e) {
                throw new JBIException(e);
            }
        }
        doStart();

    }


    public final void stop() throws JBIException {
        started.set(false);
        doStop();
    }

    protected void doInit() throws JBIException {

    }

    protected void doStart() throws JBIException {

    }

    protected void doStop() throws JBIException {

    }

    protected void doShutdown() throws JBIException {

    }

    public ObjectName getExtensionMBeanName() {
        return mbeanName;
    }

    public void setExtensionMBeanName(ObjectName mbeanName) {
        this.mbeanName = mbeanName;
    }

    //--------- Work impl ---------//
    //- This receive code should be separated out to pluggable invocation
    //- strategies
    public void release() {

    }

    public void run() {
        while (started.get()) {
            try {
                final MessageExchange me = deliveryChannel.accept();
                if (me != null) {
                    getWorkManager().scheduleWork(new MessageExchangeWorker(me, (MessageExchangeListener) this));
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    protected class MessageExchangeWorker implements Work {
        private MessageExchange me;
        private MessageExchangeListener listener;

        public MessageExchangeWorker(MessageExchange me, MessageExchangeListener listener) {
            this.me = me;
            this.listener = listener;
        }

        public void release() {
        }

        public void run() {
            try {
                try {
                    listener.onExchange(me);
                    done(me);
                } catch (MessagingException e) {
                    error(me, e);
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    protected void handleException(Throwable t) {
        logger.error(t.getMessage(), t);
    }

    protected void error(MessageExchange me, Fault fault) throws MessagingException {
        me.setFault(fault);
        me.setStatus(ExchangeStatus.ERROR);
        context.getDeliveryChannel().send(me);
    }

    protected void done(MessageExchange me) throws MessagingException {
        me.setStatus(ExchangeStatus.DONE);
        context.getDeliveryChannel().send(me);
    }

    protected void error(MessageExchange me, Exception e) throws MessagingException {
        me.setError(e);
        me.setStatus(ExchangeStatus.ERROR);
        context.getDeliveryChannel().send(me);
    }

    private class DefaultServiceUnitManager implements ServiceUnitManager {
        public String deploy(String string, String string1) throws DeploymentException {
            return null;
        }

        public void init(String string, String string1) throws DeploymentException {

        }

        public void start(String string) throws DeploymentException {

        }

        public void stop(String string) throws DeploymentException {

        }

        public void shutDown(String string) throws DeploymentException {

        }

        public String undeploy(String string, String string1) throws DeploymentException {
            return null;
        }
    }
}
