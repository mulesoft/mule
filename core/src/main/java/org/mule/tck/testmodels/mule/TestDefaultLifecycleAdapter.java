/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.impl.DefaultLifecycleAdapter;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.model.UMOEntryPointResolverSet;

/** <code>TestDefaultLifecycleAdapter</code> TODO document */
public class TestDefaultLifecycleAdapter extends DefaultLifecycleAdapter
{
    /**
     * @param component
     * @param descriptor
     * @throws UMOException
     */
    public TestDefaultLifecycleAdapter(Object component, UMODescriptor descriptor) throws UMOException
    {
        super(component, descriptor);
    }

    /**
     * @param component
     * @param descriptor
     * @param epResolver
     * @throws UMOException
     */
    public TestDefaultLifecycleAdapter(Object component,
                                       UMODescriptor descriptor,
                                       UMOEntryPointResolverSet epResolver) throws UMOException
    {
        super(component, descriptor, epResolver);
    }

}
