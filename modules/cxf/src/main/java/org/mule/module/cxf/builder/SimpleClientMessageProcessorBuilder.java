/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.builder;

import org.mule.api.lifecycle.CreateException;
import org.mule.module.cxf.support.CxfUtils;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientFactoryBean;

public class SimpleClientMessageProcessorBuilder extends AbstractClientMessageProcessorBuilder
{
    @Override
    protected Client createClient() throws CreateException, Exception
    {
        ClientFactoryBean cpf = new ClientFactoryBean();
        cpf.setServiceClass(serviceClass);
        if (databinding == null) 
        {
            cpf.setDataBinding(new AegisDatabinding());
        }
        else 
        {
            cpf.setDataBinding(databinding);
        }
        cpf.setAddress(getAddress());
        cpf.setBus(getBus());
        cpf.setProperties(properties);

        if (wsdlLocation != null)
        {
            cpf.setWsdlURL(wsdlLocation);
        }

        // If there's a soapVersion defined then the corresponding bindingId will be set
        if(soapVersion != null)
        {
            cpf.setBindingId(CxfUtils.getBindingIdForSoapVersion(soapVersion));
        }

        return cpf.create();
    }
}
