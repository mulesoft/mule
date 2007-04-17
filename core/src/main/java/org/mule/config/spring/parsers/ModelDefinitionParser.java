/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.impl.model.ModelFactory;
import org.mule.registry.ServiceException;
import org.mule.util.ClassUtils;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.w3c.dom.Element;

/**
 * Creates a FactoryBean that will discover the Model class to instantiate from the class path.
 */
public class ModelDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{
    private String type;

    public ModelDefinitionParser(String type)
    {
        this.type = type;
        this.singleton = true;
        this.initMethod = "initialise";
        this.destroyMethod = "dispose";
    }

    protected Class getBeanClass(Element element)
    {
        return ModelFactoryBean.class;
    }

    public class ModelFactoryBean extends AbstractFactoryBean
    {

        private String type;


        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }

        public Class getObjectType()
        {
            try
            {
                return ModelFactory.getModelClass(type);
            }
            catch (ServiceException e)
            {
                throw new BeanCreationException("Failed to load model class", e);
            }
        }

        protected Object createInstance() throws Exception
        {
            return ClassUtils.instanciateClass(getObjectType(), ClassUtils.NO_ARGS);
        }
    }
}
