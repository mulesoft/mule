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

import java.util.Properties;

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

/**
 * TODO: document this class
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class QuartzConnector extends AbstractServiceEnabledConnector
{

    public static final String PROPERTY_CRON_EXPRESSION = "cronExpression";
    public static final String PROPERTY_REPEAT_INTERVAL = "repeatInterval";
    public static final String PROPERTY_REPEAT_COUNT = "repeatCount";
    public static final String PROPERTY_START_DELAY = "startDelay";
    public static final String PROPERTY_PAYLOAD = "payload";
    public static final String PROPERTY_PAYLOAD_CLASS_NAME = "payloadClassName";

    private String factoryClassName = StdSchedulerFactory.class.getName();

    private SchedulerFactory factory;

    private Properties factoryProperties;

    private Scheduler scheduler;

    public String getProtocol()
    {
        return "QUARTZ";
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
