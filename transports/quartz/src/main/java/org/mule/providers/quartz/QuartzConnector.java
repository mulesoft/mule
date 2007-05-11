/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.quartz;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractConnector;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.ConnectorException;

import java.util.Properties;

import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Creates a connection to a Quartz sheduler. This allows events to be sheduled at
 * specific times, with repeated occurences.
 */
public class QuartzConnector extends AbstractConnector
{

    public static final String PROPERTY_CRON_EXPRESSION = "cronExpression";
    public static final String PROPERTY_REPEAT_INTERVAL = "repeatInterval";
    public static final String PROPERTY_REPEAT_COUNT = "repeatCount";
    public static final String PROPERTY_START_DELAY = "startDelay";
    public static final String PROPERTY_PAYLOAD = "payload";
    public static final String PROPERTY_JOB_DISPATCH_ENDPOINT = "jobDispatchEndpoint";
    public static final String PROPERTY_JOB_RECEIVE_ENDPOINT = "jobReceiveEndpoint";
    public static final String PROPERTY_JOB_RECEIVE_TIMEOUT = "jobReceiveTimeout";

    /** deprecated: use PROPERTY_PAYLOAD_REFERENCE */
    public static final String PROPERTY_PAYLOAD_CLASS_NAME = "payloadClassName";

    public static final String PROPERTY_PAYLOAD_REFERENCE = "payloadRef";
    public static final String PROPERTY_GROUP_NAME = "groupName";
    public static final String PROPERTY_JOB_GROUP_NAME = "jobGroupName";
    public static final String PROPERTY_JOB_REF = "jobRef";
    public static final String PROPERTY_JOB_CLASS = "jobClass";
    public static final String PROPERTY_JOB_OBJECT = "jobObject";

    public static final String DEFAULT_GROUP_NAME = "mule";

    /**
     * Properties to be used for creating the scheduler.  If no properties are given, the 
     * scheduler will be created by StdSchedulerFactory.getDefaultScheduler()
     */
    private Properties factoryProperties = null;

    /**
     * The scheduler instance.  This can be configured by the user and injected as a bean
     * or if not, it will be created by Mule upon initialization.  
     */
    private Scheduler quartzScheduler = null;

    protected void doInitialise() throws InitialisationException
    {
        try
        {
            if (quartzScheduler == null)
            {
                if (factoryProperties != null)
                {
                    SchedulerFactory factory = new StdSchedulerFactory(factoryProperties);
                    quartzScheduler = factory.getScheduler();
                }
                else 
                {
                    quartzScheduler = StdSchedulerFactory.getDefaultScheduler();
                }
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.INITIALISATION_FAILURE_X,
                "Quartz provider"), e);
        }
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws UMOException
    {
        try
        {
            quartzScheduler.start();
        }
        catch (Exception e)
        {
            throw new ConnectorException(new Message(Messages.FAILED_TO_START_X, "Quartz provider"), this, e);
        }
    }

    protected void doStop() throws UMOException
    {
        try
        {
            if (quartzScheduler != null)
            {
                quartzScheduler.shutdown();
            }
        }
        catch (Exception e)
        {
            throw new ConnectorException(new Message(Messages.FAILED_TO_STOP_X, "Quartz provider"), this, e);
        }
    }

    public String getProtocol()
    {
        return "quartz";
    }

    public Scheduler getQuartzScheduler()
    {
        return quartzScheduler;
    }

    public void setQuartzScheduler(Scheduler scheduler)
    {
        this.quartzScheduler = scheduler;
    }

    public Properties getFactoryProperties()
    {
        return factoryProperties;
    }

    public void setFactoryProperties(Properties factoryProperties)
    {
        this.factoryProperties = factoryProperties;
    }

}
