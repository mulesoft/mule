/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.fruit;

import org.springframework.beans.factory.FactoryBean;

/**
 * A Spring FactoryBean implementation for unit testing.
 */
public class FloridaSunnyOrangeFactoryBean implements FactoryBean
{
    Integer segments = new Integer(10);
    Double radius = new Double(4.34);
    
    public Object getObject() throws Exception
    {
        return new Orange(segments, radius, "Florida Sunny");
    }

    public Class getObjectType()
    {
        return Orange.class;
    }

    public boolean isSingleton()
    {
        return false;
    }

    public Double getRadius()
    {
        return radius;
    }

    public void setRadius(Double radius)
    {
        this.radius = radius;
    }

    public Integer getSegments()
    {
        return segments;
    }

    public void setSegments(Integer segments)
    {
        this.segments = segments;
    }
}
