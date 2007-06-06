/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.mock;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.axis.AxisEngine;
import org.apache.axis.ConfigurationException;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.Handler;
import org.apache.axis.encoding.TypeMappingRegistry;
import org.apache.axis.handlers.soap.SOAPService;

public class MockEngineConfiguration extends Object implements EngineConfiguration
{
    public void configureEngine(AxisEngine engine) throws ConfigurationException
    {
        // don't do it
    }

    public Iterator getDeployedServices() throws ConfigurationException
    {
        return null;
    }

    public Hashtable getGlobalOptions() throws ConfigurationException
    {
        return null;
    }

    public Handler getGlobalRequest() throws ConfigurationException
    {
        return null;
    }

    public Handler getGlobalResponse() throws ConfigurationException
    {
        return null;
    }

    public Handler getHandler(QName qname) throws ConfigurationException
    {
        return null;
    }

    public List getRoles()
    {
        return null;
    }

    public SOAPService getService(QName qname) throws ConfigurationException
    {
        return null;
    }

    public SOAPService getServiceByNamespaceURI(String namespace) throws ConfigurationException
    {
        return null;
    }

    public Handler getTransport(QName qname) throws ConfigurationException
    {
        return null;
    }

    public TypeMappingRegistry getTypeMappingRegistry() throws ConfigurationException
    {
        return null;
    }

    public void writeEngineConfig(AxisEngine engine) throws ConfigurationException
    {
        // does nothing
    }
}


