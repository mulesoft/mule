/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.model;

import org.mule.umo.model.ModelException;
import org.mule.util.StringMessageHelper;

/**
 * <code>NoSatisfiableMethodsException</code> is thrown by EntryPointResolvers when
 * the component passed has no methods that meet the criteria of the configured
 * EntryPointResolver.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 * @see UMOEntryPointResolver
 */
public class NoSatisfiableMethodsException extends ModelException
{
    private static final String msg = "Could not find entry point on: ";

    /**
     * @param component 
     */
    public NoSatisfiableMethodsException(Object component)
    {
        this(component, null);
    }

    public NoSatisfiableMethodsException(Object component, Exception cause)
    {
        super(msg + StringMessageHelper.getObjectValue(component), cause);
    }
}
