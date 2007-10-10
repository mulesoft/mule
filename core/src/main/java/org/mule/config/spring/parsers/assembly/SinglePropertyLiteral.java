/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.assembly;

public class SinglePropertyLiteral implements SingleProperty
{

    private boolean isCollection;
    private boolean isIgnored;
    private boolean isReference;

    public SinglePropertyLiteral(boolean isCollection, boolean isIgnored, boolean isReference)
    {
        this.isCollection = isCollection;
        this.isIgnored = isIgnored;
        this.isReference = isReference;
    }

    public SinglePropertyLiteral(boolean isReference)
    {
        this(false, false, isReference);
    }

    public SinglePropertyLiteral()
    {
        this(false, false, false);
    }

    public boolean isCollection()
    {
        return isCollection;
    }

    public void setCollection()
    {
        this.isCollection = true;
    }

    public boolean isIgnored()
    {
        return isIgnored;
    }

    public void setIgnored()
    {
        this.isIgnored = true;
    }

    public boolean isReference()
    {
        return isReference;
    }

    public void setReference()
    {
        this.isReference = true;
    }

}
