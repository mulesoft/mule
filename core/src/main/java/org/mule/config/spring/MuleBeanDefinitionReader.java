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

import java.io.IOException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.io.DOMReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeansDtdResolver;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
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
    /**
     * logger used by this class
     */
    protected static transient final Log logger = LogFactory.getLog(MuleBeanDefinitionReader.class);

    private int contextCount = 0;
    private int configCount = 0;
    private MuleDtdResolver dtdResolver = null;

    public MuleBeanDefinitionReader(BeanDefinitionRegistry beanDefinitionRegistry, int configCount)
    {
        super(beanDefinitionRegistry);
        this.configCount = configCount;
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
            //reset validation mode
            setValidationMode(VALIDATION_AUTO);
        }
    }

    public static Transformer createTransformer(Source source) throws TransformerConfigurationException
    {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setErrorListener(new ErrorListener()
        {

            public void warning(TransformerException exception) throws TransformerException
            {
                logger.warn("failed to create transformer: " + exception.getMessage(), exception);
            }

            public void error(TransformerException exception) throws TransformerException
            {
                logger.error("failed to create transformer: " + exception.getMessage(), exception);
            }

            public void fatalError(TransformerException exception) throws TransformerException
            {
                logger.fatal("failed to create transformer: " + exception.getMessage(), exception);
            }
        });
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
                    String report = XslHelper.getFullReport();
                    IOException ex = new LegacyXmlException(report, XslHelper.getWarnings(), XslHelper.getErrors());
                    XslHelper.clearReport();
                    throw ex;
                }
                else if (XslHelper.hasWarningReport())
                {
                    logger.warn(XslHelper.getWarningReport());
                }
                XslHelper.clearReport();
            }

            try
            {
                //If we have Dom4J on the classpath we can print out the generated XML
                printResult(result);
            }
            catch (Exception e)
            {
                e.printStackTrace();
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
        //TODO RM*  this relies on Dom4j which is not in core, either we scrap this or do some reflection
        // trickery to print the XML. This is definitely useful for debugging
        String xml = new DOMReader().read((Document)result.getNode()).asXML();
        System.out.println(xml);
        if (logger.isDebugEnabled())
        {
            //logger.debug("Transformed document is:\n" + xml);
        }
    }
    protected Source createXslSource() throws IOException
    {
        return new StreamSource(getXslResource().getInputStream(), getXslResource().getURL().toString());
    }

    protected ClassPathResource getXslResource()
    {
        if(dtdResolver!=null)
        {
            String xsl = dtdResolver.getXslForDtd();
            if (xsl != null)
            {
                return new ClassPathResource(xsl);
            }
        }
        return null;
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


    //@Override
    protected int detectValidationMode(Resource resource)
    {
        int i = super.detectValidationMode(resource);
        if(i==VALIDATION_DTD)
        {
            setEntityResolver(createEntityResolver());
        }
        else
        {
            setEntityResolver(new ResourceEntityResolver(getResourceLoader()));
        }
        return i;
    }
}
