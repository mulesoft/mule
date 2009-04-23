/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.transformer;

import org.mule.api.transformer.TransformerException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.AbstractMessageAwareTransformer;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

/**
 * <code>AbstractXStreamTransformer</code> is a base class for all XStream based
 * transformers. It takes care of creating and configuring the XStream parser.
 */

public abstract class AbstractXStreamTransformer extends AbstractMessageAwareTransformer
{
    private final AtomicReference/* XStream */xstream = new AtomicReference();
    private volatile String driverClass = XStreamFactory.XSTREAM_XPP_DRIVER;
    private volatile Map<String, Class> aliases = null;
    private volatile List<Class> converters = null;

    public final XStream getXStream() throws TransformerException
    {
        XStream instance = (XStream) xstream.get();

        if (instance == null)
        {
            try
            {
                instance = new XStreamFactory(driverClass, aliases, converters).getInstance();
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
        clone.setDriverClass(driverClass);

        if (aliases != null)
        {
            clone.setAliases(new HashMap<String, Class>(aliases));
        }
        
        if (converters != null)
        {
            clone.setConverters(new ArrayList<Class>(converters));
        }

        return clone;
    }

    public String getDriverClass()
    {
        return driverClass;
    }

    public void setDriverClass(String driverClass)
    {
        this.driverClass = driverClass;
        // force XStream instance update
        this.xstream.set(null);
    }

    public Map getAliases()
    {
        return aliases;
    }

    public void setAliases(Map<String, Class> aliases)
    {
        this.aliases = aliases;
        // force XStream instance update
        this.xstream.set(null);
    }

    public List getConverters()
    {
        return converters;
    }

    public void setConverters(List<Class> converters)
    {
        this.converters = converters;
        // force XStream instance update
        this.xstream.set(null);
    }
}
