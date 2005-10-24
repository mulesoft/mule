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

package org.mule.providers.quartz;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.ConnectorException;
import org.mule.util.ClassHelper;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;

/**
 * Creates a connection to a Quartz sheduler.  This allows events to be sheduled
 * at specific times, with repeat occurences
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class QuartzConnector extends AbstractServiceEnabledConnector
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

    private String factoryClassName = StdSchedulerFactory.class.getName();

    private SchedulerFactory factory;

    private Properties factoryProperties;

    private Scheduler scheduler;

    public String getProtocol()
    {
        return "quartz";
    }

    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        try {
            if (scheduler == null) {
                if (factory == null) {
                    Object[] args = null;
                    if (factoryProperties != null) {
                        args = new Object[] { factoryProperties };
                    }
                    factory = (SchedulerFactory) ClassHelper.instanciateClass(factoryClassName, args);
                }
                scheduler = factory.getScheduler();
            }
        } catch (Exception e) {
            throw new InitialisationException(new Message(Messages.INITIALISATION_FAILURE_X, "Quartz provider"), e);
        }
    }

    protected void doStart() throws UMOException
    {
        try {
            scheduler.start();
        } catch (Exception e) {
            throw new ConnectorException(new Message(Messages.FAILED_TO_START_X, "Quartz provider"), this, e);
        }
    }

    protected void doStop() throws UMOException
    {
        try {
            if (scheduler != null) {
                scheduler.shutdown();
            }
        } catch (Exception e) {
            throw new ConnectorException(new Message(Messages.FAILED_TO_STOP_X, "Quartz provider"), this, e);
        }
    }

    public SchedulerFactory getFactory()
    {
        return factory;
    }

    public void setFactory(SchedulerFactory factory)
    {
        this.factory = factory;
    }

    public Scheduler getScheduler()
    {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public String getFactoryClassName()
    {
        return factoryClassName;
    }

    public void setFactoryClassName(String factoryClassName)
    {
        this.factoryClassName = factoryClassName;
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
