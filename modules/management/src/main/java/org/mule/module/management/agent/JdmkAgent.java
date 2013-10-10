/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.agent;

import org.mule.AbstractAgent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
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
        super("jdmk-agent");
    }

    protected Object createAdaptor() throws Exception
    {
        final URI uri = new URI(jmxAdaptorUrl);
        final int port = uri.getPort();
        return ClassUtils.instanciateClass(CLASSNAME_ADAPTER,
                                           new Object[] {new Integer(port)}, this.getClass());
    }

    @Override
    public String getDescription()
    {
        return "Jdmk Http adaptor: " + jmxAdaptorUrl;
    }

    @Override
    public void start() throws MuleException
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

    @Override
    public void stop() throws MuleException
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

    @Override
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

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
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
