/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.stax;

import javanet.staxutils.StAXSource;

import javax.xml.stream.XMLStreamReader;

/**
 * A StaxSource which gives us access to the underlying XMLStreamReader if we
 * are StaxCapable down the line.
 */
public class StaxSource extends StAXSource
{
    private XMLStreamReader reader;

    public StaxSource(XMLStreamReader reader)
    {
        super(reader);

        this.reader = reader;
    }

    public XMLStreamReader getXMLStreamReader()
    {
        return reader;
    }

}


