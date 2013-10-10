/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
