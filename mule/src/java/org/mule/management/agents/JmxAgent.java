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
package org.mule.management.agents;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.MuleRuntimeException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.internal.events.ModelEvent;
import org.mule.impl.internal.events.ModelEventListener;
import org.mule.management.mbeans.*;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOServerEvent;

import javax.management.*;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>JmxAgent</code> registers MUle Jmx management beans
 * with an MBean server.
 *
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JmxAgent implements UMOAgent
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(JmxAgent.class);

    private String name;
    protected boolean locateServer = true;
    private boolean createServer = true;
    private String connectorServerUrl;
    private MBeanServer mBeanServer;
    private JMXConnectorServer connectorServer;
    private boolean enableStatistics = true;
    private List registeredMBeans = new ArrayList();

    /* (non-Javadoc)
     * @see org.mule.umo.manager.UMOAgent#getName()
     */
    public String getName()
    {
        return this.name;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.manager.UMOAgent#setName(java.lang.String)
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.manager.UMOAgent#getDescription()
     */
    public String getDescription()
    {
        if (connectorServerUrl != null)
        {
            return "JMX Agent: " + connectorServerUrl;
        } else
        {
            return "JMX Agent";
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.lifecycle.Initialisable#initialise()
     */
    public void initialise() throws InitialisationException
    {
        if (!locateServer && !createServer)
        {
            throw new InitialisationException(new Message(Messages.JMX_CREATE_OR_LOCATE_SHOULD_BE_SET), this);
        }
        if (mBeanServer == null && locateServer)
        {
            List l = MBeanServerFactory.findMBeanServer(null);
            if (l != null && l.size() > 0)
            {
                mBeanServer = (MBeanServer) l.get(0);
            }
        }
        if (mBeanServer == null && createServer)
        {
            mBeanServer = MBeanServerFactory.createMBeanServer();
        }
        if (mBeanServer == null)
        {
            throw new InitialisationException(new Message(Messages.JMX_CANT_LOCATE_CREATE_SERVER), this);
        }
        if (connectorServerUrl != null)
        {
            try
            {
                JMXServiceURL url = new JMXServiceURL(connectorServerUrl);
                connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mBeanServer);
            } catch (Exception e)
            {
                throw new InitialisationException(new Message(Messages.FAILED_TO_CREATE_X, "Jmx Connector"), e, this);
            }
        }

        //We need to register all the services once the server has initialised
        MuleManager.getInstance().registerListener(new
        ModelEventListener()
        {
            public void onEvent(UMOServerEvent event)
            {
                if (event.getAction() == ModelEvent.MODEL_INITIALISED)
                {
                    try
                    {
                        registerStatisticsService();
                        registerMuleService();
                        //registerConfigurationService();
                        registerModelService();
                        registerComponentServices();
                    } catch (Exception e)
                    {
                        throw new MuleRuntimeException(new Message(Messages.X_FAILED_TO_INITIALISE, "MBeans"), e);
                    }
                }
            }
        });

    }

    /* (non-Javadoc)
     * @see org.mule.umo.lifecycle.Startable#start()
     */
    public void start() throws UMOException
    {
        if (connectorServer != null)
        {
            try
            {
                connectorServer.start();
            } catch (Exception e)
            {
                throw new JmxManagementException(new Message(Messages.FAILED_TO_START_X, "Jmx Connector"), e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.lifecycle.Stoppable#stop()
     */
    public void stop() throws UMOException
    {
        if (connectorServer != null)
        {
            try
            {
                connectorServer.stop();
            } catch (Exception e)
            {
                throw new JmxManagementException(new Message(Messages.FAILED_TO_STOP_X, "Jmx Connector"), e);                
            }
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.lifecycle.Disposable#dispose()
     */
    public void dispose()
    {
        if(mBeanServer!=null) {
            for (Iterator iterator = registeredMBeans.iterator(); iterator.hasNext();)
            {
                ObjectName objectName = (ObjectName) iterator.next();
                try
                {
                    mBeanServer.unregisterMBean(objectName);
                } catch (Exception e)
                {
                    logger.warn("Failed to unregister MBean: " + objectName + ". Error is: " + e.getMessage());
                }
            }
            MBeanServerFactory.releaseMBeanServer(mBeanServer);
            mBeanServer = null;
        }
    }

/* (non-Javadoc)
	 * @see org.mule.umo.manager.UMOAgent#registered()
	 */
    public void registered()
    {
    }

    /* (non-Javadoc)
	 * @see org.mule.umo.manager.UMOAgent#unregistered()
	 */
    public void unregistered()
    {
    }
    

    protected void registerStatisticsService() throws NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, MalformedObjectNameException
    {
        ObjectName on = new ObjectName("Mule:type=statistics");
        StatisticsService mBean = new StatisticsService();
        mBean.setManager(MuleManager.getInstance());
        mBean.setEnabled(isEnableStatistics());
        mBeanServer.registerMBean(mBean, on);
        registeredMBeans.add(on);
    }

    protected void registerModelService() throws NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, MalformedObjectNameException
    {
        ObjectName on = new ObjectName("Mule:type=control,name=ModelService");
        ModelServiceMBean serviceMBean = new ModelService();
        mBeanServer.registerMBean(serviceMBean, on);
        registeredMBeans.add(on);
    }

    protected void registerMuleService() throws NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, MalformedObjectNameException
    {
        ObjectName on = new ObjectName("Mule:type=control,name=MuleService");
        MuleServiceMBean serviceMBean = new MuleService();
        mBeanServer.registerMBean(serviceMBean, on);
        registeredMBeans.add(on);
    }

    protected void registerConfigurationService() throws NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, MalformedObjectNameException
    {
        ObjectName on = new ObjectName("Mule:type=control,name=ConfigurationService");
        MuleConfigurationServiceMBean serviceMBean = new MuleConfigurationService();
        mBeanServer.registerMBean(serviceMBean, on);
        registeredMBeans.add(on);
    }

    protected void registerComponentServices() throws NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, MalformedObjectNameException
    {
        Iterator iter = MuleManager.getInstance().getModel().getComponentNames();
        String name;
        while (iter.hasNext())
        {
            name = iter.next().toString();
            ObjectName on = new ObjectName("Mule:type=control,name=" + name + "ComponentService");
            ComponentServiceMBean serviceMBean = new ComponentService(name);
            mBeanServer.registerMBean(serviceMBean, on);
            registeredMBeans.add(on);
        }

    }


    /**
     * @return Returns the createServer.
     */
    public boolean isCreateServer()
    {
        return createServer;
    }

    /**
     * @param createServer The createServer to set.
     */
    public void setCreateServer(boolean createServer)
    {
        this.createServer = createServer;
    }

    /**
     * @return Returns the locateServer.
     */
    public boolean isLocateServer()
    {
        return locateServer;
    }

    /**
     * @param locateServer The locateServer to set.
     */
    public void setLocateServer(boolean locateServer)
    {
        this.locateServer = locateServer;
    }

    /**
     * @return Returns the connectorServerUrl.
     */
    public String getConnectorServerUrl()
    {
        return connectorServerUrl;
    }

    /**
     * @param connectorServerUrl The connectorServerUrl to set.
     */
    public void setConnectorServerUrl(String connectorServerUrl)
    {
        this.connectorServerUrl = connectorServerUrl;
    }

    /**
     * @return Returns the enableStatistics.
     */
    public boolean isEnableStatistics()
    {
        return enableStatistics;
    }

    /**
     * @param enableStatistics The enableStatistics to set.
     */
    public void setEnableStatistics(boolean enableStatistics)
    {
        this.enableStatistics = enableStatistics;
    }

    /**
     * @return Returns the mBeanServer.
     */
    public MBeanServer getMBeanServer()
    {
        return mBeanServer;
    }

    /**
     * @param mBeanServer The mBeanServer to set.
     */
    public void setMBeanServer(MBeanServer mBeanServer)
    {
        this.mBeanServer = mBeanServer;
    }
}
