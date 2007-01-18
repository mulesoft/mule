/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import java.util.HashMap;

/**
 * The ComponentReferenceFactory returns ComponentReferences that
 * are used to store registry information about a component.
 *
 * @author <a href="mailto:lajos@mulesource.com">Lajos Moczar</a>
 * @version $Revision: $
 */

public interface ComponentReferenceFactory 
{
    public static String REF_MULE_COMPONENT = "mule-component";
    public static String REF_OSGI_REGISTRATION = "osgi-registration";

    /**
     * Returns a ComponentReference that can be used for storing
     * the reference information. Default call will return the default
     * component reference (BasicComponentReference)
     *
     * @return the ComponentReference
     */
    ComponentReference getInstance();

    /**
     * Returns a ComponentReference that can be used for storing
     * the reference information. The actually type of reference object
     * will be determined by the "referenceType" variable.
     *
     * @param referenceType the type of reference ("OSGi_Bundle")
     * @return the ComponentReference
     */
    ComponentReference getInstance(String referenceType);
}
