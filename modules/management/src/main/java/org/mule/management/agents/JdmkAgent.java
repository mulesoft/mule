/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.agents;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.AbstractAgent;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.net.URI;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * <code>JdmkAgent</code> configures an Jdmk Http Adaptor for Jmx management,
 * statistics and configuration viewing of a Mule instance.
* <p/>
 * TODO MULE-1353 
 */
public class JdmkAgent extends AbstractAgent
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

    private MBeanServer mBeanServer;
    private ObjectName adaptorName;


    public JdmkAgent()
    {
        super("JDMK Agent");
    }

    protected Object createAdaptor() throws Exception
    {
        final URI uri = new URI(jmxAdaptorUrl);
        final int port = uri.getPort();
        return ClassUtils.instanciateClass(CLASSNAME_ADAPTER,
                                           new Object[] {new Integer(port)}, this.getClass());
    }

    public String getDescription()
    {
        return "Jdmk Http adaptor: " + jmxAdaptorUrl;
    }

    public void start() throws UMOException
    {
        try
        {
            mBeanServer.invoke(adaptorName, "start", null, null);
        }
        catch (InstanceNotFoundException e)
        {
            throw new JmxManagementException(
                CoreMessages.failedToStart("Jdmk agent"), adaptorName, e);
        }
        catch (MBeanException e)
        {
            throw new JmxManagementException(
                CoreMessages.failedToStart("Jdmk agent"), adaptorName, e);
        }
        catch (ReflectionException e)
        {
            // ignore
        }
    }

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
            throw new JmxManagementException(
                CoreMessages.failedToStop("Jdmk agent"), adaptorName, e);
        }
        catch (MBeanException e)
        {
            throw new JmxManagementException(
                CoreMessages.failedToStop("Jdmk agent"), adaptorName, e);
        }
        catch (ReflectionException e)
        {
            // ignore
        }
    }

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

    public void registered()
    {
        // nothing to do
    }

    public void unregistered()
    {
        // nothing to do
    }

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
            throw new InitialisationException(CoreMessages.failedToStart("Jdmk Agent"), e, this);
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
