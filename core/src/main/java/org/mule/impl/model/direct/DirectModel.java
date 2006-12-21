/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.direct;

import org.mule.impl.MuleDescriptor;
import org.mule.impl.model.AbstractModel;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;

public class DirectModel extends AbstractModel
{

    protected UMOComponent createComponent(UMODescriptor descriptor)
    {
        return new DirectComponent((MuleDescriptor)descriptor, this);
    }

    /**
     * Returns the model type name. This is a friendly identifier that is used to
     * look up the SPI class for the model
     * 
     * @return the model type
     */
    public String getType()
    {
        return "direct";
    }
}
