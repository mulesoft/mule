/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import org.mule.MuleServer;
import org.mule.api.MuleException;
import org.mule.api.service.Service;
import org.mule.management.stats.ServiceStatistics;
import org.mule.model.seda.SedaService;
import org.mule.service.AbstractService;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ServiceService</code> exposes service information about a Mule Managed
 * service.
 */
public class ServiceService implements ServiceServiceMBean, MBeanRegistration, ServiceStatsMBean
{

    /**
     * logger used by this class
     */
    private static Log LOGGER = LogFactory.getLog(ServiceService.class);

    private MBeanServer server;

    private String name;

    private ObjectName statsName;

    private ObjectName objectName;

    private ServiceStatistics statistics;

    public ServiceService(String name)
    {
        this.name = name;
        this.statistics = getComponent().getStatistics();

    }

    public int getQueueSize()
    {
        Service c = getComponent();
        if (c instanceof SedaService)
        {
            return ((SedaService)c).getQueueSize();
        }
        else
        {
            return -1;
        }
    }

    /**
     * Pauses event processing for theComponent. Unlike stop(), a paused service
     * will still consume messages from the underlying transport, but those messages
     * will be queued until the service is resumed. <p/> In order to persist these
     * queued messages you can set the 'recoverableMode' property on the
     * Muleconfiguration to true. this causes all internal queues to store their
     * state.
     * 
     * @throws org.mule.api.MuleException if the service failed to pause.
     * @see org.mule.config.MuleConfiguration
     */
    public void pause() throws MuleException
    {
        getComponent().pause();
    }

    /**
     * Resumes the Service that has been paused. If the service is not paused
     * nothing is executed.
     * 
     * @throws org.mule.api.MuleException if the service failed to resume
     */
    public void resume() throws MuleException
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

    public void stop() throws MuleException
    {
        getComponent().stop();
    }

    public void forceStop() throws MuleException
    {
        getComponent().forceStop();
    }

    public boolean isStopping()
    {
        return getComponent().isStopping();
    }

    public void dispose() throws MuleException
    {
        getComponent().dispose();
    }

    public void start() throws MuleException
    {
        getComponent().start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.management.mbeans.ServiceServiceMBean#getStatistics()
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
    public void postRegister(Boolean registrationDone)
    {
        try
        {
            if (getComponent().getStatistics() != null)
            {
                statsName = new ObjectName(objectName.getDomain() + ":type=org.mule.Statistics,service="
                                           + getName());
                // unregister old version if exists
                if (this.server.isRegistered(statsName))
                {
                    this.server.unregisterMBean(statsName);
                }

                this.server.registerMBean(new ServiceStats(getComponent().getStatistics()), this.statsName);
            }
        }
        catch (Exception e)
        {
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
        try
        {
            if (this.server.isRegistered(statsName))
            {
                this.server.unregisterMBean(statsName);
            }
        }
        catch (Exception ex)
        {
            LOGGER.error("Error unregistering ServiceService child " + statsName.getCanonicalName(), ex);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister()
    {
        // nothing to do
    }

    private AbstractService getComponent()
    {
        return (AbstractService)MuleServer.getMuleContext().getRegistry().lookupService(getName());
    }

    // ///// Service stats impl /////////

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
