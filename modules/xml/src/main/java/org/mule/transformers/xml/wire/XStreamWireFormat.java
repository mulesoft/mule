/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml.wire;

import org.mule.transformers.wire.TransformerPairWireFormat;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.transformers.xml.XStreamFactory;
import org.mule.transformers.xml.XmlToObject;

import java.util.List;
import java.util.Map;

/**
 * Serializes objects using XStream. This is equivelent of using the ObjectToXml and
 * XmlToObject except that there is not source or return type checking. WireFormats
 * are only
 */
public class XStreamWireFormat extends TransformerPairWireFormat
{
    public XStreamWireFormat() throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        this(XStreamFactory.XSTREAM_XPP_DRIVER, null, null);
    }

    public XStreamWireFormat(String driverClassName, Map aliases, List converters)
        throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        XmlToObject in = new XmlToObject();
        in.setDriverClassName(driverClassName);
        in.setAliases(aliases);
        in.setConverters(converters);
        setInboundTransformer(in);

        ObjectToXml out = new ObjectToXml();
        out.setDriverClassName(driverClassName);
        out.setAliases(aliases);
        out.setConverters(converters);
        setOutboundTransformer(out);
    }

}
