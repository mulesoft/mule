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
package org.mule.management.mbeans;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.impl.MuleComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOSession;

/**
 * <code>ComponentService</code> exposes service information about a Mule
 * Managed component
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ComponentService implements ComponentServiceMBean, MBeanRegistration
{

	/**
	 * logger used by this class
	 */
	private static transient Log LOGGER = LogFactory.getLog(ComponentService.class);
	
    private MBeanServer server;

    private String name;

    private ObjectName statsName;

    private ObjectName objectName;

    public ComponentService(String name)
    {
        this.name = name;

    }

    public String getName()
    {
        return name;
    }

    public int getQueueSize()
    {
        return getComponent().getQueueSize();
    }

    // public String printStatistics()
    // {
    // MuleComponent c = getComponent();
    // if(!c.getStatistics().isEnabled()) {
    // return "Statistics not enabled on this component: " + name;
    // }
    // StringWriter writer = new StringWriter();
    // HtmlTablePrinter printer = new HtmlTablePrinter(writer);
    // c.getStatistics().logSummary(printer);
    // return writer.toString();
    // }

    /**
     * Pauses event processing for theComponent. Unlike stop(), a paused
     * component will still consume messages from the underlying transport, but
     * those messages will be queued until the component is resumed. <p/> In
     * order to persist these queued messages you can set the 'recoverableMode'
     * property on the Muleconfiguration to true. this causes all internal
     * queues to store their state.
     * 
     * @throws org.mule.umo.UMOException if the component failed to pause.
     * @see org.mule.config.MuleConfiguration
     */
    public void pause() throws UMOException
    {
        getComponent().pause();
    }

    /**
     * Resumes the Component that has been paused. If the component is not
     * paused nothing is executed.
     * 
     * @throws org.mule.umo.UMOException if the component failed to resume
     */
    public void resume() throws UMOException
    {
        getComponent().resume();
    }

    public boolean isPaused()
    {
        return getComponent().isPaused();
    }

    public boolean isStopped()
    {
        return getComponent().isStopped();
    }

    public void stop() throws UMOException
    {
        getComponent().stop();
    }

    public void dispose() throws UMOException
    {
        getComponent().dispose();
    }

    public void start() throws UMOException
    {
        getComponent().start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.management.mbeans.ComponentServiceMBean#getStatistics()
     */
    public ObjectName getStatistics()
    {
        return statsName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer,
     *      javax.management.ObjectName)
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception
    {
        this.server = server;
        this.objectName = name;
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     */
    public void postRegister(Boolean registrationDone) {
		try {
			if (getComponent().getStatistics() != null) {
				statsName = new ObjectName(objectName.getDomain()
						+ ":type=statistics,name=" + getName());
                // unregister old version if exists
                if (this.server.isRegistered(statsName)) {
                	this.server.unregisterMBean(statsName);
                }
				this.server.registerMBean(new ComponentStats(getComponent()
						.getStatistics()), this.statsName);
			}
		} catch (Exception e) {
			LOGGER.error("Error post-registering the MBean", e);
		}
	}

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.MBeanRegistration#preDeregister()
     */
    public void preDeregister() throws Exception
    {
        try {
            if (this.server.isRegistered(statsName)) {
                this.server.unregisterMBean(statsName);
            }
        } catch(Exception ex) {
            LOGGER.error("Error unregistering ComponentService child " + statsName.getCanonicalName(),
                        ex);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister()
    {
    }

    private MuleComponent getComponent()
    {
        UMOSession session = MuleManager.getInstance().getModel().getComponentSession(getName());
        if (session == null) {
            return null;
        } else {
            return (MuleComponent) session.getComponent();
        }
    }
}
