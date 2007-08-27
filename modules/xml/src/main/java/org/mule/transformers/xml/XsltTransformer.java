/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * <code>XsltTransformer</code> performs an XSLT transform on a DOM (or other
 * XML-ish) object.
 */

public class XsltTransformer extends AbstractXmlTransformer
{
    // keep at least 1 XSLT Transformer ready by default
    private static final int MIN_IDLE_TRANSFORMERS = 1;
    // keep max. 32 XSLT Transformers around by default
    private static final int MAX_IDLE_TRANSFORMERS = 32;
    // MAX_IDLE is also the total limit
    private static final int MAX_ACTIVE_TRANSFORMERS = MAX_IDLE_TRANSFORMERS;

    protected final GenericObjectPool transformerPool;

    private volatile String xslTransformerFactoryClassName;
    private volatile String xslFile;
    private volatile String xslt;

    public XsltTransformer()
    {
        super();
        transformerPool = new GenericObjectPool(new PooledXsltTransformerFactory());
        transformerPool.setMinIdle(MIN_IDLE_TRANSFORMERS);
        transformerPool.setMaxIdle(MAX_IDLE_TRANSFORMERS);
        transformerPool.setMaxActive(MAX_ACTIVE_TRANSFORMERS);
    }

    /**
     *
     */
    public void initialise() throws InitialisationException
    {
        try
        {
            transformerPool.addObject();
        }
        catch (Throwable te)
        {
            throw new InitialisationException(te, this);
        }
    }

    /**
     * Transform, using XSLT, a XML String to another String.
     * 
     * @param src The source XML (String, byte[], DOM, etc.)
     * @return The result String (or DOM)
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            Source sourceDoc = this.getXmlSource(src);
            if (sourceDoc == null)
            {
                return null;
            }

            ResultHolder holder = getResultHolder(returnClass);
            if (holder == null)
            {
                holder = getResultHolder(src.getClass());
            }

            DefaultErrorListener errorListener = new DefaultErrorListener(this);
            Transformer transformer = null;
            Object result;

            try
            {
                transformer = (Transformer) transformerPool.borrowObject();

                transformer.setErrorListener(errorListener);
                transformer.setOutputProperty(OutputKeys.ENCODING, encoding);

                transformer.transform(sourceDoc, holder.getResult());
                result = holder.getResultObject();

                if (errorListener.isError())
                {
                    throw errorListener.getException();
                }
            }
            finally
            {
                if (transformer != null)
                {
                    transformerPool.returnObject(transformer);
                }
            }

            return result;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    /**
     * Returns the name of the currently configured javax.xml.transform.Transformer
     * factory class used to create XSLT Transformers.
     * 
     * @return a TransformerFactory class name or <code>null</code> if none has
     *         been configured
     */
    public String getXslTransformerFactory()
    {
        return xslTransformerFactoryClassName;
    }

    /**
     * Configures the javax.xml.transform.Transformer factory class
     * 
     * @param xslTransformerFactory the name of the TransformerFactory class to use
     */
    public void setXslTransformerFactory(String xslTransformerFactory)
    {
        this.xslTransformerFactoryClassName = xslTransformerFactory;
    }

    /**
     * @return Returns the xslFile.
     */
    public String getXslFile()
    {
        return xslFile;
    }

    /**
     * @param xslFile The xslFile to set.
     */
    public void setXslFile(String xslFile)
    {
        this.xslFile = xslFile;
    }

    public String getXslt()
    {
        return xslt;
    }

    public void setXslt(String xslt)
    {
        this.xslt = xslt;
    }

    /**
     * Returns the StreamSource corresponding to xslFile
     * 
     * @return The StreamSource
     */
    protected StreamSource getStreamSource() throws InitialisationException
    {
        if (xslt != null)
        {
            return new StreamSource(new StringReader(xslt));
        }

        if (xslFile == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("xslFile"), this);
        }

        InputStream is;
        try
        {
            is = IOUtils.getResourceAsStream(xslFile, getClass());
        }
        catch (IOException e)
        {
            throw new InitialisationException(e, this);
        }
        if (is != null)
        {
            return new StreamSource(is);
        }
        else
        {
            throw new InitialisationException(CoreMessages.failedToLoad(xslFile), this);
        }
    }

    protected class PooledXsltTransformerFactory extends BasePoolableObjectFactory
    {
        public Object makeObject() throws Exception
        {
            StreamSource source = XsltTransformer.this.getStreamSource();
            String factoryClassName = XsltTransformer.this.getXslTransformerFactory();
            TransformerFactory factory;

            if (StringUtils.isNotEmpty(factoryClassName))
            {
                factory = (TransformerFactory) ClassUtils.instanciateClass(factoryClassName,
                    ClassUtils.NO_ARGS, this.getClass());
            }
            else
            {
                // fall back to JDK default
                factory = TransformerFactory.newInstance();
            }

            factory.setURIResolver(new URIResolver()
            {
                public Source resolve(String href, String base)
                    throws javax.xml.transform.TransformerException
                {
                    try
                    {
                        return new StreamSource(IOUtils.getResourceAsStream(href, getClass()));
                    }
                    catch (IOException e)
                    {
                        throw new javax.xml.transform.TransformerException(e);
                    }
                }
            });
            return factory.newTransformer(source);
        }
    }

    protected class DefaultErrorListener implements ErrorListener
    {
        private TransformerException e = null;
        private final UMOTransformer trans;

        public DefaultErrorListener(UMOTransformer trans)
        {
            this.trans = trans;
        }

        public TransformerException getException()
        {
            return e;
        }

        public boolean isError()
        {
            return e != null;
        }

        public void error(javax.xml.transform.TransformerException exception)
            throws javax.xml.transform.TransformerException
        {
            e = new TransformerException(trans, exception);
        }

        public void fatalError(javax.xml.transform.TransformerException exception)
            throws javax.xml.transform.TransformerException
        {
            e = new TransformerException(trans, exception);
        }

        public void warning(javax.xml.transform.TransformerException exception)
            throws javax.xml.transform.TransformerException
        {
            logger.warn(exception.getMessage());
        }
    }

    /**
     * @return The current maximum number of allowable active transformer objects in
     *         the pool
     */
    public int getMaxActiveTransformers()
    {
        return transformerPool.getMaxActive();
    }

    /**
     * Sets the the current maximum number of active transformer objects allowed in the
     * pool
     * 
     * @param maxActiveTransformers New maximum size to set
     */
    public void setMaxActiveTransformers(int maxActiveTransformers)
    {
        transformerPool.setMaxActive(maxActiveTransformers);
    }

    /**
     * @return The current maximum number of allowable idle transformer objects in
     *         the pool
     */
    public int getMaxIdleTransformers()
    {
        return transformerPool.getMaxIdle();
    }

    /**
     * Sets the the current maximum number of idle transformer objects allowed in the
     * pool
     * 
     * @param maxIdleTransformers New maximum size to set
     */
    public void setMaxIdleTransformers(int maxIdleTransformers)
    {
        transformerPool.setMaxIdle(maxIdleTransformers);
    }

}
