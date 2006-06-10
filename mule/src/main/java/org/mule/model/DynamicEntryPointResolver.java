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

package org.mule.model;

import org.mule.umo.UMODescriptor;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.umo.model.UMOEntryPointResolver;

/**
 * <code>DynamicEntryPointResolver</code> is similar to the
 * <code>NonVoidEntryPointResolver</code> except it also allows for void entry
 * point s to be used. void entry points should be used with caution when
 * leaving event dispatching to the descretion of the Mule Server. If an event
 * is processed by a component and no return type is given, Mule will dispatch
 * the previous event assuming any changes by the componentwould have been made
 * to the previous event. In most situations thisbehaviour is fine, but there
 * are circumstances where this is not suitable.
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
     * Determinse if a void Entrypoint can be accepted. This will always return
     * true for this implementation
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
