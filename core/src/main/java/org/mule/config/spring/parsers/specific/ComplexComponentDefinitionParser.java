/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.MuleChildDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractDelegatingDefinitionParser;
import org.mule.config.spring.parsers.delegate.MapDefinitionParserMutator;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Allow definition of more complex components.  This combines functionality similar
 * to {@link org.mule.config.spring.parsers.delegate.SingleParentFamilyDefinitionParser}
 * with {@link org.mule.config.spring.parsers.delegate.MapDefinitionParserMutator}, allowing
 * a single element to be used to configure both the factory (if any confiuration is needed)
 * and set proeprties for the component.  Nested component contents will also be handled
 * correctly.
 */
public class ComplexComponentDefinitionParser extends AbstractDelegatingDefinitionParser
{

    private static final int START = 0;
    private static final int POST_CHILDREN = 1;
    private static final int COMPLETE = 2;

    private MuleChildDefinitionParser objectFactoryParser;
    private MapDefinitionParserMutator componentParser;
    private int state = START;

    public ComplexComponentDefinitionParser(MuleChildDefinitionParser objectFactoryParser,
                                            ChildDefinitionParser componentParser)
    {
        this("properties", objectFactoryParser, new String[]{}, componentParser);
    }

    public ComplexComponentDefinitionParser(String setter,
                                            MuleChildDefinitionParser objectFactoryParser,
                                            String[] objectFactoryAttributes,
                                            ChildDefinitionParser componentParser)
    {
        this.objectFactoryParser = objectFactoryParser;
        this.componentParser = new MapDefinitionParserMutator(setter, componentParser);
        // assign attributes appropriately
        objectFactoryParser.setIgnoredDefault(true);
        componentParser.setIgnoredDefault(false);
        for (int i = 0; i < objectFactoryAttributes.length; i++)
        {
            objectFactoryParser.removeIgnored(objectFactoryAttributes[i]);
            componentParser.addIgnored(objectFactoryAttributes[i]);
        }
    }

    public AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext)
    {
        switch (state)
        {
        case START:
            // first we parse the object factory, allowing it access to its own attributes
            // only.  this will search for the parent bean (which is presumably expecting a
            // factory to be injected) and wire itself up as the child.
            AbstractBeanDefinition objectFactoryDefn = objectFactoryParser.parseDelegate(element, parserContext);
            // next we fake the parent for the component, since this is where the properties
            // will need to be injected
            componentParser.forceParent(objectFactoryDefn);
            // now we do the initial parse for the component.  because this is wrapped in the
            // map mutator the properties are not set yet.  instead the bean assembler is stored
            // in the mutator
            AbstractBeanDefinition componentDefn1 = componentParser.parseDelegate(element, parserContext);
            state = POST_CHILDREN;
            // we return the component definition, which will be associated with this element.
            // child nodes will then extend that definition.  once they are processed we will
            // be called again.
            return componentDefn1;
        case POST_CHILDREN:
            // child nodes have now been processed, so we can set the properties in the factory.
            AbstractBeanDefinition componentDefn2 = componentParser.parseDelegate(element, parserContext);
            // that should be it.  since we are stateful we make sure that any duplicate call
            // triggers an error
            state = COMPLETE;
            return componentDefn2;
        default:
            throw new IllegalStateException("Statefil definition parser cannot be resued");

        }
    }

}
