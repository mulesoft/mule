/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.ejb;

import org.mule.providers.rmi.RmiMessageAdapter;
import org.mule.umo.provider.MessageTypeNotSupportedException;

/*
 * Wraps an object obtained by calling a method on an EJB object
 */

public class EjbMessageAdapter extends RmiMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3932390270676915501L;

    public EjbMessageAdapter(Object message) throws MessageTypeNotSupportedException
    {
        super(message);
    }
}
