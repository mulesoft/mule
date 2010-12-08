/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.InvokerMethodDefinitionParser;
import org.mule.config.spring.util.ParamReader;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Turns a POJO into a config element and a set of method tags which map to MessageProcessors.
 */
public abstract class AbstractPojoNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void registerPojo(String configElementName, Class<?> cls)
    {
        // register a generic configuration element
        OrphanDefinitionParser parser = new OrphanDefinitionParser(cls, true);
        parser.addIgnored("name");
        registerMuleBeanDefinitionParser(configElementName, parser);

        // register invoker parser for each non setter
        try
        {
            ParamReader paramReader = new ParamReader(cls); // use the parameter names from our class
            for (Method m : cls.getDeclaredMethods())
            {
                // don't create parsers for setters
                if (!m.getName().startsWith("set")) 
                { 
                    String[] parameterNames = paramReader.getParameterNames(m);
                    
                    registerMuleBeanDefinitionParser(m.getName(), 
                        new InvokerMethodDefinitionParser("messageProcessor", m.getName(), parameterNames));
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
