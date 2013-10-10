/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Use this BeanDefinitionParser when you need a "wrapper" element for an ObjectFactory.
 * For example, suppose we have the following class:
 * 
 *   class Car
 *   {
 *     private ObjectFactory<Wheel> wheel;
 *   }
 * 
 * The following registration in the namespace:
 *
 *   registerBeanDefinitionParser("wheel", new ObjectFactoryWrapper("wheel"));
 * 
 * would allow a config such as:
 * 
 *   <car>
 *     <wheel>
 *       <prototype-object class="com.wheelsrus.BigWheel">
 *         <properties>
 *           <spring:property name="tire" value="goodyear"/>
 *           <spring:property name="diameter" value="35R"/>
 *         </properties>
 *       </prototype-object>
 *     </wheel>
 *   </car>
 */
public class ObjectFactoryWrapper extends ParentDefinitionParser
{
    public static final String OBJECT_FACTORY_SETTER = "objectFactoryPropertyName";
    
    private String objectFactoryPropertyName;
    
    public ObjectFactoryWrapper(String objectFactoryPropertyName)
    {
        super();
        this.objectFactoryPropertyName = objectFactoryPropertyName;
    }

    protected void postProcess(ParserContext context, BeanAssembler assembler, Element element)
    {
        super.postProcess(context, assembler, element);
        BeanDefinition parent = getParentBeanDefinition(element);
        parent.setAttribute(OBJECT_FACTORY_SETTER, objectFactoryPropertyName);
    }
}
