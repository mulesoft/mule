/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.beans;

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
