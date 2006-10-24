/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.codec;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.Base64;

/**
 * <code>Base64Encoder</code> transforms strings or byte arrays into Base64 encoded
 * string
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class Base64Decoder extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3297461704379793293L;

    public Base64Decoder()
    {
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(byte[].class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.transformer.UMOTransformer#transform(java.lang.Object)
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        String data = null;

        try
        {
            if (src instanceof byte[])
            {
                data = new String((byte[])src, encoding);
            }
            else
            {
                data = (String)src;
            }

            byte[] result = Base64.decode(data);

            if (getReturnClass().equals(String.class))
            {
                return new String(result, encoding);
            }
            else
            {
                return result;
            }
        }
        catch (Exception ex)
        {
            throw new TransformerException(new Message(Messages.TRANSFORM_FAILED_FROM_X_TO_X, "base64",
                this.getReturnClass().getName()), this, ex);
        }
    }

}
