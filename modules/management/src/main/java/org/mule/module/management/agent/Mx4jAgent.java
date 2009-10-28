/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.management.agent;

import org.mule.AbstractAgent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.management.mbean.MBeanServerFactory;
import org.mule.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.module.management.support.JmxSupport;
import org.mule.module.management.support.JmxSupportFactory;
import org.mule.module.xml.util.XMLUtils;
import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import mx4j.log.CommonsLogger;
import mx4j.log.Log;
import mx4j.tools.adaptor.http.HttpAdaptor;
import mx4j.tools.adaptor.http.XSLTProcessor;
import mx4j.tools.adaptor.ssl.SSLAdaptorServerSocketFactory;
import mx4j.tools.adaptor.ssl.SSLAdaptorServerSocketFactoryMBean;

import org.apache.commons.logging.LogFactory;

/**
 * <code>Mx4jAgent</code> configures an Mx4J Http Adaptor for Jmx management,
 * statistics and configuration viewing of a Mule instance.
 */
public class Mx4jAgent extends AbstractAgent
{
    public static final String HTTP_ADAPTER_OBJECT_NAME = "name=Mx4jHttpAdapter";

    protected static final String DEFAULT_PATH_IN_JAR = 
        StringUtils.replaceChars(ClassUtils.getPackageName(Mx4jAgent.class), '.', '/') + "/http/xsl";

    private static final org.apache.commons.logging.Log logger = LogFactory.getLog(Mx4jAgent.class);

    private static final String PROTOCOL_PREFIX = "http://";
    public static final String DEFAULT_HOSTNAME = "localhost";
    public static final int DEFAULT_PORT = 9999;
    public static final String DEFAULT_JMX_ADAPTOR_URL = PROTOCOL_PREFIX + DEFAULT_HOSTNAME + ":" + DEFAULT_PORT;

    private String jmxAdaptorUrl;
    private String host;
    private String port;

    private HttpAdaptor adaptor;
    private MBeanServer mBeanServer;
    private ObjectName adaptorName;

    // Adaptor overrides
    private String login;

    private String password;

    private String authenticationMethod = "basic";

    // TODO AH check how an embedded scenario can be handled (no mule home) 
    private String xslFilePath = System.getProperty("mule.home") + "/lib/mule/mule-module-management-" +
            MuleManifest.getProductVersion() + ".jar";

    private String pathInJar = DEFAULT_PATH_IN_JAR;

    private boolean cacheXsl = true;

    // SSL/TLS socket factory config
    private Map socketFactoryProperties = new HashMap();

    private JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    private JmxSupport jmxSupport;


    public Mx4jAgent()
    {
        super("jmx-mx4j-adaptor");
    }

    protected HttpAdaptor createAdaptor() throws Exception
    {
        Log.redirectTo(new CommonsLogger());
        URI uri = new URI(StringUtils.stripToEmpty(jmxAdaptorUrl));
        adaptor = new HttpAdaptor(uri.getPort(), uri.getHost());

        // Set the XSLT Processor with any local overrides
        XSLTProcessor processor;
        try
        {
            processor = new XSLTProcessor();
        }
        catch (TransformerFactoryConfigurationError e)
        {
            System.setProperty("javax.xml.transform.TransformerFactory", XMLUtils.TRANSFORMER_FACTORY_JDK5);
            processor = new XSLTProcessor();
        }

        if (StringUtils.isNotBlank(xslFilePath))
        {
            processor.setFile(xslFilePath.trim());
        }

        if (StringUtils.isNotBlank(pathInJar))
        {
            processor.setPathInJar(pathInJar.trim());
        }

        processor.setUseCache(cacheXsl);

        adaptor.setProcessor(processor);

        // Set endpoint authentication if required
        if (login != null)
        {
            adaptor.addAuthorization(login, password);
            adaptor.setAuthenticationMethod(authenticationMethod);
        }

        if (socketFactoryProperties != null && !socketFactoryProperties.isEmpty())
        {
            SSLAdaptorServerSocketFactoryMBean factory;
            if (SystemUtils.isIbmJDK())
            {
                factory = new IBMSslAdapterServerSocketFactory();
            }
            else
            {
                // BEA are using Sun's JSSE, so no extra checks necessary
                factory = new SSLAdaptorServerSocketFactory();
            }
            BeanUtils.populateWithoutFail(factory, socketFactoryProperties, true);
            adaptor.setSocketFactory(factory);
        }

        return adaptor;
    }

    public void initialise() throws InitialisationException
    {
        try
        {
            jmxSupport = jmxSupportFactory.getJmxSupport();
            mBeanServer = MBeanServerFactory.getOrCreateMBeanServer();

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

            adaptor = createAdaptor();
            adaptorName = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":" + HTTP_ADAPTER_OBJECT_NAME);

            unregisterMBeansIfNecessary();
            mBeanServer.registerMBean(adaptor, adaptorName);
        }
        catch (Exception e)
        {
            throw new InitialisationException(CoreMessages.failedToStart("mx4j agent"), e, this);
        }
    }

    public void start() throws MuleException
    {
        if (mBeanServer == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("mBeanServer has not yet been created"), this);
        }
        
        try
        {
            mBeanServer.invoke(adaptorName, "start", null, null);
        }
        catch (InstanceNotFoundException e)
        {
            throw new JmxManagementException(
                CoreMessages.failedToStart("Mx4j agent"), adaptorName, e);
        }
        catch (MBeanException e)
        {
            throw new JmxManagementException(
                CoreMessages.failedToStart("Mx4j agent"), adaptorName, e);
        }
        catch (ReflectionException e)
        {
            // ignore
        }
    }

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
                CoreMessages.failedToStop("Mx4j agent"), adaptorName, e);
        }
        catch (MBeanException e)
        {
            throw new JmxManagementException(
                CoreMessages.failedToStop("Mx4j agent"), adaptorName, e);
        }
        catch (ReflectionException e)
        {
            // ignore
        }
    }

    /**
     * Unregister all Mx4j MBeans if there are any left over the old deployment
     */
    protected void unregisterMBeansIfNecessary()
        throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException
    {
        if (mBeanServer != null && mBeanServer.isRegistered(adaptorName))
        {
            mBeanServer.unregisterMBean(adaptorName);
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
            logger.warn("Failed to stop Mx4jAgent: " + e.getMessage());
        }
        finally
        {
            try
            {
                unregisterMBeansIfNecessary();
            }
            catch (Exception e)
            {
                logger.error("Couldn't unregister MBean: "
                             + (adaptorName != null ? adaptorName.getCanonicalName() : "null"), e);
            }
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

    // /////////////////////////////////////////////////////////////////////////
    // Getters and setters
    // /////////////////////////////////////////////////////////////////////////
    @Override
    public String getDescription()
    {
        return "MX4J Http adaptor: " + jmxAdaptorUrl;
    }

    public String getJmxAdaptorUrl()
    {
        return jmxAdaptorUrl;
    }

    public void setJmxAdaptorUrl(String jmxAdaptorUrl)
    {
        this.jmxAdaptorUrl = jmxAdaptorUrl;
    }

    public Map getSocketFactoryProperties()
    {
        return socketFactoryProperties;
    }

    public void setSocketFactoryProperties(Map socketFactoryProperties)
    {
        this.socketFactoryProperties = socketFactoryProperties;
    }

    public String getLogin()
    {
        return login;
    }

    public void setLogin(String login)
    {
        this.login = login;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getAuthenticationMethod()
    {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(String authenticationMethod)
    {
        this.authenticationMethod = authenticationMethod;
    }

    public String getXslFilePath()
    {
        return xslFilePath;
    }

    public void setXslFilePath(String xslFilePath)
    {
        this.xslFilePath = xslFilePath;
    }

    public String getPathInJar()
    {
        return pathInJar;
    }

    public void setPathInJar(String pathInJar)
    {
        this.pathInJar = pathInJar;
    }

    public boolean isCacheXsl()
    {
        return cacheXsl;
    }

    public void setCacheXsl(boolean cacheXsl)
    {
        this.cacheXsl = cacheXsl;
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
