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
package org.mule.jbi.framework;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.ManagementContext;
import org.mule.MuleManager;
import org.mule.impl.container.MultiContainerContext;
import org.mule.impl.jndi.MuleInitialContextFactory;
import org.mule.jbi.Endpoints;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.management.*;
import org.mule.jbi.nmr.DirectRouter;
import org.mule.jbi.nmr.InternalMessageRouter;
import org.mule.jbi.registry.JbiRegistryFactory;
import org.mule.registry.Registry;
import org.mule.registry.store.XmlRegistryStore;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.util.queue.QueueSession;
import org.objectweb.jotm.Jotm;

import javax.jbi.JBIException;
import javax.jbi.management.AdminServiceMBean;
import javax.jbi.management.DeploymentServiceMBean;
import javax.jbi.management.InstallationServiceMBean;
import javax.management.*;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;
import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class JbiContainerImpl implements JbiContainer {

	private static final String DEFAULT_WORKING_DIR = ".mule-jbi";
	private static final String DEFAULT_JMX_DOMAIN = "mule-jbi";

    public static final String DEFAULT_URL = "service:jmx:rmi:///jndi/rmi://{0}:{1}/server";
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 8000;
	
	private static final Log LOGGER = LogFactory.getLog(JbiContainer.class);
	
	private Endpoints endpoints;
    private MultiContainerContext objectContainer;
    private InternalMessageRouter router;
    private ManagementContext context;

    private static java.rmi.registry.Registry rmiRegistry = null;
    
	public JbiContainerImpl() {
        context = new ManagementContext();
		context.setWorkingDir(new File(DEFAULT_WORKING_DIR));
		context.setJmxDomainName(DEFAULT_JMX_DOMAIN);
        objectContainer = new MultiContainerContext();
	}
	
	public MBeanServer getMBeanServer() {
		return context.getMBeanServer();
	}

	public void setMBeanServer(MBeanServer mBeanServer) {
		context.setMBeanServer(mBeanServer);
	}

	public TransactionManager getTransactionManager() {
		return context.getTransactionManager();
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		context.setTransactionManager(transactionManager);
	}
	
	public String getJmxDomainName() {
		return context.getJmxDomainName();
	}

	public void setJmxDomainName(String jmxDomainName) {
		context.setJmxDomainName(jmxDomainName);
	}

	public InternalMessageRouter getRouter() {
		return this.router;
	}

	public void setRouter(InternalMessageRouter router) {
		this.router = router;
	}
	
	public InitialContext getNamingContext() {
		return context.getNamingContext();
	}
	
	public File getWorkingDir() {
		return context.getWorkingDir();
	}

	public void setWorkingDir(File workingDir) {
		context.setWorkingDir(workingDir);
	}
	
	public ObjectName createMBeanName(String componentName, String type, String name) {
		return context.createMBeanName(componentName, type, name);
	}


	public Registry getRegistry() {
        return context.getRegistry();
	}

	public Endpoints getEndpoints() {
		return this.endpoints;
	}

	public void initialize() throws JBIException {
		try {
			JbiContainer.Factory.setInstance(this);
			Directories.createDirectories(getWorkingDir());
            //initialise a Mule instance that will manage our connections
            MuleManager.getConfiguration().setEmbedded(true);
            MuleManager.getInstance().setTransactionManager(getTransactionManager());
            //todo should set workmanager too
            ((MuleManager)MuleManager.getInstance()).initialise();
			if (getMBeanServer() == null) {
				LOGGER.debug("Creating MBeanServer");
				List l = MBeanServerFactory.findMBeanServer(null);
				if (l != null && l.size() > 0) {
					setMBeanServer((MBeanServer) l.get(0));
				} else {
					setMBeanServer(MBeanServerFactory.createMBeanServer());
				}
			}
			// Create jmx remote access
			if (rmiRegistry == null) {
				rmiRegistry = LocateRegistry.createRegistry(DEFAULT_PORT);
			}
			String url = MessageFormat.format(DEFAULT_URL, new Object[] { DEFAULT_HOST, Integer.toString(DEFAULT_PORT) });
			Map env = new HashMap();
			env.put(RMIConnectorServer.JNDI_REBIND_ATTRIBUTE, "true");
			JMXConnectorServer con = JMXConnectorServerFactory.newJMXConnectorServer(new JMXServiceURL(url), env, getMBeanServer());
			con.start();
			
			if (getTransactionManager() == null) {
				LOGGER.debug("Creating TransactionManager");
				setTransactionManager(new Jotm(true, false).getTransactionManager());
				getTransactionManager().setTransactionTimeout(60);
			}
			if (getNamingContext() == null) {
			    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, MuleInitialContextFactory.class.getName());
			    System.setProperty(Context.PROVIDER_URL, "mule-jbi");
			    setNamingContext(new InitialContext());
			}
			this.endpoints = new EndpointsImpl();
			File regStore = new File(getWorkingDir(), "/registry.xml");
			if (regStore.isFile()) {
				try {
                    //todo support other store types
					context.setRegistry(new XmlRegistryStore(context).load(regStore.getAbsolutePath()));
				} catch (Exception e) {
					LOGGER.warn("Invalid registry found. Creating a new one");
				}
			} else {
				LOGGER.info("No registry found. Creating a new one");
			}
			if (getRegistry() == null) {
				context.setRegistry(new XmlRegistryStore(context).create(regStore.getAbsolutePath(), new JbiRegistryFactory()));
			}
			if (this.router == null) {
				this.router = new InternalMessageRouter(this, new DirectRouter(getRegistry()));
			}
		} catch (Exception e) {
			if (e instanceof JBIException) {
				throw (JBIException) e;
			} else {
				throw new JBIException(e);
			}
		}
	}
	
	public void start() throws JBIException {
		try {
			LOGGER.info("Starting JBI");
			if (getRegistry() == null) {
				initialize();
			}
			AdminService admin = new AdminService(context);
			registerMBean(new StandardMBean(admin, AdminServiceMBean.class), createMBeanName(null, "service", "admin"));
			InstallationService install = new InstallationService(context);
			registerMBean(new StandardMBean(install, InstallationServiceMBean.class), createMBeanName(null, "service", "install"));
			DeploymentService deploy = new DeploymentService(this);
			registerMBean(new StandardMBean(deploy, DeploymentServiceMBean.class), createMBeanName(null, "service", "deploy"));
			AutoInstallService autoinstall = new AutoInstallService();
			registerMBean(new StandardMBean(autoinstall, AutoInstallServiceMBean.class), createMBeanName(null, "service", "autoinstall"));
			// TODO: debug only
			autoinstall.setPollingFrequency(1000);
			autoinstall.start();
			getRegistry().start();
			Directories.deleteMarkedDirectories(getWorkingDir());
            MuleManager.getInstance().start();
		} catch (Exception e) {
			if (e instanceof JBIException) {
				throw (JBIException) e;
			} else {
				throw new JBIException(e);
			}
		}
	}
	
	public void shutDown() throws JBIException {
		try {
			unregisterMBean(createMBeanName(null, "service", "admin"));
			unregisterMBean(createMBeanName(null, "service", "install"));
			unregisterMBean(createMBeanName(null, "service", "deploy"));
			unregisterMBean(createMBeanName(null, "service", "autoinstall"));
			getRegistry().shutDown();
			JbiContainer.Factory.setInstance(null);
		} catch (Exception e) {
			if (e instanceof JBIException) {
				throw (JBIException) e;
			} else {
				throw new JBIException(e);
			}
		}
	}
	
	private void unregisterMBean(ObjectName name) throws JMException {
		if (getMBeanServer().isRegistered(name)) {
			getMBeanServer().unregisterMBean(name);
		}
	}
	
	private void registerMBean(Object mbean, ObjectName name) throws JMException {
		unregisterMBean(name);
		getMBeanServer().registerMBean(mbean, name);
	}

	public void setNamingContext(InitialContext namingContext) {
		context.setNamingContext(namingContext);
	}

    /**
     * associatesone or more Dependency Injector/Jndi containers with this container.
     * This can be used to integrate container managed resources with Mule resources
     *
     * @param container a Container container to use. By default, there is a
     *            default Mule container <code>MuleContainerContext</code>
     *            that will assume that the reference key for an oblect is a
     *            classname and will try to instanciate it.
     */
    public void addObjectContainer(UMOContainerContext container) throws JBIException
    {
        if (container == null) {
            if (objectContainer != null) {
                objectContainer.dispose();
            }
            objectContainer = new MultiContainerContext();
        } else {
            try {
                objectContainer.initialise();
            } catch (InitialisationException e) {
                throw new JBIException(e.getMessage(), e.getCause());
            }
            objectContainer.addContainer(container);
        }
    }

    public UMOContainerContext getObjectContainer() {
        return objectContainer;
    }

    public UMOContainerContext removeObjectContainer(UMOContainerContext container) {
        return objectContainer.removeContainer(container.getName());
    }
    
    public QueueSession getQueueSession() {
    	return MuleManager.getInstance().getQueueManager().getQueueSession();
    }

    public ManagementContext getManagementContext() {
        return context;
    }

    public void setManagementContext(ManagementContext context) {
        this.context = context;
    }
}
