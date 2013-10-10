/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.mbean;

import org.mule.api.config.MuleConfiguration;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.util.StringUtils;

/**
 * <code>MuleConfigurationService</code> exposes the MuleConfiguration settings as
 * a management service.
 * 
 */
public class MuleConfigurationService implements MuleConfigurationServiceMBean
{
    private  MuleConfiguration muleConfiguration;

    public MuleConfigurationService(MuleConfiguration muleConfiguration)
    {
        this.muleConfiguration = muleConfiguration;
    }

    public int getSynchronousEventTimeout()
    {
        return muleConfiguration.getDefaultResponseTimeout();
    }

    public String getWorkingDirectory()
    {
        return muleConfiguration.getWorkingDirectory();
    }

    public int getTransactionTimeout()
    {
        return muleConfiguration.getDefaultTransactionTimeout();
    }

    public int getShutdownTimeout()
    {
        return muleConfiguration.getShutdownTimeout();
    }

    public boolean isClientMode()
    {
        return muleConfiguration.isClientMode();
    }


    public String getEncoding()
    {
        return muleConfiguration.getDefaultEncoding();
    }

    public boolean isContainerMode()
    {
        return muleConfiguration.isContainerMode();
    }

    public boolean isFullStackTraces()
    {
        /*
            Sacrifice the code quality for the sake of keeping things simple -
            the alternative would be to pass MuleContext into every exception constructor.
         */
        return DefaultMuleConfiguration.fullStackTraces;
    }

    public void setFullStackTraces(boolean flag)
    {
        /*
           Sacrifice the code quality for the sake of keeping things simple -
           the alternative would be to pass MuleContext into every exception constructor.
        */
        DefaultMuleConfiguration.fullStackTraces = flag;
    }

    public String getStackTraceFilter()
    {
        return StringUtils.join(DefaultMuleConfiguration.stackTraceFilter, ',');
    }

    public void setStackTraceFilter(String filterAsString)
    {
        DefaultMuleConfiguration.stackTraceFilter = filterAsString.split(",");
    }
}
