/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;

import org.mule.umo.lifecycle.InitialisationException;

import org.springframework.beans.factory.FactoryBean;

/**
 * This is a simple wrapper around a Spring FactoryBean (i.e., an object
 * which implements the org.springframework.beans.factory.FactoryBean 
 * interface) in order to use it within Mule's ObjectFactory framework.
 * 
 *   <model>
 *       <service name="myOrangeService">
 *           <component>
 *               <spring-factory-bean ref="&amp;myCustomFactory"/>
 *           </component>
 *       </service>
 *   </model>
 *
 *   <spring:bean id="myCustomFactory" class="org.mule.tck.testmodels.fruit.FloridaSunnyOrangeFactory"/>
 */
public class SpringFactoryBean implements ObjectFactory
{
    private FactoryBean factoryBean;

    public Object getOrCreate() throws Exception
    {
        return factoryBean.getObject();
    }

    public Class getObjectClass() throws Exception
    {
        return factoryBean.getObjectType();
    }
    
    public void initialise() throws InitialisationException
    {
        // empty
    }

    public void dispose()
    {
        // empty
    }

    public Object lookup(String id) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    public void release(Object object) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    public FactoryBean getFactoryBean()
    {
        return factoryBean;
    }

    public void setFactoryBean(FactoryBean factoryBean)
    {
        this.factoryBean = factoryBean;
    }
}