/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.persistence.serializers;

import org.mule.MuleException;
import org.mule.persistence.PersistenceHelper;
import org.mule.persistence.PersistenceSerializer;
import org.mule.transformers.xml.XStreamFactory;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.IOUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

import java.io.File;
import java.io.FileOutputStream;

/**
 * The purpose of the XStreamSerializer is to customize the persistence
 * of a Persistable object. Two examples of persistence serializers are the
 * XStreamSerializer and the JavaSeralizer.
 */
public class XStreamSerializer implements PersistenceSerializer 
{
    private XStreamFactory xstreamFactory = null;

    public XStreamSerializer() 
    {
    }

    /**
     * {@inheritDoc}
     * @param managementContext
     */
    public void initialise(UMOManagementContext managementContext) throws InitialisationException
    {
        try 
        {
            xstreamFactory = new XStreamFactory();
        } 
        catch (Exception e)
        {
            String msg = "Unable to initialize the XStreamFactory: " + 
                    e.toString();
            xstreamFactory = null;
            throw new InitialisationException(new MuleException(msg), this);
        }
    }

    public void serialize(File f, Object data) throws Exception
    {
        serialize(f, data, null);
    }

    public void serialize(File f, Object data, PersistenceHelper helper) throws Exception
    {
        XStream xstream = xstreamFactory.getInstance();
        if (helper != null && helper instanceof Converter) {
            xstream.registerConverter((Converter)helper);
            /* TODO: get top tag out of there
            String cn = data.getClass().getName();
            int pos = cn.lastIndexOf(".");
            if (pos > -1) cn = cn.substring(pos+1);
            xstream.alias(cn, data.getClass());
            */
        }
        IOUtils.write(xstream.toXML(data), new FileOutputStream(f));
    }

}


