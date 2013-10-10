/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.assembly.configuration;

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
