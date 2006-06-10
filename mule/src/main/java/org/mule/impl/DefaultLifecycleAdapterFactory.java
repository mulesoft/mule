/* 
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.impl;

import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.UMOLifecycleAdapter;
import org.mule.umo.lifecycle.UMOLifecycleAdapterFactory;
import org.mule.umo.model.UMOEntryPointResolver;

/**
 * <code>DefaultLifecycleAdapterFactory</code> TODO (document class)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DefaultLifecycleAdapterFactory implements UMOLifecycleAdapterFactory
{
    public DefaultLifecycleAdapterFactory()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.UMOLifecycleAdapterFactory#create(java.lang.Object,
     *      org.mule.umo.UMODescriptor)
     */
    public UMOLifecycleAdapter create(Object component, UMODescriptor descriptor, UMOEntryPointResolver resolver)
            throws UMOException
    {
        return new DefaultLifecycleAdapter(component, descriptor, resolver);
    }

}
