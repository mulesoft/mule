/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.IOUtils;

/**
 * <code>XsltTransformer</code> performs an XSLT transform on a DOM (or other
 * XML-ish) object
 * 
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe Stephane</a>
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:jesper@selskabet.org">Jesper Steen Moller</a>
 * @version $Revision$
 */

public class XsltTransformer extends AbstractXmlTransformer
{
    /**
     * Serail version
     */
    private static final long serialVersionUID = -6958917343589717387L;

    private ObjectPool transformerPool;

    private int maxIdleTransformers = 2;

    private String xslFile;

    private String xslt;

    private static final int MIN_IDLE = 1;

    public XsltTransformer()
    {
        super();
    }

    /**
     * @see org.mule.umo.lifecycle.Initialisable#initialise()
     */
    public void initialise() throws InitialisationException
    {
        try
        {
            transformerPool = new StackObjectPool(new BasePoolableObjectFactory()
            {
                public Object makeObject() throws Exception
                {
                    StreamSource source = getStreamSource();
                    TransformerFactory factory = TransformerFactory.newInstance();
                    return factory.newTransformer(source);
                }
            }, Math.max(MIN_IDLE, maxIdleTransformers));
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
            Source sourceDoc = getXmlSource(src);
            if (sourceDoc == null) return null;

            ResultHolder holder = getResultHolder(returnClass);
            if (holder == null) holder = getResultHolder(src.getClass());

            DefaultErrorListener errorListener = new DefaultErrorListener(this);
            Transformer transformer = null;
            Object result;
            try
            {
                transformer = (Transformer)transformerPool.borrowObject();

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
                if (transformer != null) transformerPool.returnObject(transformer);
            }
            return result;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
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

    /**
     * Returns the StreamSource corresponding to xslFile
     * 
     * @return The StreamSource
     * @throws InitialisationException
     */
    private StreamSource getStreamSource() throws InitialisationException
    {
        if (xslt != null)
        {
            return new StreamSource(new StringReader(xslt));
        }

        if (xslFile == null)
        {
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "xslFile"), this);
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
            throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, xslFile), this);
        }
    }

    public Object clone() throws CloneNotSupportedException
    {
        XsltTransformer x = (XsltTransformer)super.clone();
        try
        {
            if (x.nextTransformer == null)
            {
                x.initialise();
            }
        }
        catch (Exception e)
        {
            throw new CloneNotSupportedException(e.getMessage());
        }
        return x;
    }

    private class DefaultErrorListener implements ErrorListener
    {
        private TransformerException e = null;

        private UMOTransformer trans;

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
            logger.error(exception.getMessage(), exception);
            e = new TransformerException(trans, exception);
        }

        public void fatalError(javax.xml.transform.TransformerException exception)
            throws javax.xml.transform.TransformerException
        {
            logger.fatal(exception.getMessage());
            e = new TransformerException(trans, exception);
        }

        public void warning(javax.xml.transform.TransformerException exception)
            throws javax.xml.transform.TransformerException
        {
            logger.warn(exception.getMessage());
        }
    }

    /**
     * @return The current maximum number of allowable idle transformer objects in
     *         the pool
     */
    public int getMaxIdleTransformers()
    {
        return maxIdleTransformers;
    }

    /**
     * Sets the the current maximum number of idle transformer objects allowed in the
     * pool
     * 
     * @param maxIdleTransformers New maximum size to set
     */
    public void setMaxIdleTransformers(int maxIdleTransformers)
    {
        this.maxIdleTransformers = maxIdleTransformers;
    }

    public String getXslt()
    {
        return xslt;
    }

    public void setXslt(String xslt)
    {
        this.xslt = xslt;
    }
}
