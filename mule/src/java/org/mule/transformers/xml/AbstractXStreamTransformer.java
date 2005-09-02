/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.transformers.xml;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.transformers.AbstractTransformer;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.ClassHelper;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.XppDriver;

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
    private XStream xstream = null;
    private boolean useJaxpDom = false;
    private Map aliases;
    private List converters;

    public final XStream getXStream() throws TransformerException
    {
        if (xstream == null) {
            if (useJaxpDom) {
                xstream = new XStream(new DomDriver());
            } else {
                xstream = new XStream(new XppDriver());
            }
        }
        addAliases();
        addConverters();
        return xstream;
    }

    public boolean isUseJaxpDom()
    {
        return useJaxpDom;
    }

    public void setUseJaxpDom(boolean useJaxpDom)
    {
        this.useJaxpDom = useJaxpDom;
    }

    private void addAliases() throws TransformerException
    {
        if (aliases == null) {
            return;
        }

        Map.Entry entry;
        String classname;
        Class clazz;
        for (Iterator iterator = aliases.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry) iterator.next();
            classname = entry.getValue().toString();
            try {
                clazz = ClassHelper.loadClass(classname, getClass());
            } catch (ClassNotFoundException e) {
                throw new TransformerException(new Message(Messages.CLASS_X_NOT_FOUND, classname), this, e);
            }
            xstream.alias(entry.getKey().toString(), clazz);
        }
    }

    private void addConverters() throws TransformerException
    {
        if (converters == null) {
            return;
        }
        String classname;
        Class clazz;

        for (Iterator iterator = converters.iterator(); iterator.hasNext();) {
            classname = iterator.next().toString();
            try {
                clazz = ClassHelper.loadClass(classname, getClass());
                xstream.registerConverter((Converter) clazz.newInstance());
            } catch (Exception e) {
                throw new TransformerException(this, e);
            }
        }
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

}
