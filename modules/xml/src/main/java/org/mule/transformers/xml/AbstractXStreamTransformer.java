/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import com.thoughtworks.xstream.XStream;

import java.util.List;
import java.util.Map;

import org.mule.config.i18n.Message;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.XStreamFactory;

/**
 * <code>AbstractXStreamTransformer</code> is a base class for all XStream
 * based transformers. It takes care of creating and configuring the xstream
 * parser
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractXStreamTransformer extends AbstractEventAwareTransformer
{
    private XStreamFactory xstream = null;
    private boolean useJaxpDom = false;
    private Map aliases;
    private List converters;

    public final XStream getXStream() throws TransformerException
    {
        if (xstream == null)  {
        	try {
        		xstream = new XStreamFactory(useJaxpDom, aliases, converters);
        	} catch (Exception e) {
                throw new TransformerException(Message.createStaticMessage("Unable to initialize XStream"), e);
        	}
        }
        return xstream.getInstance();
    }

    public boolean isUseJaxpDom()
    {
        return useJaxpDom;
    }

    public void setUseJaxpDom(boolean useJaxpDom)
    {
        this.useJaxpDom = useJaxpDom;
    }

    public Map getAliases()
    {
        return aliases;
    }

    public void setAliases(Map aliases)
    {
        this.aliases = aliases;
    }

    public List getConverters()
    {
        return converters;
    }

    public void setConverters(List converters)
    {
        this.converters = converters;
    }

    protected boolean requiresCurrentEvent() {
        return false;
    }
}
