/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
                return new QName(getServiceNamespace(), ((Port) service.getPorts().values().iterator().next()).getName());
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
}
