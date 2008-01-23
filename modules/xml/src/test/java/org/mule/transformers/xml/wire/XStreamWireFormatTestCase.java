/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml.wire;

import org.mule.api.transformer.wire.WireFormat;
import org.mule.transformer.wire.AbstractWireFormatTestCase;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.transformers.xml.XmlToObject;

public class XStreamWireFormatTestCase extends AbstractWireFormatTestCase
{

    protected WireFormat getWireFormat() throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        return new XStreamWireFormat();
    }

    public void testGetDefaultInboundTransformer()
        throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        assertEquals(XmlToObject.class, ((XStreamWireFormat) getWireFormat()).getInboundTransformer().getClass());

    }

    public void testGetDefaultOutboundTransformer()
        throws IllegalAccessException, InstantiationException, ClassNotFoundException
    {
        assertEquals(ObjectToXml.class, ((XStreamWireFormat) getWireFormat()).getOutboundTransformer().getClass());
    }

}
