package org.mule.module.cxf.builder;

import org.mule.api.lifecycle.CreateException;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;

public class SimpleClientMessageProcessorBuilder extends AbstractClientMessageProcessorBuilder
{
    @Override
    protected Client createClient() throws CreateException, Exception
    {
        ClientProxyFactoryBean cpf = new ClientProxyFactoryBean();
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
            cpf.setWsdlLocation(wsdlLocation);
        }

        return ClientProxy.getClient(cpf.create());
    }
}
