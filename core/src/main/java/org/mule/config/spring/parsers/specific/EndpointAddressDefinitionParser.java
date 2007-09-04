/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.impl.endpoint.MuleEndpointURI;

import java.util.Properties;
import java.net.URI;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EndpointAddressDefinitionParser extends ChildDefinitionParser
{

    private String protocol;

    public EndpointAddressDefinitionParser(String protocol)
    {
        super("endpointURI", EndpointAddress.class);
        this.protocol = protocol;
    }

    // @Override
    protected void postProcess(BeanDefinitionBuilder builder, Element element)
    {
        super.postProcess(builder, element);
        getBeanAssembler(element, builder).extendBean("protocol", protocol, false);
    }


    /**
     * This isn't efficient, but is only used during startup.  It has the advantage that it separates
     * the simple string-based config here from the bloated mess that is UMOEndpointURI.  Presumably
     * at some point that will be simplified and the code here should need minimal changes.
     */
    public static class EndpointAddress implements UMOEndpointURI
    {

        private Log logger = LogFactory.getLog(getClass());

        private String protocol;
        private String username;
        private String password;
        private String hostname;
        private Integer port;
        private String path;

        public void setUsername(String username)
        {
            this.username = username;
        }

        public void setPassword(String password)
        {
            this.password = password;
        }

        public void setHostname(String hostname)
        {
            this.hostname = hostname;
        }

        public void setPort(int port)
        {
            this.port = new Integer(port);
        }

        public void setProtocol(String protocol)
        {
            this.protocol = protocol;
        }

        public void setPath(String path)
        {
            this.path = path;
        }

        public String toString()
        {
            StringBuffer buffer = new StringBuffer();
            buffer.append(protocol);
            buffer.append("://");
            if (null != username)
            {
                buffer.append(username);
                if (null != password)
                {
                    buffer.append(":");
                    buffer.append(password);
                }
                buffer.append("@");
            }
            buffer.append(hostname);
            if (null != port)
            {
                buffer.append(":");
                buffer.append(port);
            }
            if (null != path)
            {
                buffer.append("/");
                buffer.append(path);
            }
            return buffer.toString();
        }

        protected UMOEndpointURI delegate()
        {
            try
            {
                return new MuleEndpointURI(toString());
            }
            catch (EndpointException e)
            {
                throw (IllegalStateException) new IllegalStateException("Bad address").initCause(e);
            }
        }


        public String getAddress()
        {
            return delegate().getAddress();
        }

        public String getFilterAddress()
        {
            return delegate().getFilterAddress();
        }

        public String getEndpointName()
        {
            return delegate().getEndpointName();
        }

        public void setEndpointName(String name)
        {
            throw new UnsupportedOperationException("EndpointAddress.setEdpointName");
        }

        public Properties getParams()
        {
            return delegate().getParams();
        }

        public Properties getUserParams()
        {
            return delegate().getUserParams();
        }

        public String getScheme()
        {
            return delegate().getScheme();
        }

        public String getSchemeMetaInfo()
        {
            return delegate().getSchemeMetaInfo();
        }

        public String getFullScheme()
        {
            return delegate().getFullScheme();
        }

        public String getAuthority()
        {
            return delegate().getAuthority();
        }

        public String getHost()
        {
            return delegate().getHost();
        }

        public int getPort()
        {
            return delegate().getPort();
        }

        public String getPath()
        {
            return delegate().getPath();
        }

        public String getQuery()
        {
            return delegate().getQuery();
        }

        public String getUserInfo()
        {
            return delegate().getUserInfo();
        }

        public String getTransformers()
        {
            return delegate().getTransformers();
        }

        public String getResponseTransformers()
        {
            return delegate().getResponseTransformers();
        }

        public int getCreateConnector()
        {
            return delegate().getCreateConnector();
        }

        public URI getUri()
        {
            return delegate().getUri();
        }

        public String getConnectorName()
        {
            return delegate().getConnectorName();
        }

        public String getResourceInfo()
        {
            return delegate().getResourceInfo();
        }

        public String getUsername()
        {
            return delegate().getUsername();
        }

        public String getPassword()
        {
            return delegate().getPassword();
        }

        public void initialise() throws InitialisationException
        {
            // do nothing
        }

    }

}
