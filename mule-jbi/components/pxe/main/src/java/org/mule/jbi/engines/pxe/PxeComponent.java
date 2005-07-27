/*
 * Copyright 2005 SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * ------------------------------------------------------------------------------------------------------
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.jbi.engines.pxe;

import com.fs.pxe.bpel.provider.BpelServiceProvider;
import com.fs.pxe.kernel.modbpellog.ModBpelEventLogger;
import com.fs.pxe.kernel.modhibernatedao.ModHibernateDAO;
import com.fs.pxe.kernel.modjdbc.ModJdbcDS;
import com.fs.pxe.kernel.modsfwk.ModSfwk;
import com.fs.pxe.kernel.modsfwk.ModSvcProvider;
import com.fs.pxe.sfwk.core.PxeSystemException;
import com.fs.pxe.sfwk.deployment.SarFile;
import com.fs.pxe.sfwk.deployment.SystemDeploymentBundle;
import com.fs.pxe.sfwk.deployment.som.SystemDescriptor;
import com.fs.pxe.sfwk.mngmt.DomainAdminMBean;
import com.fs.pxe.sfwk.mngmt.SystemAdminMBean;
import com.fs.pxe.sfwk.spi.Message;
import com.fs.pxe.sfwk.spi.MessageExchangeEvent;
import com.fs.pxe.sfwk.spi.ServiceContext;
import com.fs.pxe.sfwk.spi.ServicePort;
import com.fs.utils.DOMUtils;
import org.mule.jbi.components.AbstractComponent;
import org.w3c.dom.Document;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PxeComponent extends AbstractComponent {

	private MBeanServer server;
	private ObjectName[] names;
	private Map services;
	private Map endpoints;


	/** Singleton instance */
	private static PxeComponent instance;
	
	public static PxeComponent getInstance() {
		return PxeComponent.instance;
	}
	
	public PxeComponent() {
		PxeComponent.instance = this;
		this.services = new HashMap();
		this.endpoints = new HashMap();
	}
	
	/* (non-Javadoc)
	 * @see org.mule.jbi.components.AbstractComponent#doInit()
	 */
	protected void doInit() throws Exception {
		this.server = this.context.getMBeanServer();

		InitialContext ctx = new InitialContext();
		ctx.rebind("PxeTransactionManager", this.context.getTransactionManager());
		ctx.close();
		
		ObjectName modBpelEventLogger = new ObjectName("fivesight.pxe:mod=BpelLogger");
		this.server.createMBean(ModBpelEventLogger.class.getName(), modBpelEventLogger);
		this.server.invoke(modBpelEventLogger, "start", new Object[0], new String[0]);
		
		ObjectName modJdbcDS = new ObjectName("fivesight.pxe:mod=PxeDB");
		this.server.createMBean(ModJdbcDS.class.getName(), modJdbcDS);
		this.server.setAttribute(modJdbcDS, new Attribute("DataSourceName", "pxe-ds"));
		this.server.setAttribute(modJdbcDS, new Attribute("TransactionManagerName", "PxeTransactionManager"));
		this.server.setAttribute(modJdbcDS, new Attribute("Driver", "org.hsqldb.jdbcDriver"));
		this.server.setAttribute(modJdbcDS, new Attribute("Username", "sa"));
		this.server.setAttribute(modJdbcDS, new Attribute("Password", ""));
		this.server.setAttribute(modJdbcDS, new Attribute("Url", "jdbc:hsqldb:" + new File(this.context.getWorkspaceRoot()).getCanonicalFile().toURI() + "hsqldb/pxeDb"));
		this.server.setAttribute(modJdbcDS, new Attribute("PoolMax", new Integer(10)));
		this.server.setAttribute(modJdbcDS, new Attribute("PoolMin", new Integer(5)));
		this.server.invoke(modJdbcDS, "start", new Object[0], new String[0]);
		
		ObjectName modHibernateDAO = new ObjectName("fivesight.pxe:mod=HibernateDAO");
		this.server.createMBean(ModHibernateDAO.class.getName(), modHibernateDAO);
		this.server.setAttribute(modHibernateDAO, new Attribute("BpelStateStoreConnectionFactory", "bpel_sscf"));
		this.server.setAttribute(modHibernateDAO, new Attribute("StateStoreConnectionFactory", "sscf"));
		this.server.setAttribute(modHibernateDAO, new Attribute("TransactionManager", "PxeTransactionManager"));
		this.server.setAttribute(modHibernateDAO, new Attribute("DataSource", "pxe-ds"));
		this.server.setAttribute(modHibernateDAO, new Attribute("HibernateProperties", Thread.currentThread().getContextClassLoader().getResource("hibernate.properties").toString()));
		this.server.invoke(modHibernateDAO, "start", new Object[0], new String[0]);
		
		ObjectName modSPBpel = new ObjectName("fivesight.pxe:mod=ServiceProvider,name=Bpel");
		this.server.createMBean(ModSvcProvider.class.getName(), modSPBpel);
		this.server.setAttribute(modSPBpel, new Attribute("JndiName", "BpelSP"));
		this.server.setAttribute(modSPBpel, new Attribute("TransactionManagerName", "PxeTransactionManager"));
		this.server.setAttribute(modSPBpel, new Attribute("ProviderClass", BpelServiceProvider.class.getName()));
		this.server.setAttribute(modSPBpel, new Attribute("ProviderURI", "uri:bpelProvider"));
		this.server.setAttribute(modSPBpel, new Attribute("ProviderProperties", "stateStoreConnectionFactory=bpel_sscf"));
		this.server.invoke(modSPBpel, "start", new Object[0], new String[0]);
		
		ObjectName modSPJbi = new ObjectName("fivesight.pxe:mod=ServiceProvider,name=Jbi");
		this.server.createMBean(ModSvcProvider.class.getName(), modSPJbi);
		this.server.setAttribute(modSPJbi, new Attribute("JndiName", "JbiSP"));
		this.server.setAttribute(modSPJbi, new Attribute("TransactionManagerName", "PxeTransactionManager"));
		this.server.setAttribute(modSPJbi, new Attribute("ProviderClass", JbiAdapter.class.getName()));
		this.server.setAttribute(modSPJbi, new Attribute("ProviderURI", "uri:protocoladapter.jbi"));
		this.server.invoke(modSPJbi, "start", new Object[0], new String[0]);
		
		ObjectName modSfwk = new ObjectName("fivesight.pxe:mod=PXE");
		this.server.createMBean(ModSfwk.class.getName(), modSfwk);
		this.server.setAttribute(modSfwk, new Attribute("DomainId", "mule-jbi"));
		this.server.setAttribute(modSfwk, new Attribute("TransactionManager", "PxeTransactionManager"));
		this.server.setAttribute(modSfwk, new Attribute("DAOConnectionFactory", "sscf"));
		this.server.setAttribute(modSfwk, new Attribute("SvcProviderJndiBindings", "BpelSP,JbiSP"));
		this.server.invoke(modSfwk, "start", new Object[0], new String[0]);
		
		this.names = new ObjectName[] { 
				modBpelEventLogger, modJdbcDS,
				modHibernateDAO, modSPBpel, 
				modSPJbi, modSfwk
		};
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.components.AbstractComponent#doShutDown()
	 */
	protected void doShutDown() throws Exception {
		for (int i = 0; i < this.names.length; i++) {
			this.server.invoke(this.names[i], "stop", new Object[0], new String[0]);
			this.server.unregisterMBean(this.names[i]);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.jbi.components.AbstractComponent#doDeploy(java.lang.String, java.lang.String)
	 */
	protected String doDeploy(String serviceUnitName, String serviceUnitRootPath) throws Exception {
		SystemDeploymentBundle bundle = null;
	    File[] files = new File(serviceUnitRootPath).listFiles();
	    if (files.length == 1 && files[0].getName().endsWith(".jar")) {
	    	bundle = JarToSarConverter.convert(files[0]);
	    } else if (files.length == 1 && files[0].getName().endsWith(".sar")) {
	    	bundle = new SarFile(files[0]);
	    } else {
	    	bundle = DirectoryToSarConverter.convert(new File(serviceUnitRootPath));
	    }
	    if (!serviceUnitName.equals(bundle.getDescriptor().getName())) {
	    	throw new Exception("Process and unit service name should be the same");
	    }
	    handleDeploymentBundle(bundle);
	    return null;
	}

	private boolean handleDeploymentBundle(SystemDeploymentBundle sdb) throws Exception {
		ObjectName adminName = (ObjectName) this.server.getAttribute(new ObjectName("fivesight.pxe:mod=PXE"), "DomainAdminMBean");
		DomainAdminMBean _domainAdminMBean = (DomainAdminMBean) resolveJmx(adminName, DomainAdminMBean.class);
		SystemDescriptor desc = sdb.getDescriptor();
		ObjectName systemName = _domainAdminMBean.getSystem(desc.getName());
		SystemAdminMBean system = (SystemAdminMBean) resolveJmx(systemName, SystemAdminMBean.class);
		if (systemName != null && system != null) {
			logger.info("System '" + desc.getName() + "' already exist.  Undeploying...");
			try {
				system.undeploy();
			} catch (Exception e1) {
				logger.error("Error undeploying system '" + desc.getName() + "'", e1);
				return false;
			}
		}
		try {
			systemName = _domainAdminMBean.deploySystemBundle(sdb);
			system = (SystemAdminMBean) resolveJmx(systemName, SystemAdminMBean.class);
			system.enable();
		} catch (PxeSystemException e1) {
			logger.error("Error deploying system '" + desc.getName() + "'", e1);
			return false;
		}

		logger.info("System " + sdb.getDescriptor().getName() + " successfully deployed.");
		return true;
	}

	private Object resolveJmx(ObjectName oname, Class mbeanClass) {
		return MBeanServerInvocationHandler.newProxyInstance(this.server,
				oname, mbeanClass, true);
	}

	public void activateService(ServiceContext service) {
		try {
			ServicePort[] ports = service.getImports();
			if (ports != null && ports.length > 0) {
				Definition def = service.getSystemWSDL();
				QName serviceName = new QName(def.getTargetNamespace(), service.getServiceName());
				logger.info("Activating service: " + serviceName);
				List eps = new ArrayList();
				for (int i = 0; i < ports.length; i++) {
					logger.info("Creating endpoint: " + ports[i].getPortName());
					ServiceEndpoint endpoint = this.context.activateEndpoint(serviceName, ports[i].getPortName());
					eps.add(endpoint);
				}
				this.endpoints.put(service.getServiceUUID(), eps);
				this.services.put(service.getServiceUUID(), service);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deactivateService(ServiceContext service) {
		try {
			String id = service.getServiceUUID();
			this.services.remove(id);
			List eps = (List) this.endpoints.remove(id);
			if (eps != null) {
				for (Iterator iter = eps.iterator(); iter.hasNext();) {
					ServiceEndpoint se = (ServiceEndpoint) iter.next();
					this.context.deactivateEndpoint(se);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Process message exchange from JBI
	 */
	protected void process(MessageExchange me) {
		// Ignore consumer messages as we only handle in-only messages
		// These are to status DONE
		if (me.getRole() == MessageExchange.Role.CONSUMER) {
			return;
		}
		try {
			if (isInOnly(me)) {
				throw new Exception("Unsupporte mep pattern: " + me.getPattern());
			}
			String svcName = me.getEndpoint().getServiceName().getLocalPart();
			String port    = me.getEndpoint().getEndpointName();
			String oper    = me.getOperation().getLocalPart();
			for (Iterator it = this.services.values().iterator(); it.hasNext();) {
				ServiceContext service = (ServiceContext) it.next();
				if (svcName.equals(service.getServiceName())) {
					ServicePort svcPort = service.getImport(port);
					if (svcPort != null) {
						TransactionManager mgr = (TransactionManager) this.context.getTransactionManager();
						Transaction tx = mgr.getTransaction();
						try {
							if (tx == null) {
								mgr.begin();
							}
							DOMResult r = new DOMResult();
							TransformerFactory.newInstance().newTransformer().transform(me.getMessage("in").getContent(), r);
							com.fs.pxe.sfwk.spi.MessageExchange mePxe = service.createMessageExchange(svcPort, null, oper);
							Message im = mePxe.createInputMessage();
							im.setMessage(((Document) r.getNode()).getDocumentElement());
							mePxe.input(im);
							break;
						} finally {
							if (tx == null) {
								mgr.commit();
							}
						}
					}
				}
			}
			me.setStatus(ExchangeStatus.DONE);
		} catch (Exception e) {
			logger.error("Error handling incoming message", e);
			if (me.getPattern().equals(IN_ONLY_PATTERN)) {
				try {
					me.setStatus(ExchangeStatus.DONE);
				} catch (MessagingException e2) {
					logger.error("Error setting message status to DONE", e2);
				}
			} else {
				me.setError(e);
			}
		}
		try {
			this.context.getDeliveryChannel().send(me);
		} catch (MessagingException e) {
			logger.error("Error sending message", e);
		}
	}
	
	/**
	 * Process message exchange from pxe
	 */
	public void onMessageExchange(MessageExchangeEvent event) {
		try {
		    if (event.getEventType() == MessageExchangeEvent.IN_RCVD_EVENT) {
		    	com.fs.pxe.sfwk.spi.MessageExchange me = event.getMessageExchange();
		    	Message input = me.lastInput();
		    	System.err.println(input);
		    	System.err.println(DOMUtils.domToString(input.getMessage()));
		    	ServiceContext service = event.getTargetService();
		    	Definition def = service.getSystemWSDL();
		    	QName serviceName = new QName(def.getTargetNamespace(), service.getServiceName());
		    	String portName = event.getPort().getPortName();
		    	String operation = me.getName();
		    	System.err.println("Sending message to " + serviceName + " at " + portName);
		    	ServiceEndpoint se = this.context.getEndpoint(serviceName, portName);
		    	MessageExchangeFactory mef = this.context.getDeliveryChannel().createExchangeFactory(se);
		    	InOnly in = mef.createInOnlyExchange();
		    	in.setOperation(new QName(def.getTargetNamespace(), operation));
		    	NormalizedMessage m = in.createMessage();
		    	m.setContent(new DOMSource(input.getMessage()));
		    	in.setInMessage(m);
		    	this.context.getDeliveryChannel().send(in);
		    	
		    } else {
		    	// TODO : handle other types
		    	//   especially response to requests
		    	throw new UnsupportedOperationException();
		    }
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
