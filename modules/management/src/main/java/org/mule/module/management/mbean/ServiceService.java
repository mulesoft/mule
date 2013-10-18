/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleConfiguration;
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
public class ServiceService extends FlowConstructService implements ServiceServiceMBean, MBeanRegistration, ServiceStatsMBean
{
    /**
     * logger used by this class
     */
    private static Log LOGGER = LogFactory.getLog(ServiceService.class);

    private ServiceStatistics  statistics;

    public ServiceService(String name, MuleContext muleContext)
    {
        super("Service", name, muleContext);
        this.statistics = getComponent().getStatistics();
        super.statistics = statistics;
    }

    public int getQueueSize()
    {
        Service c = getComponent();
        if (c instanceof SedaService)
        {
            return (int) c.getStatistics().getQueuedEvents();
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
     * @throws MuleException if the service failed to pause.
     * @see MuleConfiguration
     */
    public void pause() throws MuleException
    {
        getComponent().pause();
    }

    /**
     * Resumes the Service that has been paused. If the service is not paused
     * nothing is executed.
     *
     * @throws MuleException if the service failed to resume
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

    @Override
    public ObjectName getStatistics()
    {
        return statsName;
    }

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception
    {
        return super.preRegister(server, name);
    }

    @Override
    public void postRegister(Boolean registrationDone)
    {
        try
        {
            if (getComponent().getStatistics() != null)
            {
                statsName = jmxSupport.getObjectName(String.format("%s:type=org.mule.Statistics,service=%s", objectName.getDomain(), 
                     jmxSupport.escape(getName())));
                
                // unregister old version if exists
                if (server.isRegistered(statsName))
                {
                    server.unregisterMBean(statsName);
                }

                server.registerMBean(new ServiceStats(getComponent().getStatistics()), statsName);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error post-registering the MBean", e);
        }
    }

    @Override
    public void preDeregister() throws Exception
    {
        super.preDeregister();
    }

    @Override
    public void postDeregister()
    {
        super.postDeregister();
    }

    private AbstractService getComponent()
    {
        return (AbstractService)muleContext.getRegistry().lookupService(getName());
    }

    // ///// Service stats impl /////////
    @Override
    public void clearStatistics()
    {
        statistics.clear();
    }

    @Override
    public long getAsyncEventsReceived()
    {
        return statistics.getAsyncEventsReceived();
    }

    @Override
    public long getSyncEventsReceived()
    {
        return statistics.getSyncEventsReceived();
    }

    @Override
    public long getTotalEventsReceived()
    {
        return statistics.getTotalEventsReceived();
    }

    public long getAsyncEventsSent()
    {
        return statistics.getAsyncEventsSent();
    }

    public long getAverageExecutionTime()
    {
        return statistics.getAverageExecutionTime();
    }

    public long getAverageQueueSize()
    {
        return statistics.getAverageQueueSize();
    }

    public long getExecutedEvents()
    {
        return statistics.getExecutedEvents();
    }

    public long getMaxExecutionTime()
    {
        return statistics.getMaxExecutionTime();
    }

    public long getMaxQueueSize()
    {
        return statistics.getMaxQueueSize();
    }

    public long getMinExecutionTime()
    {
        return statistics.getMinExecutionTime();
    }

    public long getQueuedEvents()
    {
        return statistics.getQueuedEvents();
    }

    public long getReplyToEventsSent()
    {
        return statistics.getReplyToEventsSent();
    }

    public long getSyncEventsSent()
    {
        return statistics.getSyncEventsSent();
    }

    public long getTotalEventsSent()
    {
        return statistics.getTotalEventsSent();
    }

    public long getTotalExecutionTime()
    {
        return statistics.getTotalExecutionTime();
    }
}
