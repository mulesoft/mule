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

import org.mule.config.i18n.MessageFactory;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.transformer.TransformerException;

import com.thoughtworks.xstream.XStream;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>AbstractXStreamTransformer</code> is a base class for all XStream based
 * transformers. It takes care of creating and configuring the XStream parser.
 */

public abstract class AbstractXStreamTransformer extends AbstractEventAwareTransformer
{
    private final AtomicReference/* XStream */xstream = new AtomicReference();
    private volatile String driverClassName = XStreamFactory.XSTREAM_XPP_DRIVER;
    private volatile Map aliases = null;
    private volatile List converters = null;

    public final XStream getXStream() throws TransformerException
    {
        XStream instance = (XStream) xstream.get();

        if (instance == null)
        {
            try
            {
                instance = new XStreamFactory(driverClassName, aliases, converters).getInstance();
                if (!xstream.compareAndSet(null, instance))
                {
                    instance = (XStream)xstream.get();
                }
            }
            catch (Exception e)
            {
                throw new TransformerException(MessageFactory.createStaticMessage("Unable to initialize XStream"), e);
            }
        }

        return instance;
    }

    public Object clone() throws CloneNotSupportedException
    {
        AbstractXStreamTransformer clone = (AbstractXStreamTransformer) super.clone();
        clone.setDriverClassName(driverClassName);

        if (aliases != null)
        {
            clone.setAliases(new HashMap(aliases));
        }
        
        if (converters != null)
        {
            clone.setConverters(new ArrayList(converters));
        }

        return clone;
    }

    public String getDriverClassName()
    {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName)
    {
        this.driverClassName = driverClassName;
        // force XStream instance update
        this.xstream.set(null);
    }

    public Map getAliases()
    {
        return aliases;
    }

    public void setAliases(Map aliases)
    {
        this.aliases = aliases;
        // force XStream instance update
        this.xstream.set(null);
    }

    public List getConverters()
    {
        return converters;
    }

    public void setConverters(List converters)
    {
        this.converters = converters;
        // force XStream instance update
        this.xstream.set(null);
    }

    protected boolean requiresCurrentEvent()
    {
        return false;
    }

}
