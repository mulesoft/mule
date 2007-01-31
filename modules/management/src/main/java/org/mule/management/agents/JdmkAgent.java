/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.agents;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.net.URI;

/**
 * <code>JdmkAgent</code> configures an Jdmk Http Adaptor for Jmx management,
 * statistics and configuration viewing of a Mule instance.
* <p/>
 * TODO MULE-1353 
 */
public class JdmkAgent implements UMOAgent
{
    /** A FQN of the adaptor class to instantiate via reflection. */
    public static final String CLASSNAME_ADAPTER = "com.sun.jdmk.comm.HtmlAdaptorServer";

    private static final String PROTOCOL_PREFIX = "http://";
    public static final String DEFAULT_HOSTNAME = "localhost";
    public static final int DEFAULT_PORT = 9092;
    public static final String DEFAULT_JMX_ADAPTOR_URL = PROTOCOL_PREFIX + DEFAULT_HOSTNAME + ":" + DEFAULT_PORT;

    private String jmxAdaptorUrl;
    private String host;
    private String port;

    private String name = "JDMK Agent";
    private MBeanServer mBeanServer;
    private ObjectName adaptorName;

    protected Object createAdaptor() throws Exception
    {
        final URI uri = new URI(jmxAdaptorUrl);
        final int port = uri.getPort();
        return ClassUtils.instanciateClass(CLASSNAME_ADAPTER,
                                           new Object[] {new Integer(port)}, this.getClass());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.manager.UMOAgent#getName()
     */
    public String getName()
    {
        return this.name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.manager.UMOAgent#setName(java.lang.String)
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.manager.UMOAgent#getDescription()
     */
    public String getDescription()
    {
        return "Jdmk Http adaptor: " + jmxAdaptorUrl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Startable#start()
     */
    public void start() throws UMOException
    {
        try
        {
            mBeanServer.invoke(adaptorName, "start", null, null);
        }
        catch (InstanceNotFoundException e)
        {
            throw new JmxManagementException(new Message(Messages.FAILED_TO_START_X, "Jdmk agent"),
                adaptorName, e);
        }
        catch (MBeanException e)
        {
            throw new JmxManagementException(new Message(Messages.FAILED_TO_START_X, "Jdmk agent"),
                adaptorName, e);
        }
        catch (ReflectionException e)
        {
            // ignore
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Stoppable#stop()
     */
    public void stop() throws UMOException
    {
        if (mBeanServer == null)
        {
            return;
        }
        
        try
        {
            mBeanServer.invoke(adaptorName, "stop", null, null);
        }
        catch (InstanceNotFoundException e)
        {
            throw new JmxManagementException(new Message(Messages.FAILED_TO_STOP_X, "Jdmk agent"),
                adaptorName, e);
        }
        catch (MBeanException e)
        {
            throw new JmxManagementException(new Message(Messages.FAILED_TO_STOP_X, "Jdmk agent"),
                adaptorName, e);
        }
        catch (ReflectionException e)
        {
            // ignore
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Disposable#dispose()
     */
    public void dispose()
    {
        try
        {
            stop();
        }
        catch (Exception e)
        {
            // TODO: log an exception
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.manager.UMOAgent#registered()
     */
    public void registered()
    {
        // nothing to do
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.manager.UMOAgent#unregistered()
     */
    public void unregistered()
    {
        // nothing to do
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Initialisable#initialise()
     */
    public void initialise() throws InitialisationException
    {
        try
        {
            mBeanServer = (MBeanServer)MBeanServerFactory.findMBeanServer(null).get(0);
            final Object adaptor = createAdaptor();
            if (StringUtils.isBlank(jmxAdaptorUrl))
            {
                if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(port))
                {
                    jmxAdaptorUrl = PROTOCOL_PREFIX + host + ":" + port;
                }
                else
                {
                    jmxAdaptorUrl = DEFAULT_JMX_ADAPTOR_URL;
                }
            }
            // TODO use Jmx support classes
            adaptorName = new ObjectName("Adaptor:class=" + adaptor.getClass().getName());
            mBeanServer.registerMBean(adaptor, adaptorName);
        }
        catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.FAILED_TO_START_X, "Jdmk Agent"), e, this);
        }
    }

    /**
     * @return Returns the jmxAdaptorUrl.
     */
    public String getJmxAdaptorUrl()
    {
        return jmxAdaptorUrl;
    }

    /**
     * @param jmxAdaptorUrl The jmxAdaptorUrl to set.
     */
    public void setJmxAdaptorUrl(String jmxAdaptorUrl)
    {
        this.jmxAdaptorUrl = jmxAdaptorUrl;
    }


    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public String getPort()
    {
        return port;
    }

    public void setPort(String port)
    {
        this.port = port;
    }
}
