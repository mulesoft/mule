/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

public class ProxyServiceFactoryBean extends ReflectionServiceFactoryBean
{

    private static final Logger LOG = LogUtils.getLogger(ProxyServiceFactoryBean.class);

    public ProxyServiceFactoryBean()
    {
        getServiceConfigurations().add(0, new ProxyServiceConfiguration());

        List<String> ignoredClasses = new ArrayList<String>();
        ignoredClasses.add("java.lang.Object");
        ignoredClasses.add("java.lang.Throwable");
        ignoredClasses.add("org.omg.CORBA_2_3.portable.ObjectImpl");
        ignoredClasses.add("org.omg.CORBA.portable.ObjectImpl");
        ignoredClasses.add("javax.ejb.EJBObject");
        ignoredClasses.add("javax.rmi.CORBA.Stub");
        setIgnoredClasses(ignoredClasses);
    }
    
    @Override
    protected void initializeWSDLOperations()
    {
        if (getServiceClass().isAssignableFrom(ProxyService.class))
        {
            initializeWSDLOperationsForProvider();
        }
        else
        {
            super.initializeWSDLOperations();
        }
    }

    protected void initializeWSDLOperationsForProvider()
    {
        Class c = Source.class;

        if (getEndpointInfo() == null && isFromWsdl())
        {
            // most likely, they specified a WSDL, but for some reason
            // did not give a valid ServiceName/PortName. For provider,
            // we'll allow this since everything is bound directly to
            // the invoke method, however, this CAN cause other problems
            // such as addresses in the wsdl not getting updated and such
            // so we'll WARN about it.....
            List<QName> enames = new ArrayList<QName>();
            for (ServiceInfo si : getService().getServiceInfos())
            {
                for (EndpointInfo ep : si.getEndpoints())
                {
                    enames.add(ep.getName());
                }
            }
            LOG.log(Level.WARNING, "COULD_NOT_FIND_ENDPOINT", new Object[]{getEndpointName(), enames});
        }

        try
        {
            Method invoke = getServiceClass().getMethod("invoke", c);

            // Bind every operation to the invoke method.
            for (ServiceInfo si : getService().getServiceInfos())
            {
                for (OperationInfo o : si.getInterface().getOperations())
                {
                    getMethodDispatcher().bind(o, invoke);
                }
            }
        }
        catch (SecurityException e)
        {
            throw new ServiceConstructionException(e);
        }
        catch (NoSuchMethodException e)
        {
            throw new ServiceConstructionException(e);
        }

    }

}
