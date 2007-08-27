/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model.streaming;

import org.mule.impl.MuleDescriptor;
import org.mule.impl.model.AbstractModel;
import org.mule.impl.model.resolvers.StreamingEntryPointResolver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.model.UMOEntryPointResolver;

/**
 * TODO
 */
public class StreamingModel extends AbstractModel
{


    public StreamingModel()
    {
        super();
        //Set this entrypoint respolver by default
        setEntryPointResolver(new StreamingEntryPointResolver());
    }

    protected UMOComponent createComponent(UMODescriptor descriptor)
    {
        return new StreamingComponent((MuleDescriptor) descriptor, this);
    }

    public String getType()
    {
        return "streaming";
    }


    /*
    * (non-Javadoc)
    *
    * @see org.mule.umo.model.UMOModel#setEntryPointResolver(org.mule.umo.model.UMOEntryPointResolver)
    */
    public void setEntryPointResolver(UMOEntryPointResolver entryPointResolver)
    {
        if (!(entryPointResolver instanceof StreamingEntryPointResolver))
        {
            throw new IllegalArgumentException("EntrypointResolver needs to be an instance of " + StreamingEntryPointResolver.class.getName());
        }
        super.setEntryPointResolver(entryPointResolver);
    }
    
}
