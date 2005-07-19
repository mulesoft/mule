package org.mule.jbi.components;

import java.util.HashMap;
import java.util.Map;

import javax.jbi.JBIException;
import javax.jbi.component.Component;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.resource.spi.work.Work;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.jbi.work.MuleWorkManager;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

public class AbstractComponent implements Component, ComponentLifeCycle, ServiceUnitManager {

	protected final transient Log logger = LogFactory.getLog(getClass());
	
	protected ComponentContext context;
	
	protected ObjectName mbeanName;
	
	protected Map serviceDescriptions = new HashMap();
	
	protected Thread meListener;
	
	protected MuleWorkManager workManager;
	
	public ComponentContext getContext() {
		return context;
	}
	
	/* (non-Javadoc)
	 * @see javax.jbi.component.Component#getLifeCycle()
	 */
	public final ComponentLifeCycle getLifeCycle() {
		return this;
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

	/* (non-Javadoc)
	 * @see javax.jbi.component.ComponentLifeCycle#init(javax.jbi.component.ComponentContext)
	 */
	public final void init(ComponentContext context) throws JBIException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Initializing component");
			}
			this.context = context;
			doInit();
			if (logger.isDebugEnabled()) {
				logger.debug("Component initialized");
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling init", e);
		}
	}
	
	protected void doInit() throws Exception {
		Object mbean = getExtensionMBean();
		if (mbean != null) {
			this.mbeanName = this.context.getMBeanNames().createCustomComponentMBeanName("extension");
			MBeanServer server = this.context.getMBeanServer();
			if (server == null) {
				throw new JBIException("null mBeanServer");
			}
			if (server.isRegistered(this.mbeanName)) {
				server.unregisterMBean(this.mbeanName);
			}
			server.registerMBean(mbean, this.mbeanName);
		}
		// Create work manager
		this.workManager = new MuleWorkManager();
	}
	
	/**
	 * 
	 * @return the component extension MBean.
	 * @throws Exception if an error occurs
	 */
	protected Object getExtensionMBean() throws Exception {
		return null;
	}


	/* (non-Javadoc)
	 * @see javax.jbi.component.ComponentLifeCycle#shutDown()
	 */
	public final void shutDown() throws JBIException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Shutting down component");
			}
			doShutDown();
			this.context = null;
			if (logger.isDebugEnabled()) {
				logger.debug("Component shut down");
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling init", e);
		}
	}

	protected void doShutDown() throws Exception {
		if (this.mbeanName != null) {
			MBeanServer server = this.context.getMBeanServer();
			if (server == null) {
				throw new JBIException("null mBeanServer");
			}
			if (server.isRegistered(this.mbeanName)) {
				server.unregisterMBean(this.mbeanName);
			}
		}
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.ComponentLifeCycle#start()
	 */
	public final void start() throws JBIException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Starting component");
			}
			doStart();
			if (logger.isDebugEnabled()) {
				logger.debug("Component started");
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling init", e);
		}
	}

	protected void doStart() throws Exception {
		this.workManager.start();
		this.meListener = new Thread(new Runnable() {
			public void run() {
				try {
					DeliveryChannel channel = AbstractComponent.this.context.getDeliveryChannel();
					while (true) {
						final MessageExchange me = channel.accept();
						AbstractComponent.this.workManager.scheduleWork(new Work() {
							public void release() {
							}
							public void run() {
								AbstractComponent.this.process(me);
							} 
						});
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} 
		});
		this.meListener.setDaemon(true);
		this.meListener.start();
	}
	
	protected void process(MessageExchange me) {
		
	}

	/* (non-Javadoc)
	 * @see javax.jbi.component.ComponentLifeCycle#stop()
	 */
	public final void stop() throws JBIException {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Stopping component");
			}
			doStop();
			if (logger.isDebugEnabled()) {
				logger.debug("Component stopped");
			}
		} catch (JBIException e) {
			throw e;
		} catch (Exception e) {
			throw new JBIException("Error calling init", e);
		}
	}

	protected void doStop() throws Exception {
		this.meListener.interrupt();
		this.meListener.join();
		this.workManager.stop();
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

}
