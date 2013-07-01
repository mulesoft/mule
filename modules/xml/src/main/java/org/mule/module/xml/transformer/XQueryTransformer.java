/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItem;
import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQResultSequence;

import net.sf.saxon.Configuration;
import net.sf.saxon.xqj.SaxonXQDataSource;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.DocumentSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
    private volatile Map<String, Object> contextProperties;
    private volatile XQConnection connection;
    protected Configuration configuration;

    public XQueryTransformer()
    {
        super();
        transformerPool = new GenericObjectPool(new PooledXQueryTransformerFactory());
        transformerPool.setMinIdle(MIN_IDLE_TRANSFORMERS);
        transformerPool.setMaxIdle(MAX_IDLE_TRANSFORMERS);
        transformerPool.setMaxActive(MAX_ACTIVE_TRANSFORMERS);

        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.create(DocumentSource.class));
        registerSourceType(DataTypeFactory.create(org.dom4j.Document.class));
        registerSourceType(DataTypeFactory.create(Document.class));
        registerSourceType(DataTypeFactory.create(Element.class));
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        setReturnDataType(DataTypeFactory.create(Element.class));
    }

    public XQueryTransformer(String xqueryFile)
    {
        this();
        this.xqueryFile = xqueryFile;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (xquery != null && xqueryFile != null)
        {
            throw new InitialisationException(XmlMessages.canOnlySetFileOrXQuery(), this);
        }

        try
        {
            if (xqueryFile != null)
            {
                xquery = IOUtils.getResourceAsString(xqueryFile, getClass());
            }
            if (configuration == null)
            {
                configuration = new Configuration();
            }

            XQDataSource ds = new SaxonXQDataSource(configuration);
            connection = ds.getConnection();

            transformerPool.addObject();

        }
        catch (Throwable te)
        {
            throw new InitialisationException(te, this);
        }
    }

    @Override
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

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
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
                List<Object> results = new ArrayList<Object>();
                while (result.next())
                {
                    XQItem item = result.getItem();

                    Class<?> type = returnType.getType();
                    if (Node.class.isAssignableFrom(type) || Node[].class.isAssignableFrom(type))
                    {
                        results.add(item.getNode());
                    }
                    else if (String.class.isAssignableFrom(type) || String[].class.isAssignableFrom(type))
                    {
                        results.add(item.getItemAsString(null));
                    }
                    else if (XMLStreamReader.class.isAssignableFrom(type) || XMLStreamReader[].class.isAssignableFrom(type))
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
                    if (!type.isArray())
                    {
                        break;
                    }
                }
                if (returnType.getType().isArray())
                {
                    return results.toArray();
                }
                if (results.size() == 1)
                {
                    return results.get(0);
                }
                else if (results.size() == 0)
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
                    // clear transformation parameters before returning transformer to the
                    // pool
                    //TODO find out what the scope is for bound variables, there doesn't seem to be a way to unbind them
                    unbindParameters(transformer);
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
            for (Map.Entry<String, Object> parameter : contextProperties.entrySet())
            {
                String key = parameter.getKey();
                Object o = evaluateTransformParameter(key, parameter.getValue(), message);

                if (o instanceof String)
                {
                    transformer.bindAtomicValue(new QName(key), o.toString(), connection.createAtomicType(XQItemType.XQBASETYPE_STRING));
                }
                else if (o instanceof Boolean)
                {
                    transformer.bindBoolean(new QName(key), ((Boolean) o).booleanValue(), connection.createAtomicType(XQItemType.XQBASETYPE_BOOLEAN));
                }
                else if (o instanceof Byte)
                {
                    transformer.bindByte(new QName(key), ((Byte) o).byteValue(), connection.createAtomicType(XQItemType.XQBASETYPE_BYTE));
                }
                else if (o instanceof Short)
                {
                    transformer.bindShort(new QName(key), ((Short) o).shortValue(), connection.createAtomicType(XQItemType.XQBASETYPE_SHORT));
                }
                else if (o instanceof Integer)
                {
                    transformer.bindInt(new QName(key), ((Integer) o).intValue(), connection.createAtomicType(XQItemType.XQBASETYPE_INT));
                }
                else if (o instanceof Long)
                {
                    transformer.bindLong(new QName(key), ((Long) o).longValue(), connection.createAtomicType(XQItemType.XQBASETYPE_LONG));
                }
                else if (o instanceof Float)
                {
                    transformer.bindFloat(new QName(key), ((Float) o).floatValue(), connection.createAtomicType(XQItemType.XQBASETYPE_FLOAT));
                }
                else if (o instanceof Double)
                {
                    transformer.bindDouble(new QName(key), ((Double) o).doubleValue(), connection.createAtomicType(XQItemType.XQBASETYPE_DOUBLE));
                }
                else
                {
                    logger.error("Cannot bind value: " + o + " cannot be bound to the Xquery context. Not of supported type");
                }
            }
        }
    }

    /**
     * Removes any parameter bindings from the transformer, replacing them with empty strings
     *
     * @param transformer the transformer to remove properties from
     */
    protected void unbindParameters(XQPreparedExpression transformer) throws XQException
    {
        // Replace transformation parameters with null values
        if (contextProperties != null)
        {
            for (Map.Entry<String, Object> parameter: contextProperties.entrySet())
            {
                String key = parameter.getKey();
                transformer.bindAtomicValue(new QName(key), "", connection.createAtomicType(XQItemType.XQBASETYPE_STRING));
            }
        }
    }

    /**
     * Returns the InputSource corresponding to xqueryFile or xquery
     *
     * @param src
     * @param transformer
     * @throws XQException
     *
     * @throws Exception
     *
     */
    protected void bindDocument(Object src, XQPreparedExpression transformer) throws Exception
    {
        if (src instanceof byte[])
        {
            transformer.bindDocument(new QName(SOURCE_DOCUMENT_NAMESPACE), new StreamSource(new ByteArrayInputStream((byte[]) src)), connection.createDocumentType());
        }
        else if (src instanceof InputStream)
        {
            transformer.bindDocument(new QName(SOURCE_DOCUMENT_NAMESPACE), new StreamSource((InputStream) src), connection.createDocumentType());
        }
        else if (src instanceof String)
        {
            transformer.bindDocument(new QName(SOURCE_DOCUMENT_NAMESPACE), new StreamSource(new ByteArrayInputStream(((String) src).getBytes())),connection.createDocumentType());
        }
        else if (src instanceof Document)
        {
            transformer.bindNode(new QName(SOURCE_DOCUMENT_NAMESPACE), (Document) src, connection.createDocumentType());
        }
        else if (src instanceof Element)
        {
            transformer.bindNode(new QName(SOURCE_DOCUMENT_NAMESPACE), (Element) src, connection.createDocumentType());
        }
        else if (src instanceof org.dom4j.Document)
        {
            DOMWriter domWriter = new DOMWriter();
            Document dom = domWriter.write((org.dom4j.Document) src);
            transformer.bindNode(new QName(SOURCE_DOCUMENT_NAMESPACE), dom, connection.createDocumentType());
        }
        else if (src instanceof DocumentSource)
        {
            transformer.bindDocument(new QName(SOURCE_DOCUMENT_NAMESPACE), ((DocumentSource) src), null);
        }
        else
        {
            throw new IllegalArgumentException(CoreMessages.transformUnexpectedType(src.getClass(), null).getMessage());
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

    protected class PooledXQueryTransformerFactory extends BasePoolableObjectFactory
    {
        @Override
        public Object makeObject() throws Exception
        {
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
    public Map<String, Object> getContextProperties()
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
    public void setContextProperties(Map<String, Object> contextProperties)
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
     * <p/>
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
            ((Initialisable) clone).initialise();
            return clone;
        }
        catch (InitialisationException e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToClone(getClass().getName()), e);
        }
    }
}
