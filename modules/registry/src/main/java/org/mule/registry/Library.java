/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import java.util.List;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface Library extends Entry
{

    RegistryComponent[] getComponents();

    List getClassPathElements();

    boolean isClassLoaderParentFirst();

    void addComponent(RegistryComponent component);

    void removeComponent(RegistryComponent component);

    void install() throws RegistryException;

    void uninstall() throws RegistryException;

    /**
     * Return the descriptor for this component.
     * 
     * @return
     */
    RegistryDescriptor getDescriptor() throws RegistryException;

}
