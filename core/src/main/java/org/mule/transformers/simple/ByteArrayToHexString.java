/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.StringUtils;

/**
 * Converts a Byte array to a Hex String
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ByteArrayToHexString extends AbstractTransformer
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -7444711426569720031L;

    private volatile boolean upperCase = false;

    public ByteArrayToHexString()
    {
        registerSourceType(byte[].class);
        setReturnClass(String.class);
    }

    public boolean getUpperCase()
    {
        return upperCase;
    }

    public void setUpperCase(boolean value)
    {
        upperCase = value;
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src == null) {
            return StringUtils.EMPTY;
        }

        try {
            return StringUtils.toHexString((byte[])src, upperCase);
        }
        catch (Exception ex) {
            throw new TransformerException(this, ex);
        }
    }

}
