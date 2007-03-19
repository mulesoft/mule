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

import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A configurable message transformer that allows users to add, overwrite and delete
 * properties on the current message. Users can set a {@link Set} of
 * 'deleteProperties' names to remove from the message and can also set a {@link Map}
 * of 'addProperties' that will be added to the message and possibly overwrite
 * existing properties with the same name.
 */
public class MessagePropertiesTransformer extends AbstractEventAwareTransformer
{
    private Set deleteProperties = Collections.EMPTY_SET;
    private Map addProperties = Collections.EMPTY_MAP;

    public MessagePropertiesTransformer()
    {
        registerSourceType(Object.class);
        setReturnClass(Object.class);
    }

    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException
    {
        if (deleteProperties != null && deleteProperties.size() > 0)
        {
            for (Iterator iterator = deleteProperties.iterator(); iterator.hasNext();)
            {
                Object o = iterator.next();
                context.getMessage().removeProperty(o.toString());
            }
        }

        if (addProperties != null && addProperties.size() > 0)
        {
            for (Iterator iterator = addProperties.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry entry = (Map.Entry)iterator.next();
                if (entry.getKey() == null)
                {
                    logger.error("Setting Null property keys is not supported, this entry is being ignored");
                }
                else
                {
                    context.getMessage().setProperty(entry.getKey().toString(), entry.getValue());
                }
            }
        }
        return context.getMessage();
    }

    public Set getDeleteProperties()
    {
        return deleteProperties;
    }

    public void setDeleteProperties(Set deleteProperties)
    {
        this.deleteProperties = deleteProperties;
    }

    public Map getAddProperties()
    {
        return addProperties;
    }

    public void setAddProperties(Map addProperties)
    {
        this.addProperties = addProperties;
    }
}
