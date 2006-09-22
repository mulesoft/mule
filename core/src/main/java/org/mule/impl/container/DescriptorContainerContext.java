/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.container;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMODescriptor;
import org.mule.umo.manager.ContainerException;
import org.mule.umo.manager.ObjectNotFoundException;

import java.io.Reader;

/**
 * will Load the component form the descriptors' own properties
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DescriptorContainerContext extends AbstractContainerContext
{
    public static final String DESCRIPTOR_CONTAINER_NAME = "descriptor";

    public DescriptorContainerContext() {
        super(DESCRIPTOR_CONTAINER_NAME);
    }

    public void configure(Reader configuration) throws ContainerException {
        throw new UnsupportedOperationException("configure");
    }

    public void setName(String name) {
        //no op
    }

    /**
     * Queries a component from the underlying container
     *
     * @param key the key fo find the component with. Its up to the individual
     *            implementation to check the type of this key and look up
     *            objects accordingly
     * @return The component found in the container
     * @throws org.mule.umo.manager.ObjectNotFoundException
     *          if the component is not found
     */
    public Object getComponent(Object key) throws ObjectNotFoundException {

        if(key instanceof DescriptorContainerKeyPair) {
            DescriptorContainerKeyPair dckp =  (DescriptorContainerKeyPair)key;

            UMODescriptor d = MuleManager.getInstance().getModel().getDescriptor(dckp.getDescriptorName());
            if(d==null) {
                throw new ObjectNotFoundException(key.toString(),
                        new ContainerException(new Message(Messages.FAILED_LOAD_X, "descriptor: " + dckp.getDescriptorName())));
            }
            Object component = d.getProperties().get(dckp.getKey());
            if(component==null) {
                 throw new ObjectNotFoundException(dckp.getKey().toString());
            }
            return component;
        } else {
            throw new ObjectNotFoundException(key.toString());
        }
    }

}
