/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.module.xml.util.XMLUtils;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;

public class CustomXsltTransformerFactory extends TransformerFactory
{
    private final TransformerFactory delegate;

    public CustomXsltTransformerFactory()
    {
        super();

        TransformerFactory tf;
        try
        {
            tf = TransformerFactory.newInstance();
        }
        catch (TransformerFactoryConfigurationError e)
        {
            System.setProperty("javax.xml.transform.TransformerFactory", XMLUtils.TRANSFORMER_FACTORY_JDK5);
            tf = TransformerFactory.newInstance();
        }
        this.delegate = tf;
    }

    public boolean equals(Object obj)
    {
        return delegate.equals(obj);
    }

    public Source getAssociatedStylesheet(Source source, String s, String s1, String s2)
        throws TransformerConfigurationException
    {
        return delegate.getAssociatedStylesheet(source, s, s1, s2);
    }

    public Object getAttribute(String s)
    {
        return delegate.getAttribute(s);
    }

    public ErrorListener getErrorListener()
    {
        return delegate.getErrorListener();
    }

    public boolean getFeature(String s)
    {
        return delegate.getFeature(s);
    }

    public URIResolver getURIResolver()
    {
        return delegate.getURIResolver();
    }

    public int hashCode()
    {
        return delegate.hashCode();
    }

    public Templates newTemplates(Source source) throws TransformerConfigurationException
    {
        return delegate.newTemplates(source);
    }

    public Transformer newTransformer() throws TransformerConfigurationException
    {
        return delegate.newTransformer();
    }

    public Transformer newTransformer(Source source) throws TransformerConfigurationException
    {
        return delegate.newTransformer(source);
    }

    public void setAttribute(String s, Object obj)
    {
        delegate.setAttribute(s, obj);
    }

    public void setErrorListener(ErrorListener errorlistener)
    {
        delegate.setErrorListener(errorlistener);
    }

    public void setFeature(String s, boolean flag) throws TransformerConfigurationException
    {
        delegate.setFeature(s, flag);
    }

    public void setURIResolver(URIResolver uriresolver)
    {
        delegate.setURIResolver(uriresolver);
    }

    public String toString()
    {
        return delegate.toString();
    }
}
