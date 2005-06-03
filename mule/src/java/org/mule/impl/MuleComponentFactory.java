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
package org.mule.impl;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.model.UMOComponentFactory;

/**
 * <code>MuleComponentFactory</code> creates an instance of MuleComponent when
 * a Descriptor is registered
 * 
 * @see MuleComponent
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleComponentFactory implements UMOComponentFactory
{
    public UMOComponent create(UMODescriptor descriptor)
    {
        if (descriptor instanceof MuleDescriptor) {
            return new MuleComponent((MuleDescriptor) descriptor);
        } else {
            throw new IllegalArgumentException(new Message(Messages.CANT_SET_PROP_X_ON_X_OF_TYPE_X,
                                                           MuleDescriptor.class.getName(),
                                                           MuleComponent.class.getName(),
                                                           descriptor.getClass().getName()).getMessage());
        }
    }
}
