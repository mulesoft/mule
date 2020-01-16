/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.support;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.cxf.binding.soap.SoapBindingConstants.SOAP_ACTION;

import javax.xml.namespace.QName;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapBody;
import org.apache.cxf.databinding.stax.StaxDataBindingInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.URIMappingInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * Sets the correct operation when the binding style is RPC. The processing of the message content is delegated to the
 * StaxDataBindingInterceptor.
 *
 */
public class ProxyAddBindingOperationInfoInterceptor extends AbstractInDatabindingInterceptor
{
    public ProxyAddBindingOperationInfoInterceptor()
    {
        super(Phase.UNMARSHAL);
        addAfter(URIMappingInterceptor.class.getName());
        addBefore(StaxDataBindingInterceptor.class.getName());
    }

    private BindingOperationInfo getOperation(Message message, QName opName)
    {
        BindingOperationInfo bop = ServiceModelUtil.getOperation(message.getExchange(), opName);
        if (bop == null)
        {
            Endpoint ep = message.getExchange().get(Endpoint.class);
            if (ep == null)
            {
                return null;
            }

            BindingInfo service = ep.getEndpointInfo().getBinding();
            boolean output = !isRequestor(message);
            for (BindingOperationInfo info : service.getOperations())
            {
                if (info.getName().getLocalPart().equals(opName.getLocalPart()))
                {
                    SoapBody body;
                    if (output)
                    {
                        body = info.getOutput().getExtensor(SoapBody.class);
                    }
                    else
                    {
                        body = info.getInput().getExtensor(SoapBody.class);
                    }

                    if (body != null && opName.getNamespaceURI().equals(body.getNamespaceURI()))
                    {
                        return info;
                    }
                }
            }
        }
        return bop;
    }

    @Override
    public void handleMessage(Message message) throws Fault
    {
        // In case there already exists an action or the binding was already added, the binding operation should
        // not be added.
        String action = (String) message.get(SOAP_ACTION);
        if (!isEmpty(action) || message.getExchange().get(BindingOperationInfo.class) != null)
        {
            return;
        }

        DepthXMLStreamReader xmlReader = getXMLStreamReader(message);

        if (!StaxUtils.toNextElement(xmlReader))
        {
            message.setContent(Exception.class, new RuntimeException("There must be a method name element."));
        }

        String opName = xmlReader.getLocalName();
        if (isRequestor(message) && opName.endsWith("Response"))
        {
            opName = opName.substring(0, opName.length() - 8);
        }

        BindingOperationInfo operation = getOperation(message, new QName(xmlReader.getNamespaceURI(), opName));
        message.getExchange().put(BindingOperationInfo.class, operation);
    }
}
