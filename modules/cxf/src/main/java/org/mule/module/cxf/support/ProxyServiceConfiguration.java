/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import org.mule.module.cxf.i18n.CxfMessages;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.xml.namespace.QName;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.factory.DefaultServiceConfiguration;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.wsdl.WSDLManager;

public class ProxyServiceConfiguration extends DefaultServiceConfiguration
{

    private static final Logger LOG = LogUtils.getLogger(ProxyServiceFactoryBean.class);

    private String soapVersion;

    /**
     * Override to use port name from service definition in WSDL when we are doing
     * WSDL-first. This is required so that CXF's internal endpointName and port name
     * match and a CXF Service gets created. See:
     * https://issues.apache.org/jira/browse/CXF-1920
     * http://fisheye6.atlassian.com/changelog/cxf?cs=737994
     */
    @Override
    public QName getEndpointName()
    {
        try
        {
            if (getServiceFactory().getWsdlURL() != null)
            {
                Definition definition = getServiceFactory().getBus()
                    .getExtension(WSDLManager.class)
                    .getDefinition(getServiceFactory().getWsdlURL());
                Service service = getServiceFromDefinition(definition);
                setServiceNamespace(service.getQName().getNamespaceURI());

                // Try to find a port that matches the SOAP version specified in the proxy service (if available).
                QName endpointName = getPortMatchingSoapVersion(service);
                if (endpointName == null)
                {
                    // Fallback to default behaviour
                    endpointName = new QName(getServiceNamespace(), ((Port) service.getPorts().values().iterator().next()).getName());
                }

                LOG.fine(String.format("ProxyServiceConfiguration using endpoint %s", endpointName));
                return endpointName;
            }
            else
            {
                return super.getEndpointName();
            }

        }
        catch (WSDLException e)
        {
            throw new ServiceConstructionException(new Message("SERVICE_CREATION_MSG", LOG), e);
        }
    }

    private QName getPortMatchingSoapVersion(Service service)
    {
        if (soapVersion == null)
        {
            // No SOAP version specified in the config.
            return null;
        }

        for (Port port : (Iterable<Port>) service.getPorts().values())
        {
            for (Object element : port.getBinding().getExtensibilityElements())
            {
                if ((element instanceof SOAPBinding && "1.1".equals(soapVersion)) ||
                    (element instanceof SOAP12Binding && "1.2".equals(soapVersion)))
                {
                    return new QName(getServiceNamespace(), port.getName());
                }
            }
        }

        // No port matching the specified SOAP version.
        return null;
    }

    protected Service getServiceFromDefinition(Definition definition)
    {
        Service service = definition.getService(getServiceFactory().getServiceQName());
        if (service == null)
        {
            List<QName> probableServices = getProbableServices(definition);
            List<QName> allServices = getAllServices(definition);
            throw new ComponentNotFoundRuntimeException(CxfMessages.invalidOrMissingNamespace(
                getServiceFactory().getServiceQName(), probableServices, allServices));
        }
        return service;
    }

    /**
     * This method returns a list of all the services defined in the definition. Its
     * current purpose is only for generating a better error message when the service
     * cannot be found.
     */
    @SuppressWarnings("unchecked")
    protected List<QName> getAllServices(Definition definition)
    {
        return new LinkedList<QName>(CollectionUtils.select(definition.getServices().keySet(),
            new Predicate()
            {
                public boolean evaluate(Object object)
                {
                    return object instanceof QName;
                }
            }));
    }

    /**
     * This method returns the list of services that matches with the local part of
     * the service QName. Its current purpose is only for generating a better error
     * message when the service cannot be found.
     */
    protected List<QName> getProbableServices(Definition definition)
    {
        QName serviceQName = getServiceFactory().getServiceQName();
        List<QName> probableServices = new LinkedList<QName>();
        Map<?, ?> services = definition.getServices();
        for (Iterator<?> iterator = services.keySet().iterator(); iterator.hasNext();)
        {
            Object key = iterator.next();
            if (key instanceof QName)
            {
                QName qNameKey = (QName) key;
                if (qNameKey.getLocalPart() != null
                    && qNameKey.getLocalPart().equals(serviceQName.getLocalPart()))
                {
                    probableServices.add(qNameKey);
                }
            }
        }
        return probableServices;
    }

    public void setSoapVersion(String soapVersion)
    {
        this.soapVersion = soapVersion;
    }
}
