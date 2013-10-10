/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.transformer.jaxb;

import org.mule.api.MuleContext;
import org.mule.config.transformer.AbstractAnnotatedTransformerArgumentResolver;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * This resolver is used by the transform engine to inject a JAXBContext into a method that requires it.
 * A shared JAXB context can be created for the application and stored in the registry, this will get injected
 * into any transform methods that add {@link javax.xml.bind.JAXBContext} to the method signature.
 * <p/>
 * IF there is no shared JAXB context one will be created. First this resolver will attempt to create the context from
 * the package of the the annotated class, for this to work either a jaxb.index file must be present or an {@link javax.naming.spi.ObjectFactory}
 * must be in the package.  This allows for JAXB generated classes to be used easily.  If this method fails a context will
 * be created using just the annotated class to initialise the context.
 *
 * @since 3.0
 */
public class JAXBContextResolver extends AbstractAnnotatedTransformerArgumentResolver
{
    protected Class getArgumentClass()
       {
           return JAXBContext.class;
       }

    protected Object createArgument(Class annotatedType, MuleContext muleContext) throws Exception
    {
       try
        {
            return JAXBContext.newInstance(annotatedType);
        }
        catch (JAXBException e)
        {
            //Fallback to just adding the annotated class to the context
            logger.warn(e.getMessage() + ". Initializing context using JAXB annotated class: " + annotatedType);
            return JAXBContext.newInstance(annotatedType);
        }
    }

   protected String getAnnotationsPackageName()
   {
       return "javax.xml.bind.annotation";
   }

}
