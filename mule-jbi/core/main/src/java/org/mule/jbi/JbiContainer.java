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

import java.io.File;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

public interface JbiContainer extends LifeCycle {

	MBeanServer getMBeanServer();

	TransactionManager getTransactionManager();

	String getJmxDomainName();

	Router getRouter();

	InitialContext getNamingContext();
	
	File getWorkingDir();

	ObjectName createComponentMBeanName(String componentName, String type, String name);

	ComponentRegistry getComponentRegistry();

	EndpointRegistry getEndpointRegistry();

}
