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
package org.mule.jbi;

import org.mule.jbi.nmr.InternalMessageRouter;
import org.mule.jbi.registry.Registry;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.util.queue.QueueSession;

import javax.jbi.JBIException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;
import java.io.File;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface JbiContainer {

	/**
	 * Access to the deployement registry.
	 * @return
	 */
	Registry getRegistry();
	
	/**
	 * Access to the endpoints registry.
	 * @return
	 */
	Endpoints getEndpoints();
	
	/**
	 * Router
	 * @return
	 */
	InternalMessageRouter getRouter();
	
	MBeanServer getMBeanServer();

	TransactionManager getTransactionManager();

	String getJmxDomainName();

	InitialContext getNamingContext();
	
	File getWorkingDir();

	ObjectName createMBeanName(String componentName, String type, String name);

	void initialize() throws JBIException;
	
	void start() throws JBIException;
	
	void shutDown() throws JBIException;
	
	public static class Factory {
		private static JbiContainer instance;
		public static JbiContainer getInstance() {
			return Factory.instance;
		}
		public static void setInstance(JbiContainer instance) {
			Factory.instance = instance;
		}
	}

    UMOContainerContext getObjectContainer();

    void addObjectContainer(UMOContainerContext container) throws JBIException;

    UMOContainerContext removeObjectContainer(UMOContainerContext container);
    
    QueueSession getQueueSession();
}
