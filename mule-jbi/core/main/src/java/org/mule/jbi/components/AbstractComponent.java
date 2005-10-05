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
*
*/
package org.mule.jbi.components;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.work.MuleWorkManager;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.messaging.MessageExchangeConstants;
import org.mule.jbi.messaging.MessageListener;
import org.mule.jbi.messaging.NoMessageException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import javax.jbi.JBIException;
import javax.jbi.component.Bootstrap;
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
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.resource.spi.work.Work;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractComponent implements Component, ComponentLifeCycle,
        MessageExchangeConstants, ServiceUnitManager, Work {

	protected final transient Log logger = LogFactory.getLog(getClass());
	
	protected ComponentContext context;
	
	protected ObjectName mbeanName;
	
	protected Map serviceDescriptions = new HashMap();
	
	protected MuleWorkManager workManager;

    protected AtomicBoolean stopped = new AtomicBoolean(true);

    protected AtomicBoolean stopping = new AtomicBoolean(false);

    protected MessageExchangeFactory exchangeFactory;

    protected DeliveryChannel channel;

    protected JbiContainer container;

    protected Bootstrap bootstrap;

    protected String name;

    protected QName service;


    public QName getService() {
        return service;
    }

    public void setService(QName service) {
        this.service = service;
    }

	public ComponentContext getContext() {
		return context;
	}
	
	/* (non-Javadoc)
	 * @see javax.jbi.component.Component#getLifeCycle()
	 */
	public final ComponentLifeCycle getLifeCycle() {
		return this;
	}

    public JbiContainer getContainer() {
        return container;
    }

    public void setContainer(JbiContainer container) {
        this.container = container;
    }

	/* (non-Javadoc)
	 * @see javax.jbi.component.Component#getServiceUnitManager()
	 */
	public final ServiceUnitManager getServiceUnitManager() {
		return this;
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.Component#getServiceDescription(javax.jbi.servicedesc.ServiceEndpoint)
	 */
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

	/* (non-Javadoc)
	 * @see javax.jbi.component.ComponentLifeCycle#getExtensionMBeanName()
	 */
	public final ObjectName getExtensionMBeanName() {
		return this.mbeanName;
	}

    public DeliveryChannel getChannel() {
        return channel;
    }

	/* (non-Javadoc)
	 * @see javax.jbi.component.ComponentLifeCycle#init(javax.jbi.component.ComponentContext)
	 */
	public synchronized final void init(ComponentContext context) throws JBIException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Initializing component: " + getName());
			}
			this.context = context;
            Object mbean = getExtensionMBean();
            if (mbean != null) {
                this.mbeanName = createExtensionMBeanName();
                MBeanServer server = this.context.getMBeanServer();
                if (server == null) {
                    throw new JBIException("null mBeanServer");
                }
                if (server.isRegistered(this.mbeanName)) {
                    server.unregisterMBean(this.mbeanName);
                }
                server.registerMBean(mbean, this.mbeanName);
            }
            if (service == null) {
                service = new QName(name);
            }
            // Create work manager
            this.workManager = new MuleWorkManager();
			doInit();
			if (logger.isDebugEnabled()) {
				logger.debug("Component initialized: " + getName());
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling init on " + getName(), e);
		}
	}
	
	protected void doInit() throws Exception {

	}
	
	/**
	 * 
	 * @return the component extension MBean.
	 * @throws Exception if an error occurs
	 */
	protected Object getExtensionMBean() throws Exception {
		return null;
	}
	
	protected ObjectName createExtensionMBeanName() throws Exception {
		return this.context.getMBeanNames().createCustomComponentMBeanName("extension");
	}


	/* (non-Javadoc)
	 * @see javax.jbi.component.ComponentLifeCycle#shutDown()
	 */
	public final void shutDown() throws JBIException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Shutting down component: "  + getName());
			}
            doShutDown();
            if (this.mbeanName != null) {
                MBeanServer server = this.context.getMBeanServer();
                if (server == null) {
                    throw new JBIException("null mBeanServer");
                }
                if (server.isRegistered(this.mbeanName)) {
                    server.unregisterMBean(this.mbeanName);
                }
            }
			this.context = null;
			if (logger.isDebugEnabled()) {
				logger.debug("Component shut down: "  + getName());
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling shutdown on " + getName(), e);
		}
	}

	protected void doShutDown() throws Exception {

	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.ComponentLifeCycle#start()
	 */
	public final void start() throws JBIException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Starting component: " + getName());
			}
            this.workManager.start();
            channel = context.getDeliveryChannel();
            if (this instanceof MessageListener) {
                workManager.scheduleWork(this);
            }
			doStart();
            stopped.set(false);
			if (logger.isDebugEnabled()) {
				logger.debug("Component started: " + getName());
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling start on " + getName(), e);
		}
	}

//	protected void doStart() throws Exception {
//
//		this.workManager.start();
//		this.meListener = new Thread(new Runnable() {
//			public void run() {
//				try {
//					DeliveryChannel channel = AbstractComponent.this.container.getDeliveryChannel();
//					while (true) {
//						final MessageExchange me = channel.accept();
//						if (me.isTransacted()) {
//							TransactionManager mgr = (TransactionManager) AbstractComponent.this.container.getTransactionManager();
//							Transaction tx = (Transaction) me.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME);
//							if (tx == mgr.getTransaction()) {
//								mgr.suspend();
//							}
//						}
//						workManager.scheduleWork(new Work() {
//							public void release() {
//							}
//							public void run() {
//								try {
//									if (me.isTransacted()) {
//										TransactionManager mgr = (TransactionManager) AbstractComponent.this.container.getTransactionManager();
//										Transaction tx = (Transaction) me.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME);
//										mgr.resume(tx);
//									}
//									AbstractComponent.this.process(me);
//								} catch (Exception e) {
//									logger.error("Error processing message", e);
//								}
//							}
//						});
//					}
//				} catch (Exception e) {
//					throw new RuntimeException(e);
//				}
//			}
//		});
//		this.meListener.setDaemon(true);
//		this.meListener.start();
//	}


	/* (non-Javadoc)
	 * @see javax.jbi.component.ComponentLifeCycle#stop()
	 */
	public final void stop() throws JBIException {
		try {
            stopping.set(true);
			if (logger.isDebugEnabled()) {
				logger.debug("Stopping component: " + getName());
			}
            doStop();
//            this.meListener.interrupt();
//            this.meListener.join();
            this.workManager.stop();
            stopped.set(true);
            stopping.set(false);
			if (logger.isDebugEnabled()) {
				logger.debug("Component stopped: " + getName());
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling stop on " + getName(), e);
		}
	}

	protected void doStop() throws Exception {

	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.ServiceUnitManager#deploy(java.lang.String, java.lang.String)
	 */
	public final String deploy(String serviceUnitName, String serviceUnitRootPath) throws DeploymentException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Deploying service unit");
			}
			String result = doDeploy(serviceUnitName, serviceUnitRootPath);
			if (logger.isDebugEnabled()) {
				logger.debug("Service unit deployed");
			}
			return result;
		} catch (DeploymentException e) {
			throw e;
		} catch (Exception e) {
			throw new DeploymentException("Error deploying service unit", e);
		}
	}
	
	protected String doDeploy(String serviceUnitName, String serviceUnitRootPath) throws Exception {
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.ServiceUnitManager#init(java.lang.String, java.lang.String)
	 */
	public void init(String serviceUnitName, String serviceUnitRootPath) throws DeploymentException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Initializing service unit");
			}
			doInit(serviceUnitName, serviceUnitRootPath);
			if (logger.isDebugEnabled()) {
				logger.debug("Service unit initialized");
			}
		} catch (DeploymentException e) {
			throw e;
		} catch (Exception e) {
			throw new DeploymentException("Error initializing service unit", e);
		}
	}

	protected void doInit(String serviceUnitName, String serviceUnitRootPath) throws Exception {
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.ServiceUnitManager#start(java.lang.String)
	 */
	public void start(String serviceUnitName) throws DeploymentException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Starting service unit");
			}
			doStart(serviceUnitName);
			if (logger.isDebugEnabled()) {
				logger.debug("Service unit started");
			}
		} catch (DeploymentException e) {
			throw e;
		} catch (Exception e) {
			throw new DeploymentException("Error starting service unit", e);
		}
	}

    protected void doStart() throws Exception {}

	protected void doStart(String serviceUnitName) throws Exception {
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.ServiceUnitManager#stop(java.lang.String)
	 */
	public void stop(String serviceUnitName) throws DeploymentException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Stopping service unit");
			}
			doStop(serviceUnitName);
			if (logger.isDebugEnabled()) {
				logger.debug("Service unit stopped");
			}
		} catch (DeploymentException e) {
			throw e;
		} catch (Exception e) {
			throw new DeploymentException("Error stopping service unit", e);
		}
	}

	protected void doStop(String serviceUnitName) throws Exception {
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.ServiceUnitManager#shutDown(java.lang.String)
	 */
	public void shutDown(String serviceUnitName) throws DeploymentException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Shutting down service unit");
			}
			doShutDown(serviceUnitName);
			if (logger.isDebugEnabled()) {
				logger.debug("Service unit shut down");
			}
		} catch (DeploymentException e) {
			throw e;
		} catch (Exception e) {
			throw new DeploymentException("Error shutting down service unit", e);
		}
	}

	protected void doShutDown(String serviceUnitName) throws Exception {
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.ServiceUnitManager#undeploy(java.lang.String, java.lang.String)
	 */
	public String undeploy(String serviceUnitName, String serviceUnitRootPath) throws DeploymentException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Undeploying service unit");
			}
			String result = doUndeploy(serviceUnitName, serviceUnitRootPath);
			if (logger.isDebugEnabled()) {
				logger.debug("Service unit undeployed");
			}
			return result;
		} catch (DeploymentException e) {
			throw e;
		} catch (Exception e) {
			throw new DeploymentException("Error undeploying service unit", e);
		}
	}

	protected String doUndeploy(String serviceUnitName, String serviceUnitRootPath) throws Exception {
		return null;
	}

    protected void error(MessageExchange me, Exception e) throws MessagingException {
        me.setError(e);
        me.setStatus(ExchangeStatus.ERROR);
        context.getDeliveryChannel().send(me);
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

    protected boolean isInOnly(MessageExchange me) {
        return (me.getPattern().equals(IN_ONLY_PATTERN) || me.getPattern().equals(ROBUST_IN_ONLY_PATTERN));

    }

    protected boolean isInOut(MessageExchange me) {
        return (me.getPattern().equals(IN_OUT_PATTERN) || me.getPattern().equals(IN_OPTIONAL_OUT_PATTERN));

    }

    protected MessageExchangeFactory getExchangeFactory() throws MessagingException {
        if(exchangeFactory==null) {
            exchangeFactory = context.getDeliveryChannel().createExchangeFactory();
        }
        return exchangeFactory;
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public boolean isStopping() {
        return stopping.get();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected NormalizedMessage getInMessage(MessageExchange me) throws NoMessageException{
        if(me==null) {
            throw new NullPointerException("MessageExchange is null");
        }
        NormalizedMessage message = me.getMessage(IN);
        if(message==null) {
            throw new NoMessageException(me, IN);
        }
        return message;
    }

    protected NormalizedMessage getOutMessage(MessageExchange me) throws NoMessageException{
        if(me==null) {
            throw new NullPointerException("MessageExchange is null");
        }
        NormalizedMessage message = me.getMessage(OUT);
        if(message==null) {
            throw new NoMessageException(me, OUT);
        }
        return message;
    }

    public Bootstrap getBootstrap() {
        if(bootstrap==null) {
            bootstrap = createBootstrap();
        }
        return bootstrap;
    }

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    protected Bootstrap createBootstrap() {
        return new SimpleBootstrap();
    }

    //- This receive code should be separated out to pluggable invocation
    //- strategies
    public void release() {

    }

    public void run() {
        while (!isStopped() && !isStopping()) {
            try {
                final MessageExchange me = channel.accept();
                if (me != null) {
                    workManager.scheduleWork(new MessageExchangeWorker(me, (MessageListener)this));
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    protected void handleException(Exception e) {
        logger.error(e.getMessage(), e);
    }

    protected class MessageExchangeWorker implements Work {
        private MessageExchange me;
        private MessageListener listener;

        public MessageExchangeWorker(MessageExchange me, MessageListener listener) {
            this.me = me;
            this.listener = listener;
        }

        public void release() {
        }

        public void run() {
            try {
                //todo getting component not owner exception
//                if (me.isTransacted()) {
//                    TransactionManager mgr = (TransactionManager) container.getTransactionManager();
//                    Transaction tx = (Transaction) me.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME);
//                    mgr.resume(tx);
//                }
                try {
                    listener.onMessage(me);
                    done(me);
                } catch (MessagingException e) {
                    error(me, e);
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
    }
}
