/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis.mock;

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

    public Iterator<?> getDeployedServices() throws ConfigurationException
    {
        return null;
    }

    public Hashtable<?, ?> getGlobalOptions() throws ConfigurationException
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

    public List<?> getRoles()
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


