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
package org.mule.model;

import org.mule.umo.model.UMOEntryPointResolver;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.umo.model.ModelException;
import org.mule.umo.UMODescriptor;

/**
 * An entrypoint resolver that only allows Service objects that implmement the Callable interface
 * @see org.mule.umo.lifecycle.Callable
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CallableEntryPointResolver implements UMOEntryPointResolver
 {
    public UMOEntryPoint resolveEntryPoint(UMODescriptor componentDescriptor) throws ModelException {
        return new CallableEntryPoint();
    }
}
