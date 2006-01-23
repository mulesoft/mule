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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.impl.model.AbstractComponent;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.management.stats.ComponentStatistics;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOSession;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * <code>ComponentService</code> exposes service information about a Mule
 * Managed component
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ComponentService implements ComponentServiceMBean, MBeanRegistration, ComponentStatsMBean
{

	/**
	 * logger used by this class
	 */
	private static transient Log LOGGER = LogFactory.getLog(ComponentService.class);
	
    private MBeanServer server;

    private String name;

    private ObjectName statsName;

    private ObjectName objectName;

    private ComponentStatistics statistics;

    public ComponentService(String name)
    {
        this.name = name;
        this.statistics = getComponent().getStatistics();

    }

    public int getQueueSize()
    {
        UMOComponent c = getComponent();
        if(c instanceof SedaComponent) {
            return ((SedaComponent)c).getQueueSize();
        } else {
            return -1;
        }
    }

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

    public void forceStop() throws UMOException {
        getComponent().forceStop();
    }

    public boolean isStopping() {
        return getComponent().isStopping();
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
						+ ":type=org.mule.Statistics,component=" + getName());
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

    private AbstractComponent getComponent()
    {
        UMOSession session = MuleManager.getInstance().getModel().getComponentSession(getName());
        if (session == null) {
            return null;
        } else {
            return (AbstractComponent)session.getComponent();
        }
    }

    /////// Component stats impl /////////

    /**
     *
     */
    public void clearStatistics()
    {
        statistics.clear();
    }

    /**
     * @return
     */
    public long getAsyncEventsReceived()
    {
        return statistics.getAsyncEventsReceived();
    }

    /**
     * @return
     */
    public long getAsyncEventsSent()
    {
        return statistics.getAsyncEventsSent();
    }

    /**
     * @return
     */
    public long getAverageExecutionTime()
    {
        return statistics.getAverageExecutionTime();
    }

    /**
     * @return
     */
    public long getAverageQueueSize()
    {
        return statistics.getAverageQueueSize();
    }

    /**
     * @return
     */
    public long getExecutedEvents()
    {
        return statistics.getExecutedEvents();
    }

    /**
     * @return
     */
    public long getExecutionErrors()
    {
        return statistics.getExecutionErrors();
    }

    /**
     * @return
     */
    public long getFatalErrors()
    {
        return statistics.getFatalErrors();
    }

    /**
     * @return
     */
    public long getMaxExecutionTime()
    {
        return statistics.getMaxExecutionTime();
    }

    /**
     * @return
     */
    public long getMaxQueueSize()
    {
        return statistics.getMaxQueueSize();
    }

    /**
     * @return
     */
    public long getMinExecutionTime()
    {
        return statistics.getMinExecutionTime();
    }

    /**
     * @return
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return
     */
    public long getQueuedEvents()
    {
        return statistics.getQueuedEvents();
    }

    /**
     * @return
     */
    public long getReplyToEventsSent()
    {
        return statistics.getReplyToEventsSent();
    }

    /**
     * @return
     */
    public long getSyncEventsReceived()
    {
        return statistics.getSyncEventsReceived();
    }

    /**
     * @return
     */
    public long getSyncEventsSent()
    {
        return statistics.getSyncEventsSent();
    }

    /**
     * @return
     */
    public long getTotalEventsReceived()
    {
        return statistics.getTotalEventsReceived();
    }

    /**
     * @return
     */
    public long getTotalEventsSent()
    {
        return statistics.getTotalEventsSent();
    }

    /**
     * @return
     */
    public long getTotalExecutionTime()
    {
        return statistics.getTotalExecutionTime();
    }
}
