/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model;

import org.mule.umo.UMODescriptor;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.umo.model.UMOEntryPointResolver;

/**
 * <code>DynamicEntryPointResolver</code>
 * <OL>
 * <LI> Checks to see if the component implements the Callable lifecycle interface,
 * then the onCall(UMOEventContext) method will be used to receive the event.
 * <LI> If the component has a transformer configured for it, the return type for the
 * transformer will be matched against methods on the component to see if there is a
 * method that accepts the transformer return type. If so this event will be used.
 * Note if there is more than one match, an exception will be thrown.
 * <LI> If there is a method on the component that accepts an
 * org.mule.umo.UMOEventContext . If so this event will be used. Note if there is
 * more than one match, an exception will be thrown.
 * <LI> The last chack determines if there are any meothds on the component that
 * accept a java.util.Event . If so this event will be used. Note if there is more
 * than one match, an exception will be thrown.
 * <LI> If none of the above find a match an exception will be thrown and the
 * component registration will fail.
 * </OL>
 * It allows also void methods where Mule assumes that the Payload itself of the
 * message will be modified.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DynamicEntryPointResolver implements UMOEntryPointResolver
{

    /**
     * Default Constructor
     */
    public DynamicEntryPointResolver()
    {
        super();
    }

    /**
     * Determinse if a void Entrypoint can be accepted. This will always return true
     * for this implementation
     * 
     * @return true
     */
    protected boolean isVoidOk()
    {
        return true;
    }

    public UMOEntryPoint resolveEntryPoint(UMODescriptor descriptor) throws ModelException
    {
        return new DynamicEntryPoint();
    }
}
