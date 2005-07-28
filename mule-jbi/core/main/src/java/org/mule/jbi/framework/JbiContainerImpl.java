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

import java.io.File;
import java.rmi.registry.LocateRegistry;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jbi.JBIException;
import javax.jbi.management.AdminServiceMBean;
import javax.jbi.management.DeploymentServiceMBean;
import javax.jbi.management.InstallationServiceMBean;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.impl.container.MultiContainerContext;
import org.mule.impl.jndi.MuleInitialContextFactory;
import org.mule.jbi.Endpoints;
import org.mule.jbi.JbiContainer;
import org.mule.jbi.management.AdminService;
import org.mule.jbi.management.AutoInstallService;
import org.mule.jbi.management.AutoInstallServiceMBean;
import org.mule.jbi.management.DeploymentService;
import org.mule.jbi.management.Directories;
import org.mule.jbi.management.InstallationService;
import org.mule.jbi.nmr.DirectRouter;
import org.mule.jbi.nmr.InternalMessageRouter;
import org.mule.jbi.registry.Registry;
import org.mule.jbi.registry.RegistryIO;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.util.queue.QueueSession;
import org.objectweb.jotm.Jotm;


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
	
	private MBeanServer mBeanServer;
	private TransactionManager transactionManager;
	private String jmxDomainName;
	private Registry registry;
	private Endpoints endpoints;
	private File workingDir;
	private InitialContext namingContext;
    private MultiContainerContext objectContainer;
    private InternalMessageRouter router;

    private static java.rmi.registry.Registry rmiRegistry = null;
    
	public JbiContainerImpl() {
		this.workingDir = new File(DEFAULT_WORKING_DIR);
		this.jmxDomainName = DEFAULT_JMX_DOMAIN;
        objectContainer = new MultiContainerContext();
	}
	
	public MBeanServer getMBeanServer() {
		return this.mBeanServer;
	}

	public void setMBeanServer(MBeanServer mBeanServer) {
		this.mBeanServer = mBeanServer;
	}

	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	public String getJmxDomainName() {
		return this.jmxDomainName;
	}

	public void setJmxDomainName(String jmxDomainName) {
		this.jmxDomainName = jmxDomainName;
	}

	public InternalMessageRouter getRouter() {
		return this.router;
	}

	public void setRouter(InternalMessageRouter router) {
		this.router = router;
	}
	
	public InitialContext getNamingContext() {
		return this.namingContext;
	}
	
	public File getWorkingDir() {
		return this.workingDir;
	}

	public void setWorkingDir(File workingDir) {
		this.workingDir = workingDir;
	}
	
	public ObjectName createMBeanName(String componentName, String type, String name) {
		try {
			StringBuffer sb = new StringBuffer();
			sb.append(this.jmxDomainName).append(':');
			if (componentName != null) {
				sb.append("component=").append(validateString(componentName));
				sb.append(',');
			}
			sb.append("type=").append(validateString(type));
			if (name != null) {
				sb.append(',');
				sb.append("name=").append(validateString(name));
			}
			return new ObjectName(sb.toString());
		} catch (MalformedObjectNameException e) {
			LOGGER.error("Could not create component mbean name", e);
			return null;
		}
	}

    private String validateString(String str) {
    	str = str.replace(':', '_');
    	str = str.replace('/', '_');
    	str = str.replace('\\', '_');
    	return str;
    }

	public Registry getRegistry() {
		return this.registry;
	}

	public Endpoints getEndpoints() {
		return this.endpoints;
	}

	public void initialize() throws JBIException {
		try {
			JbiContainer.Factory.setInstance(this);
			Directories.createDirectories(this.workingDir);
            //initialise a Mule instance that will manage our connections
            MuleManager.getConfiguration().setEmbedded(true);
            MuleManager.getInstance().setTransactionManager(transactionManager);
            //todo should set workmanager too
            ((MuleManager)MuleManager.getInstance()).initialise();
			if (this.mBeanServer == null) {
				LOGGER.debug("Creating MBeanServer");
				List l = MBeanServerFactory.findMBeanServer(null);
				if (l != null && l.size() > 0) {
					this.mBeanServer = (MBeanServer) l.get(0);
				} else {
					this.mBeanServer = MBeanServerFactory.createMBeanServer();
				}
			}
			// Create jmx remote access
			if (rmiRegistry == null) {
				rmiRegistry = LocateRegistry.createRegistry(DEFAULT_PORT);
			}
			String url = MessageFormat.format(DEFAULT_URL, new Object[] { DEFAULT_HOST, Integer.toString(DEFAULT_PORT) });
			Map env = new HashMap();
			env.put(RMIConnectorServer.JNDI_REBIND_ATTRIBUTE, "true");
			JMXConnectorServer con = JMXConnectorServerFactory.newJMXConnectorServer(new JMXServiceURL(url), env, this.mBeanServer);
			con.start();
			
			if (this.transactionManager == null) {
				LOGGER.debug("Creating TransactionManager");
				this.transactionManager = new Jotm(true, false).getTransactionManager();
				this.transactionManager.setTransactionTimeout(60);
			}
			if (this.namingContext == null) {
			    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, MuleInitialContextFactory.class.getName());
			    System.setProperty(Context.PROVIDER_URL, "mule-jbi");
			    this.namingContext = new InitialContext();
			}
			this.endpoints = new EndpointsImpl();
			File regStore = new File(this.workingDir, "/registry.xml");
			if (regStore.isFile()) {
				try {
					this.registry = RegistryIO.load(regStore);
				} catch (Exception e) {
					LOGGER.warn("Invalid registry found. Creating a new one");
				}
			} else {
				LOGGER.info("No registry found. Creating a new one");
			}
			if (this.registry == null) {
				this.registry = RegistryIO.create(regStore);
			}
			if (this.router == null) {
				this.router = new InternalMessageRouter(this, new DirectRouter(registry));
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
			if (this.registry == null) {
				initialize();
			}
			AdminService admin = new AdminService(this);
			registerMBean(new StandardMBean(admin, AdminServiceMBean.class), createMBeanName(null, "service", "admin"));
			InstallationService install = new InstallationService(this);
			registerMBean(new StandardMBean(install, InstallationServiceMBean.class), createMBeanName(null, "service", "install"));
			DeploymentService deploy = new DeploymentService(this);
			registerMBean(new StandardMBean(deploy, DeploymentServiceMBean.class), createMBeanName(null, "service", "deploy"));
			AutoInstallService autoinstall = new AutoInstallService();
			registerMBean(new StandardMBean(autoinstall, AutoInstallServiceMBean.class), createMBeanName(null, "service", "autoinstall"));
			// TODO: debug only
			autoinstall.setPollingFrequency(1000);
			autoinstall.start();
			this.registry.start();
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
			this.registry.shutDown();
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
		if (this.mBeanServer.isRegistered(name)) {
			this.mBeanServer.unregisterMBean(name);
		}
	}
	
	private void registerMBean(Object mbean, ObjectName name) throws JMException {
		unregisterMBean(name);
		this.mBeanServer.registerMBean(mbean, name);
	}

	public void setNamingContext(InitialContext namingContext) {
		this.namingContext = namingContext;
	}

    /**
     * associatesone or more Dependency Injector/Jndi containers with this container.
     * This can be used to integrate container managed resources with Mule resources
     *
     * @param container a Container context to use. By default, there is a
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
}
