/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.simple;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

/**
 * <code>PassThroughComponent</code> will simply return the payload back as the
 * result
 * 
 * @author <a href="mailto:lajos@galatea.com">Lajos Moczar</a>
 * @version $Revision$
 */
public class PassThroughComponent implements Callable
{
    public Object onCall(UMOEventContext context) throws Exception
    {
        return context.getTransformedMessage();
    }

}
