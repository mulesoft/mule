/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.test.transformers;

import org.mule.transformers.CompressionTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.compression.CompressionHelper;

import java.io.IOException;

/**
 * <code>ReverseCompressionTransformer</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ReverseCompressionTransformer extends CompressionTransformer
{
    private String beanProperty1;
    private int beanProperty2;

    /**
     * 
     */
    public ReverseCompressionTransformer()
    {
        super();
        registerSourceType(String.class);
    }

    /* (non-Javadoc)
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object doTransform(Object src) throws TransformerException
    {
        String msg = (String) src;
        if (!getDoCompression())
        {
            return new StringBuffer(msg).reverse().toString();
        }
        try
        {
            if (CompressionHelper.isCompressed(msg.getBytes()))
            {
                msg = new String(uncompressMessage(src));
                return new StringBuffer(msg).reverse().toString();
            }
            else
            {
                msg = new StringBuffer(msg).reverse().toString();
                return new String(compressMessage(msg));
            }
        }
        catch (IOException e)
        {
            throw new TransformerException("Failed: " + e.getMessage(), e);
        }
    }

    /**
     * @return Returns the beanProperty1.
     */
    public String getBeanProperty1()
    {
        return beanProperty1;
    }

    /**
     * @param beanProperty1 The beanProperty1 to set.
     */
    public void setBeanProperty1(String beanProperty1)
    {
        this.beanProperty1 = beanProperty1;
    }

    /**
     * @return Returns the beanProperty2.
     */
    public int getBeanProperty2()
    {
        return beanProperty2;
    }

    /**
     * @param beanProperty2 The beanProperty2 to set.
     */
    public void setBeanProperty2(int beanProperty2)
    {
        this.beanProperty2 = beanProperty2;
    }

}
