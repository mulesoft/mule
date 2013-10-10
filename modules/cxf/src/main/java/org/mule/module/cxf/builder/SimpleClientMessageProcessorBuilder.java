/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
