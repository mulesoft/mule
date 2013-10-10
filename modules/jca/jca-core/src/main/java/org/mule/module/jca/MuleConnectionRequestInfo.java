/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jca;

import java.io.Serializable;

import javax.resource.spi.ConnectionRequestInfo;

/**
 * <code>MuleConnectionRequestInfo</code> TODO
 */
public class MuleConnectionRequestInfo implements ConnectionRequestInfo, Cloneable, Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 910828075890304726L;

    private String username;
    private String password;

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

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MuleConnectionRequestInfo other = (MuleConnectionRequestInfo) obj;
        if (password == null)
        {
            if (other.password != null) return false;
        }
        else if (!password.equals(other.password)) return false;
        if (username == null)
        {
            if (other.username != null) return false;
        }
        else if (!username.equals(other.username)) return false;
        return true;
    }

    protected Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
