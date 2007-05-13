/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.impl.ManagementContextAware;
import org.mule.umo.UMOManagementContext;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import javax.resource.spi.ConnectionRequestInfo;

/**
 * <code>MuleConnectionRequestInfo</code> TODO
 */
public class MuleConnectionRequestInfo implements ConnectionRequestInfo, Cloneable, Serializable, ManagementContextAware
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 910828075890304726L;


    private String configurationBuilder = MuleXmlConfigurationBuilder.class.getName();
    private String configurations;
    private String username;
    private String password;

    private UMOManagementContext managementContext;

    public MuleConnectionRequestInfo()
    {
        super();
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException
    {
        ois.defaultReadObject();
    }

    public String getConfigurationBuilder()
    {
        return configurationBuilder;
    }

    public void setConfigurationBuilder(String configurationBuilder)
    {
        this.configurationBuilder = configurationBuilder;
    }

    public String getConfigurations()
    {
        return configurations;
    }

    public String[] getConfigurationsAsArray()
    {
        return StringUtils.splitAndTrim(configurations, ",");
    }

    public void setConfigurations(String configurations)
    {
        this.configurations = configurations;
    }

    public String getUserName()
    {
        return username;
    }

    public void setUserName(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public UMOManagementContext getManagementContext()
    {
        return managementContext;
    }

    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (this.getClass() != obj.getClass())
        {
            return false;
        }

        final MuleConnectionRequestInfo muleConnectionRequestInfo = (MuleConnectionRequestInfo)obj;

        if (configurationBuilder != null
                        ? !configurationBuilder.equals(muleConnectionRequestInfo.configurationBuilder)
                        : muleConnectionRequestInfo.configurationBuilder != null)
        {
            return false;
        }

        if (configurations != null
                        ? !configurations.equals(muleConnectionRequestInfo.configurations)
                        : muleConnectionRequestInfo.configurations != null)
        {
            return false;
        }

        if (password != null
                        ? !password.equals(muleConnectionRequestInfo.password)
                        : muleConnectionRequestInfo.password != null)
        {
            return false;
        }

        if (username != null
                        ? !username.equals(muleConnectionRequestInfo.username)
                        : muleConnectionRequestInfo.username != null)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result = (configurationBuilder != null ? configurationBuilder.hashCode() : 0);
        result = 29 * result + (configurations != null ? configurations.hashCode() : 0);
        result = 29 * result + (username != null ? username.hashCode() : 0);
        return 29 * result + (password != null ? password.hashCode() : 0);
    }

    protected Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
