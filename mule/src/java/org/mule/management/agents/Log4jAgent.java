/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.agents;

import org.apache.log4j.jmx.HierarchyDynamicMBean;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.UMOAgent;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.config.i18n.Messages;
import org.mule.config.i18n.Message;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

/**
 * <code>Log4jAgent</code> exposes the configuration of the Log4J instance running
 * in Mule for Jmx management 
 *
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class Log4jAgent implements UMOAgent {

	private String name;
	private MBeanServer mBeanServer;
	
	/* (non-Javadoc)
	 * @see org.mule.umo.UMOAgent#getName()
	 */
	public String getName() {
		return this.name;
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOAgent#setName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.UMOAgent#getDescription()
	 */
	public String getDescription() {
		return "Log4j agent";
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.lifecycle.Initialisable#initialise()
	 */
	public void initialise() throws InitialisationException {
		try {
			mBeanServer = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);
			//mBeanServer.registerMBean(new HierarchyMBeanImpl(), new ObjectName("Log4j:type=Hierarchy"));
			mBeanServer.registerMBean(new HierarchyDynamicMBean(), new ObjectName("log4j:type=Hierarchy"));
		} catch (Exception e) {
			throw new InitialisationException(new Message(Messages.FAILED_TO_START_X, "JMX Agent"), e);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.lifecycle.Startable#start()
	 */
	public void start() throws UMOException {
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.lifecycle.Stoppable#stop()
	 */
	public void stop() throws UMOException {
	}

	/* (non-Javadoc)
	 * @see org.mule.umo.lifecycle.Disposable#dispose()
	 */
	public void dispose() throws UMOException {
	}

/* (non-Javadoc)
	 * @see org.mule.umo.UMOAgent#registered()
	 */
    public void registered()
    {
    }

    /* (non-Javadoc)
	 * @see org.mule.umo.UMOAgent#unregistered()
	 */
    public void unregistered()
    {
    }
    

}
