/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleSingleBeanDefinitionParser;
import org.mule.impl.model.ModelFactory;
import org.mule.registry.ServiceException;
import org.mule.umo.model.UMOModel;
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
    }

    protected Class getBeanClass(Element element)
    {
        return ModelFactoryBean.class;
    }

    

    public class ModelFactoryBean extends AbstractFactoryBean
    {

        private UMOModel model;


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
            model = (UMOModel) ClassUtils.instanciateClass(getObjectType(), ClassUtils.NO_ARGS);
            return model;
        }

        //@java.lang.Override
        public void afterPropertiesSet() throws Exception
        {
            super.afterPropertiesSet();
            model.initialise();
        }

        //@java.lang.Override
        public void destroy() throws Exception
        {
            super.destroy();
            model.dispose();
        }
    }


}
