/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import com.thoughtworks.xstream.XStream;
import org.mule.config.i18n.Message;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.transformer.TransformerException;

import java.util.List;
import java.util.Map;

/**
 * <code>AbstractXStreamTransformer</code> is a base class for all XStream based
 * transformers. It takes care of creating and configuring the XStream parser.
 */

public abstract class AbstractXStreamTransformer extends AbstractEventAwareTransformer
{
    private String driverClassName = XStreamFactory.XSTREAM_XPP_DRIVER;
    private XStream xstream;
    private Map aliases;
    private List converters;

    public synchronized final XStream getXStream() throws TransformerException
    {
        if (xstream == null)
        {
            try
            {
                xstream = new XStreamFactory(driverClassName, aliases, converters).getInstance();
            }
            catch (Exception e)
            {
                throw new TransformerException(Message.createStaticMessage("Unable to initialize XStream"), e);
            }
        }

        return xstream;
    }

    public synchronized String getDriverClassName()
    {
        return driverClassName;
    }

    public synchronized void setDriverClassName(String driverClassName)
    {
        this.driverClassName = driverClassName;
    }

    public synchronized Map getAliases()
    {
        return aliases;
    }

    public synchronized void setAliases(Map aliases)
    {
        this.aliases = aliases;
    }

    public synchronized List getConverters()
    {
        return converters;
    }

    public synchronized void setConverters(List converters)
    {
        this.converters = converters;
    }

    protected boolean requiresCurrentEvent()
    {
        return false;
    }

}
