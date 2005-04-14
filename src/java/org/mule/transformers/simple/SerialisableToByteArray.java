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
package org.mule.transformers.simple;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * <code>SerialisableToByteArray</code> converts a serialisable object or a String
 * to a byte array.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SerialisableToByteArray extends AbstractTransformer
{
    public SerialisableToByteArray()
    {
        registerSourceType(Serializable.class);
        registerSourceType(String.class);
        registerSourceType(byte[].class);
    }

    public Object doTransform(Object src) throws TransformerException
    {
        if (src instanceof byte[])
        {
            return (byte[]) src;
        } 
        byte[] dest = null;
        ByteArrayOutputStream bs = null;
        ObjectOutputStream os = null;
        try
        {
            bs = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bs);
            os.writeObject(src);
            os.flush();
            dest = bs.toByteArray();
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        } finally {
            try
            {
                if(os!=null) os.close();
            } catch (IOException e)
            {
                //ignore
            }
        }
        return dest;
    }
}
