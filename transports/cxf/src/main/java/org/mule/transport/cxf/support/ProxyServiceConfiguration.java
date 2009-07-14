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

import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;

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
     * 
     * @Override
     */
    public QName getEndpointName()
    {
        try
        {
            if (getServiceFactory().getWsdlURL() != null)
            {
                Definition definition = getServiceFactory().getBus()
                    .getExtension(WSDLManager.class)
                    .getDefinition(getServiceFactory().getWsdlURL());
                return new QName(getServiceNamespace(), ((Port) definition.getService(
                    getServiceFactory().getServiceQName()).getPorts().values().iterator().next()).getName());
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
}
