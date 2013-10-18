/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.FaultOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

/**
 * Fault out interceptor for Proxy configuration considering that FaultInfo might not have an associated class
 * and that it uses StaxDatabinding.
 */
public class ProxyFaultOutInterceptor extends FaultOutInterceptor
{
    protected transient Log logger = LogFactory.getLog(getClass());

    @Override
    public void handleMessage(Message message) throws Fault {
        Fault f = (Fault)message.getContent(Exception.class);

        Throwable cause = f.getCause();
        if (cause == null) {
            return;
        }

        BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
        if (bop == null) {
            return;
        }
        FaultInfo fi = getFaultForClass(bop, cause.getClass());

        if (cause instanceof Exception && fi != null) {
            Exception ex = (Exception)cause;
            Object bean = getFaultBean(cause, fi, message);
            Service service = message.getExchange().get(Service.class);

            MessagePartInfo part = fi.getMessageParts().iterator().next();
            DataBinding db = service.getDataBinding();

            try
            {
                if (f.hasDetails())
                {
                    XMLStreamWriter xsw = new W3CDOMStreamWriter(f.getDetail());
                    DataWriter<XMLStreamWriter> writer = db.createWriter(XMLStreamWriter.class);
                    writer.write(bean, part, xsw);
                } else
                {
                    XMLStreamWriter xsw = new W3CDOMStreamWriter(f.getOrCreateDetail());
                    DataWriter<XMLStreamWriter> writer = db.createWriter(XMLStreamWriter.class);
                    writer.write(bean, part, xsw);
                    if (!f.getDetail().hasChildNodes())
                    {
                        f.setDetail(null);
                    }
                }

                f.setMessage(ex.getMessage());
            }
            catch (Exception fex) {
                //ignore - if any exceptions occur here, we'll ignore them
                //and let the default fault handling of the binding convert
                //the fault like it was an unchecked exception.
                logger.warn("Exception while writing fault", fex);
            }

        }
    }

    @Override
    public FaultInfo getFaultForClass(BindingOperationInfo op, Class class1) {
        for (BindingFaultInfo bfi : op.getFaults()) {

            FaultInfo faultInfo = bfi.getFaultInfo();
            Class<?> c = (Class)faultInfo.getProperty(Class.class.getName());
            if (c != null && c.isAssignableFrom(class1)) {
                return faultInfo;
            }
        }

        return null;
    }

}
