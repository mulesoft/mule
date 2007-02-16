/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.config.MuleDtdResolver;
import org.mule.config.XslHelper;
import org.mule.config.spring.editors.TransformerPropertyEditor;
import org.mule.umo.transformer.UMOTransformer;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.BeansDtdResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;

/**
 * <code>MuleBeanDefinitionReader</code> Is a custom Spring Bean reader that will
 * apply a transformation to Mule Xml configuration files before loading bean
 * definitions allowing Mule Xml config to be parsed as Spring configuration.
 */
public class MuleBeanDefinitionReader extends XmlBeanDefinitionReader
{
    private int contextCount = 0;
    private int configCount = 0;
    private MuleDtdResolver dtdResolver = null;

    public MuleBeanDefinitionReader(BeanDefinitionRegistry beanDefinitionRegistry, int configCount)
    {
        super(beanDefinitionRegistry);
        // default resource loader
        setResourceLoader(new MuleResourceLoader());
        // TODO Make this configurable as a property somehow.
        setValidationMode(VALIDATION_DTD);
        setEntityResolver(createEntityResolver());
        this.configCount = configCount;

        //Register Any custom property editors here
        ((DefaultListableBeanFactory)beanDefinitionRegistry).registerCustomEditor(UMOTransformer.class,
            new TransformerPropertyEditor((DefaultListableBeanFactory)beanDefinitionRegistry));
    }

    public int registerBeanDefinitions(Document document, Resource resource) throws BeansException
    {
        try
        {
            Document newDocument = transformDocument(document);
            return super.registerBeanDefinitions(newDocument, resource);
        }
        catch (Exception e)
        {
            throw new FatalBeanException("Failed to read config resource: " + resource, e);
        }
        finally
        {
            incConfigCount();
        }
    }

    public static Transformer createTransformer(Source source) throws TransformerConfigurationException
    {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(source);
        return transformer;
    }

    protected Document transformDocument(Document document) throws IOException, TransformerException
    {
        if (getXslResource() != null)
        {
            DOMResult result;
            try
            {
                Transformer transformer = createTransformer(createXslSource());
                result = new DOMResult();
                transformer.setParameter("firstContext", Boolean.valueOf(isFirstContext()));
                transformer.transform(new DOMSource(document), result);
            }
            finally
            {
                //If there are any configuration errors i.e. Some elements are no longer supported in Mule
                //Lets spit them out here
                if(XslHelper.hasErrorReport())
                {
                    String report = XslHelper.getErrorReport();
                    XslHelper.clearErrors();
                    throw new IOException(report);
                }
            }
            if (logger.isDebugEnabled())
            {
                try
                {
                    //If we have Dom4J on the classpath we can print out the generated XML
                    printResult(result);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            return (Document)result.getNode();
        }
        else
        {
            return document;
        }

    }

    protected void printResult(DOMResult result)
    {
        //If we have Dom4J on the classpath we can print out the generated XML
        //TODO this relies on Dom4j which is not in core, either we scrap this or do some reflection
        // trickery to print the XML. This is definitely useful for debugging
//        String xml = new DOMReader().read((Document)result.getNode()).asXML();
//        if (logger.isDebugEnabled())
//        {
//            logger.debug("Transformed document is:\n" + xml);
//        }
    }
    protected Source createXslSource() throws IOException
    {
        return new StreamSource(getXslResource().getInputStream(), getXslResource().getURL().toString());
    }

    protected ClassPathResource getXslResource()
    {
        String xsl = dtdResolver.getXslForDtd();
        if (xsl != null)
        {
            return new ClassPathResource(xsl);
        }
        else
        {
            return null;
        }
    }

    protected EntityResolver createEntityResolver()
    {
        if (dtdResolver == null)
        {
            MuleDtdResolver muleSpringResolver = new MuleDtdResolver("mule-spring-configuration.dtd",
                "mule-to-spring.xsl", new BeansDtdResolver());
            dtdResolver = new MuleDtdResolver("mule-configuration.dtd", "mule-to-spring.xsl",
                muleSpringResolver);
        }
        return dtdResolver;
    }

    public boolean isFirstContext()
    {
        return contextCount == 0;
    }

    private void incConfigCount()
    {
        contextCount++;
        if (contextCount >= configCount)
        {
            contextCount = 0;
        }
    }
}
