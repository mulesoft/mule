/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.xml.i18n.XmlMessages;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import net.sf.saxon.Configuration;
import net.sf.saxon.javax.xml.xquery.XQCommonHandler;
import net.sf.saxon.javax.xml.xquery.XQConnection;
import net.sf.saxon.javax.xml.xquery.XQDataSource;
import net.sf.saxon.javax.xml.xquery.XQException;
import net.sf.saxon.javax.xml.xquery.XQItem;
import net.sf.saxon.javax.xml.xquery.XQItemType;
import net.sf.saxon.javax.xml.xquery.XQPreparedExpression;
import net.sf.saxon.javax.xml.xquery.XQResultSequence;
import net.sf.saxon.xqj.SaxonXQDataSource;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.dom4j.DocumentException;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.DocumentSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * The XQuery Module gives users the ability to perform XQuery transformations on XML messages in Mule
 */
public class XQueryTransformer extends AbstractXmlTransformer implements Disposable
{
    public static final String SOURCE_DOCUMENT_NAMESPACE = "document";

    // keep at least 1 XSLT Transformer ready by default
    private static final int MIN_IDLE_TRANSFORMERS = 1;
    // keep max. 32 XSLT Transformers around by default
    private static final int MAX_IDLE_TRANSFORMERS = 32;
    // MAX_IDLE is also the total limit
    private static final int MAX_ACTIVE_TRANSFORMERS = MAX_IDLE_TRANSFORMERS;

    protected final GenericObjectPool transformerPool;

    private volatile String xqueryFile;
    private volatile String xquery;
    private volatile Map contextProperties;
    private volatile XQCommonHandler commonHandler;
    private volatile XQConnection connection;
    protected Configuration configuration;

    public XQueryTransformer()
    {
        super();
        transformerPool = new GenericObjectPool(new PooledXQueryTransformerFactory());
        transformerPool.setMinIdle(MIN_IDLE_TRANSFORMERS);
        transformerPool.setMaxIdle(MAX_IDLE_TRANSFORMERS);
        transformerPool.setMaxActive(MAX_ACTIVE_TRANSFORMERS);

        registerSourceType(String.class);
        registerSourceType(byte[].class);
        registerSourceType(DocumentSource.class);
        registerSourceType(org.dom4j.Document.class);
        registerSourceType(Document.class);
        registerSourceType(Element.class);
        registerSourceType(InputStream.class);
        setReturnClass(Element.class);
    }

    public XQueryTransformer(String xqueryFile)
    {
        this();
        this.xqueryFile = xqueryFile;
    }

    /**
     *
     */
    public void initialise() throws InitialisationException
    {

        if(configuration==null)
        {
            configuration = new Configuration();
        }
        try
        {
            XQDataSource ds = new SaxonXQDataSource(configuration);
            if (commonHandler != null)
            {
                ds.setCommonHandler(commonHandler);
            }
            connection = ds.getConnection();

            transformerPool.addObject();

        }
        catch (Throwable te)
        {
            throw new InitialisationException(te, this);
        }
    }

    public void dispose()
    {
        try
        {
            connection.close();
        }
        catch (XQException e)
        {
            logger.warn(e.getMessage());
        }
    }

    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
    {
        try
        {
            XQPreparedExpression transformer = null;
            try
            {
                transformer = (XQPreparedExpression) transformerPool.borrowObject();

                bindParameters(transformer, message);

                bindDocument(message.getPayload(), transformer);

                XQResultSequence result = transformer.executeQuery();
                //No support for return Arrays yet
                List results = new ArrayList();
                while (result.next())
                {
                    XQItem item = result.getItem();
     
                    if(Node.class.isAssignableFrom(returnClass) || Node[].class.isAssignableFrom(returnClass))
                    {
                        results.add(item.getNode());
                    }
                    else if(String.class.isAssignableFrom(returnClass) || String[].class.isAssignableFrom(returnClass))
                    {
                        results.add(item.getItemAsString());
                    }
                    else if(XMLStreamReader.class.isAssignableFrom(returnClass) || XMLStreamReader[].class.isAssignableFrom(returnClass))
                    {
                        try
                        {
                            results.add(item.getItemAsStream());
                        }
                        catch (XQException e)
                        {
                            throw new TransformerException(XmlMessages.streamNotAvailble(getName()));
                        }
                    }
                    else
                    {
                        //This can be a JAXB bound  object instance depending on whether the CommonHandler has been set
                        try
                        {
                            results.add(item.getObject());
                        }
                        catch (XQException e)
                        {
                            throw new TransformerException(XmlMessages.objectNotAvailble(getName()));

                        }
                    }
                    if(!returnClass.isArray())
                    {
                        break;
                    }
                }
                if(returnClass.isArray())
                {
                    return results.toArray();
                }
                if(results.size()==1)
                {
                    return results.get(0);
                }
                else if(results.size()==0)
                {
                    return null;
                }
                else
                {
                    return results.toArray();
                }

            }
            finally
            {
                if (transformer != null)
                {
                    if(transformer.getWarnings()!=null)
                    {
                        logger.warn(transformer.getWarnings().getMessage(), transformer.getWarnings().fillInStackTrace());
                    }
                    // clear transformation parameters before returning transformer to the
                    // pool
                    //TODO find out what the scope is for bound variables, there doesn't seem to be a way to unbind them

                    transformerPool.returnObject(transformer);
                }
            }

        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected void bindParameters(XQPreparedExpression transformer, MuleMessage message) throws XQException, TransformerException
    {
        // set transformation parameters
        if (contextProperties != null)
        {
            for (Iterator i = contextProperties.entrySet().iterator(); i.hasNext();)
            {
                Map.Entry parameter = (Map.Entry) i.next();
                String key = (String) parameter.getKey();
                Object o = evaluateTransformParameter(key, parameter.getValue(), message);

                if (o instanceof String)
                {
                    transformer.bindAtomicValue(new QName(key), o.toString(), connection.createAtomicItemType(XQItemType.XQBASETYPE_STRING));
                }
                else if (o instanceof Boolean)
                {
                    transformer.bindBoolean(new QName(key), ((Boolean) o).booleanValue(), connection.createAtomicItemType(XQItemType.XQBASETYPE_BOOLEAN));
                }
                else if (o instanceof Byte)
                {
                    transformer.bindByte(new QName(key), ((Byte) o).byteValue(), connection.createAtomicItemType(XQItemType.XQBASETYPE_BYTE));
                }
                else if (o instanceof Short)
                {
                    transformer.bindShort(new QName(key), ((Short) o).shortValue(), connection.createAtomicItemType(XQItemType.XQBASETYPE_SHORT));
                }
                else if (o instanceof Integer)
                {
                    transformer.bindInt(new QName(key), ((Integer) o).intValue(), connection.createAtomicItemType(XQItemType.XQBASETYPE_INT));
                }
                else if (o instanceof Long)
                {
                    transformer.bindLong(new QName(key), ((Long) o).longValue(), connection.createAtomicItemType(XQItemType.XQBASETYPE_LONG));
                }
                else if (o instanceof Float)
                {
                    transformer.bindFloat(new QName(key), ((Float) o).floatValue(), connection.createAtomicItemType(XQItemType.XQBASETYPE_FLOAT));
                }
                else if (o instanceof Double)
                {
                    transformer.bindDouble(new QName(key), ((Double) o).doubleValue(), connection.createAtomicItemType(XQItemType.XQBASETYPE_DOUBLE));
                }
                else
                {
                    logger.error("Cannot bind value: " + o + " cannot be bound to the Xquery context. Not of supported type");
                }
            }
        }
    }

    /**
     * Returns the InputSource corresponding to xqueryFile or xquery
     *
     * @param src
     * @param transformer
     * @throws net.sf.saxon.javax.xml.xquery.XQException
     * @throws org.mule.umo.transformer.TransformerException
     */
    protected void bindDocument(Object src, XQPreparedExpression transformer) throws XQException, TransformerException
    {
        if (src instanceof byte[])
        {
            transformer.bindDocument(new QName(SOURCE_DOCUMENT_NAMESPACE), new InputSource(new ByteArrayInputStream((byte[]) src)));

        }
        else if (src instanceof InputStream)
        {
            transformer.bindDocument(new QName(SOURCE_DOCUMENT_NAMESPACE), new InputSource((InputStream) src));

        }
        else if (src instanceof String)
        {
            transformer.bindDocument(new QName(SOURCE_DOCUMENT_NAMESPACE), new InputSource(new StringReader((String) src)));

        }
        else if (src instanceof Document)
        {
            transformer.bindNode(new QName(SOURCE_DOCUMENT_NAMESPACE), (Document) src, null);

        }
        else if (src instanceof Element)
        {
            transformer.bindNode(new QName(SOURCE_DOCUMENT_NAMESPACE), (Element) src, null);

        }
        else if (src instanceof org.dom4j.Document)
        {
            try
            {
                DOMWriter domWriter = new DOMWriter();
                Document dom = domWriter.write((org.dom4j.Document)src);
                transformer.bindNode(new QName(SOURCE_DOCUMENT_NAMESPACE), dom, null);
            }
            catch (DocumentException e)
            {
                throw new TransformerException(this, e);
            }

        }
        else if (src instanceof DocumentSource)
        {
            transformer.bindDocument(new QName(SOURCE_DOCUMENT_NAMESPACE), ((DocumentSource)src).getInputSource());

        }
        else
        {
            throw new TransformerException(CoreMessages.transformUnexpectedType(src.getClass(), null), this);
        }
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }

    /**
     * @return Returns the xqueryFile.
     */
    public String getXqueryFile()
    {
        return xqueryFile;
    }

    /**
     * @param xqueryFile The xqueryFile to set.
     */
    public void setXqueryFile(String xqueryFile)
    {
        this.xqueryFile = xqueryFile;
    }

    public String getXquery()
    {
        return xquery;
    }

    public void setXquery(String xquery)
    {
        this.xquery = xquery;
    }

    public XQCommonHandler getCommonHandler()
    {
        return commonHandler;
    }

    public void setCommonHandler(XQCommonHandler commonHandler)
    {
        this.commonHandler = commonHandler;
    }


    protected class PooledXQueryTransformerFactory extends BasePoolableObjectFactory
    {
        public Object makeObject() throws Exception
        {
            if (xqueryFile != null)
            {
                xquery = IOUtils.getResourceAsString(xqueryFile, getClass());
            }

            return connection.prepareExpression(xquery);
        }

        @Override
        public void destroyObject(Object o) throws Exception
        {
            ((XQPreparedExpression) o).close();
            super.destroyObject(o);
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
     * @return The current maximum number of allowable idle transformer objects in the
     *         pool
     */
    public int getMaxIdleTransformers()
    {
        return transformerPool.getMaxIdle();
    }

    /**
     * Sets the the current maximum number of idle transformer objects allowed in the pool
     *
     * @param maxIdleTransformers New maximum size to set
     */
    public void setMaxIdleTransformers(int maxIdleTransformers)
    {
        transformerPool.setMaxIdle(maxIdleTransformers);
    }

    /**
     * Gets the parameters to be used when applying the transformation
     *
     * @return a map of the parameter names and associated values
     * @see javax.xml.transform.Transformer#setParameter(java.lang.String,
     *      java.lang.Object)
     */
    public Map getContextProperties()
    {
        return contextProperties;
    }

    /**
     * Sets the parameters to be used when applying the transformation
     *
     * @param contextProperties a map of the parameter names and associated values
     * @see javax.xml.transform.Transformer#setParameter(java.lang.String,
     *      java.lang.Object)
     */
    public void setContextProperties(Map contextProperties)
    {
        this.contextProperties = contextProperties;
    }

    /**
     * <p>
     * Returns the value to be set for the parameter. This method is called for each
     * parameter before it is set on the transformer. The purpose of this method is to
     * allow dynamic parameters related to the event (usually message properties) to be
     * used. Any expression using the Mule expression syntax can be used.
     * </p>
     * <p>
     * For example: If the current event's message has a property named "myproperty", to
     * pass this in you would set the transform parameter's value to be
     * "#[mule.message:header(myproperty)]".
     *
     * <p>
     * This method may be overloaded by a sub class to provide a different dynamic
     * parameter implementation.
     * </p>
     *
     * @param name  the name of the parameter
     * @param value the value of the paramter
     * @return the object to be set as the parameter value
     * @throws TransformerException
     */
    protected Object evaluateTransformParameter(String name, Object value, MuleMessage message) throws TransformerException
    {
        if (value instanceof String)
        {
            return muleContext.getExpressionManager().parse(value.toString(), message);
        }
        return value;
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        Object clone = super.clone();
        try
        {
            ((Initialisable)clone).initialise();
            return clone;
        }
        catch (InitialisationException e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToClone(getClass().getName()), e);
        }
    }
}
