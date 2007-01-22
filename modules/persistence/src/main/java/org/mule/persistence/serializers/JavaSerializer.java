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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.mule.MuleException;
import org.mule.persistence.PersistenceHelper;
import org.mule.persistence.PersistenceSerializer;
import org.mule.transformers.xml.XStreamFactory;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.IOUtils;

/**
 * The JavaSerializer is just an interface to serialize an object
 * to file.
 */
public class JavaSerializer implements PersistenceSerializer 
{
    public JavaSerializer() 
    {
    }

    /**
     * {@inheritDoc}
     */
    public void initialise() throws InitialisationException 
    {
    }

    public void serialize(File f, Object data) throws Exception
    {
        serialize(f, data, null);
    }

    public void serialize(File f, Object data, PersistenceHelper helper) throws Exception
    {
        ObjectOutput out = new ObjectOutputStream(new FileOutputStream(f));
        out.writeObject(data);
        out.close();
    }

}



