/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.w3c.dom.Element;

/**
 * Represents a static config data object where the body of the element can be the data of a file
 * attribute can be set.  Data will be loaded from file into an {@link java.io.InputStream}
 */
public class DataObjectDefinitionParser extends ChildDefinitionParser
{
    /**
     * The class will be inferred from the class attribute
     *
     * @param setterMethod The target method (where the child will be injected)
     */
    public DataObjectDefinitionParser(String setterMethod)
    {
        super(setterMethod, DataObjectFactoryBean.class);
    }

    /**
     * The class (which is inferred from the class attribute if null here) is checked to be
     * a subclass of the constraint
     *
     * @param setterMethod The target method (where the child will be injected)
     * @param constraint   Superclass of clazz (may be null)
     */
    public DataObjectDefinitionParser(String setterMethod, Class constraint)
    {
        super(setterMethod, DataObjectFactoryBean.class, constraint);
    }

    /**
     * The class (which is inferred from the class attribute if null here) is checked to be
     * a subclass of the constraint.
     *
     * @param setterMethod        The target method (where the child will be injected)
     * @param constraint          Superclass of clazz (may be null)
     * @param allowClassAttribute Is class read from class attribute (if present, takes precedence over clazz)
     */
    public DataObjectDefinitionParser(String setterMethod, Class constraint, boolean allowClassAttribute)
    {
        super(setterMethod, DataObjectFactoryBean.class, constraint, allowClassAttribute);
    }

    @Override
    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        if(StringUtils.isNotEmpty(element.getTextContent()))
        {
            assembler.extendBean("data", element.getTextContent(), false);
        }
        super.postProcess(context, assembler, element);
    }

    public static class DataObjectFactoryBean implements FactoryBean, ApplicationContextAware
    {
        private String ref;
        private boolean binary;
        private String file;
        private Object data;
        private ApplicationContext context;

        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
        {
            context = applicationContext;
        }

        public Object getObject() throws Exception
        {
            if(data!=null)
            {
                return data;
            }

            if(file!=null)
            {
                if(binary)
                {
                    data = IOUtils.toByteArray(IOUtils.getResourceAsStream(file, getClass()));
                }
                else
                {
                    data = IOUtils.getResourceAsString(file, getClass());
                }
            }
            else if(ref!=null)
            {
                data = context.getBean(ref);
            }

            if(data==null)
            {
                throw new IllegalArgumentException("Data is null was not found");
            }
            return data;
        }

        public Class getObjectType()
        {
            return Object.class;
        }

        public boolean isSingleton()
        {
            return true;
        }

        public String getFile()
        {
            return file;
        }

        public void setFile(String file)
        {
            this.file = file;
        }

        public String getRef()
        {
            return ref;
        }

        public void setRef(String ref)
        {
            this.ref = ref;
        }

        public Object getData()
        {
            return data;
        }

        public void setData(Object data)
        {
            this.data = data;
        }

        public boolean isBinary()
        {
            return binary;
        }

        public void setBinary(boolean binary)
        {
            this.binary = binary;
        }
    }
}
