/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.persistence;

import org.mule.api.DefaultMuleException;

/**
 * <code>PersistenceException</code> is the exception thrown by
 * the PersistenceStore and/or Manager.
 */
public class PersistenceException extends DefaultMuleException
{
    public PersistenceException()
    {
        super("");
    }
}

