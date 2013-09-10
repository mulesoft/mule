/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer.wire;

import org.mule.module.xml.transformer.ObjectToXml;
import org.mule.module.xml.transformer.XStreamFactory;
import org.mule.module.xml.transformer.XmlToObject;
import org.mule.transformer.wire.TransformerPairWireFormat;

import java.util.Map;
import java.util.Set;

/**
 * Serializes objects using XStream. This is equivelent of using the ObjectToXml and
 * XmlToObject except that there is no source or return type checking.
 */
public class XStreamWireFormat extends TransformerPairWireFormat
{
    
    public XStreamWireFormat() throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        this(XStreamFactory.XSTREAM_XPP_DRIVER, null, null);
    }

    public XStreamWireFormat(String driverClassName, Map aliases, Set converters)
        throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        XmlToObject in = new XmlToObject();
        in.setDriverClass(driverClassName);
        in.setAliases(aliases);
        in.setConverters(converters);
        setInboundTransformer(in);

        ObjectToXml out = new ObjectToXml();
        out.setDriverClass(driverClassName);
        out.setAliases(aliases);
        out.setConverters(converters);
        // TODO This is currently needed as a workaround for MULE-2881, this needs to
        // be removed is this is not the solution to MULE-2881
        out.setAcceptMuleMessage(true);
        setOutboundTransformer(out);
    }

}
