/*
 * $Id: MuleBeanDefinitionReader.java 2179 2006-06-04 22:51:52Z holger $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.spring.config;

import org.dom4j.io.DOMReader;
import org.mule.config.MuleDtdResolver;
import org.mule.umo.transformer.UMOTransformer;
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

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import java.io.IOException;

/**
 * <code>MuleBeanDefinitionReader</code> Is a custom Spring Bean reader that
 * will apply a transformation to Mule Xml configuration files before loading
 * bean definitions allowing Mule Xml config to be parsed as Spring
 * configuration.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: 2179 $
 */
public class MuleBeanDefinitionReader extends XmlBeanDefinitionReader
{
    private int contextCount = 0;
    private int configCount = 0;
    private MuleDtdResolver dtdResolver = null;

    public MuleBeanDefinitionReader(BeanDefinitionRegistry beanDefinitionRegistry, int configCount)
    {
        super(beanDefinitionRegistry);
        setEntityResolver(createEntityResolver());
        this.configCount = configCount;
        ((DefaultListableBeanFactory) beanDefinitionRegistry).registerCustomEditor(
                UMOTransformer.class,
                new TransformerEditor());
    }

    public int registerBeanDefinitions(Document document, Resource resource) throws BeansException
    {
        try {
            Document newDocument = transformDocument(document);
            return super.registerBeanDefinitions(newDocument, resource);
        } catch (Exception e) {
            throw new FatalBeanException("Failed to read config resource: " + resource, e);
        } finally {
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
        if (getXslResource() != null) {
            Transformer transformer = createTransformer(createXslSource());
            DOMResult result = new DOMResult();
            transformer.setParameter("firstContext", Boolean.valueOf(isFirstContext()));
            transformer.transform(new DOMSource(document), result);
            if (logger.isDebugEnabled()) {
                try {
                    String xml = new DOMReader().read((Document) result.getNode()).asXML();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Transformed document is:\n" + xml);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return (Document) result.getNode();
        } else {
            return document;
        }

    }

    protected Source createXslSource() throws IOException
    {
        return new StreamSource(getXslResource().getInputStream(), getXslResource().getURL().toString());
    }

    protected ClassPathResource getXslResource()
    {
        String xsl = dtdResolver.getXslForDtd();
        if (xsl != null) {
            return new ClassPathResource(xsl);
        } else {
            return null;
        }
    }

    protected EntityResolver createEntityResolver()
    {
        if (dtdResolver == null) {
            MuleDtdResolver muleSpringResolver = new MuleDtdResolver("mule-spring-configuration.dtd",
                                                                     "mule-to-spring.xsl",
                                                                     new BeansDtdResolver());
            dtdResolver = new MuleDtdResolver("mule-configuration.dtd", "mule-to-spring.xsl", muleSpringResolver);
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
        if (contextCount >= configCount) {
            contextCount = 0;
        }
    }
}
