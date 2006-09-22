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

/**
 * Code by (c) 2005 P.Oikari.
 *
 * @author <a href="mailto:tsuppari@yahoo.co.uk">P.Oikari</a>
 * @version $Revision$
 */

public interface EjbAble
{
    /**
     * @return EJB remote method argument classtypes for call
     */
    public Class[] argumentClasses();

    /**
     * @return EJB remote method arguments that are used on EJB call
     */
    public Object[] arguments();
}
