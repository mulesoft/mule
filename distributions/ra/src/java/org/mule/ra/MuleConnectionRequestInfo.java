/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.ra;

import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.umo.manager.UMOManager;
import org.mule.util.StringUtils;

import javax.resource.spi.ConnectionRequestInfo;

import java.io.Serializable;

/**
 * <code>MuleConnectionRequestInfo</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleConnectionRequestInfo implements ConnectionRequestInfo, Cloneable, Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -122686675425629280L;

    private String configurationBuilder = MuleXmlConfigurationBuilder.class.getName();
    private String configurations;
    private String username;
    private String password;
    private UMOManager manager;

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
        return StringUtils.split(configurations, ",");
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

    public UMOManager getManager()
    {
        return manager;
    }

    public void setManager(UMOManager manager)
    {
        this.manager = manager;
    }

    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MuleConnectionRequestInfo)) {
            return false;
        }

        final MuleConnectionRequestInfo muleConnectionRequestInfo = (MuleConnectionRequestInfo) o;

        if (configurationBuilder != null ? !configurationBuilder.equals(muleConnectionRequestInfo.configurationBuilder)
                : muleConnectionRequestInfo.configurationBuilder != null) {
            return false;
        }
        if (configurations != null ? !configurations.equals(muleConnectionRequestInfo.configurations)
                : muleConnectionRequestInfo.configurations != null) {
            return false;
        }
        if (password != null ? !password.equals(muleConnectionRequestInfo.password)
                : muleConnectionRequestInfo.password != null) {
            return false;
        }
        if (username != null ? !username.equals(muleConnectionRequestInfo.username)
                : muleConnectionRequestInfo.username != null) {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result;
        result = (configurationBuilder != null ? configurationBuilder.hashCode() : 0);
        result = 29 * result + (configurations != null ? configurations.hashCode() : 0);
        result = 29 * result + (username != null ? username.hashCode() : 0);
        result = 29 * result + (password != null ? password.hashCode() : 0);
        return result;
    }

    public Object clone()
    {
        MuleConnectionRequestInfo clone = new MuleConnectionRequestInfo();
        clone.setConfigurationBuilder(getConfigurationBuilder());
        clone.setConfigurations(getConfigurations());
        clone.setManager(getManager());
        clone.setPassword(getPassword());
        clone.setUserName(getUserName());
        return clone;
    }
}
