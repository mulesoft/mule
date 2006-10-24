/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs.transformers;

import net.jini.core.entry.Entry;

import org.mule.providers.gs.GigaSpacesEntryConverter;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

public class UMOMessageToGigaSpacesEntry extends AbstractEventAwareTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3378344993441495689L;

    private GigaSpacesEntryConverter converter = new GigaSpacesEntryConverter();

    public UMOMessageToGigaSpacesEntry()
    {
        setReturnClass(Entry.class);
    }

    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException
    {
        Object result = null;
        UMOMessage msg = context.getMessage();
        if (Object[].class.isAssignableFrom(src.getClass()))
        {
            Object[] srcArr = (Object[])src;
            Object[] resultArr = new Object[srcArr.length];

            for (int i = 0; i < srcArr.length; ++i)
            {
                resultArr[i] = converter.toEntry(srcArr[i], msg);
            }
            result = resultArr;
        }
        else
        {
            result = converter.toEntry(src, msg);
        }
        return result;
    }

    protected Object checkReturnClass(Object object) throws TransformerException
    {
        // if (returnClass != null) {
        // if (!returnClass.isInstance(object)) {
        // throw new TransformerException(new
        // Message(Messages.TRANSFORM_X_UNEXPECTED_TYPE_X,
        // object.getClass().getName(),
        // returnClass.getName()), this);
        // }
        // }
        // logger.debug("The transformed object is of expected type. Type is: " +
        // object.getClass().getName());
        return object;
    }

}
