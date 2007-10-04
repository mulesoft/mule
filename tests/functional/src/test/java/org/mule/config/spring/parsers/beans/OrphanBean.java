/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.beans;

import org.mule.config.spring.parsers.beans.AbstractBean;
import org.mule.config.spring.parsers.beans.ChildBean;

import java.util.Collection;

/**
 * A standalone bean used in testing
 */
public class OrphanBean extends AbstractBean
{

    // test implicit naming of collection
    private Collection kids;
    // test explicit naming of collection
    private Collection offspring;

    // test simple setter/getter
    private ChildBean child;

    private Object object;

    public ChildBean getChild()
    {
        return child;
    }

    public void setChild(ChildBean child)
    {
        this.child = child;
    }

    public Collection getKids()
    {
        return kids;
    }

    public void setKids(Collection kids)
    {
        this.kids = kids;
    }

    public Collection getOffspring()
    {
        return offspring;
    }

    public void setOffspring(Collection offspring)
    {
        this.offspring = offspring;
    }

    public Object getObject()
    {
        return object;
    }

    public void setObject(Object object)
    {
        this.object = object;
    }
    
}
