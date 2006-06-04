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

package org.mule.umo.model;

import org.mule.umo.UMODescriptor;

/**
 * <code>UMOEntryPointResolver</code> resolves a method to call on the given
 * UMODescriptor when an event is received for the component
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOEntryPointResolver
{
    UMOEntryPoint resolveEntryPoint(UMODescriptor componentDescriptor) throws ModelException;
}
