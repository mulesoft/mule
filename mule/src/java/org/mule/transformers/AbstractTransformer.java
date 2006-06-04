/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */
package org.mule.transformers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.NullPayload;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;

/**
 * <code>AbstractTransformer</code> Is a base class for all transformers.
 * Transformations transform one object into another.
 *
 * @author Ross Mason
 * @version $Revision$
 */

public abstract class AbstractTransformer implements UMOTransformer
{
    /**
     * The fully qualified class name of the fallback <code>Transformer</code>
     * implementation class to use, if no other can be found.
     */
    public static final String TRANSFORMER_DEFAULT = "org.mule.transformers.NoActionTransformer";

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected Class returnClass = null;

    protected String name = null;

    protected UMOImmutableEndpoint endpoint = null;

    private List sourceTypes = new ArrayList();

    /**
     * This is the following transformer in the chain of transformers.
     */
    protected UMOTransformer nextTransformer;

    private boolean ignoreBadInput = false;

    /**
     * default constructor required for discovery
     */
    public AbstractTransformer()
    {
        name = generateTransformerName();
    }

    protected Object checkReturnClass(Object object) throws TransformerException
    {
        if (returnClass != null) {
            if (!returnClass.isInstance(object)) {
                throw new TransformerException(new Message(Messages.TRANSFORM_X_UNEXPECTED_TYPE_X,
                                                           object.getClass().getName(),
                                                           returnClass.getName()), this);
            }
        }
        logger.debug("The transformed object is of expected type. Type is: " + object.getClass().getName());
        return object;
    }

    protected void registerSourceType(Class aClass)
    {
        if (aClass.equals(Object.class)) {
            logger.debug("java.lang.Object has been added as an acceptable sourcetype for this transformer, there will be no source type checking performed");
        }
        sourceTypes.add(aClass);
    }

    protected void unregisterSourceType(Class aClass)
    {
        sourceTypes.remove(aClass);
    }

    protected Iterator getSourceTypeClassesIterator()
    {
        return sourceTypes.iterator();
    }

    /**
     * @return transformer name
     */
    public String getName()
    {
        if (name == null) {
            setName(ClassUtils.getShortClassName(getClass()));
        }
        return name;
    }

    /**
     * @param string
     */
    public void setName(String string)
    {
        logger.debug("Setting transformer name to: " + name);
        name = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.transformers.Transformer#getReturnClass()
     */
    public Class getReturnClass()
    {
        return returnClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.transformers.Transformer#setReturnClass(java.lang.String)
     */
    public void setReturnClass(Class newClass)
    {
        returnClass = newClass;
    }

    public boolean isSourceTypeSupported(Class aClass)
    {
        return isSourceTypeSupported(aClass, false);
    }
    public boolean isSourceTypeSupported(Class aClass, boolean exactMatch)
    {
        if (sourceTypes.isEmpty()) {
            return !exactMatch;
        }
        Class anotherClass;
        for (Iterator i = getSourceTypeClassesIterator(); i.hasNext();) {
            anotherClass = (Class) i.next();
            if(exactMatch) {
                if (anotherClass.equals(aClass)) {
                    return true;
                }
            }else  if (anotherClass.isAssignableFrom(aClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Transforms the object.
     *
     * @param src The source object to transform.
     * @return The transformed object
     */
    public final Object transform(Object src) throws TransformerException
    {
        Object result;
        String encoding = null;
        if (src instanceof UMOMessage && !isSourceTypeSupported(UMOMessage.class)) {
            encoding = ((UMOMessage) src).getEncoding();
            src = ((UMOMessage) src).getPayload();
        }

        if (!isSourceTypeSupported(src.getClass())) {
            if(ignoreBadInput) {
                logger.debug("Source type is incompatible with this transformer. Property 'ignoreBadInput' is set to true so the transformer chain will continue");
                return src;
            } else {
                throw new TransformerException(new Message(Messages.TRANSFORM_X_UNSUPORTED_TYPE_X_ENDPOINT_X,
                                                       getName(),
                                                       src.getClass().getName(),
                                                       endpoint.getEndpointURI()), this);
            }
        } else {
            if (endpoint != null && endpoint.getEncoding()!=null) {
               encoding = endpoint.getEncoding();
            }
            if(encoding==null) {
                encoding = MuleManager.getConfiguration().getEncoding();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Applying transformer " + getName() + " (" + getClass().getName() + ")");
                logger.debug("Object before transform: " + StringMessageUtils.truncate(src.toString(), 200, false));
            }

            result = doTransform(src, encoding);
            if (result == null) {
                result = new NullPayload();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Object after transform: " + StringMessageUtils.truncate(result.toString(), 200, false));
            }

            result = checkReturnClass(result);
            if (nextTransformer != null) {
                logger.debug("Following transformer in the chain is " + nextTransformer.getName() + " (" + nextTransformer.getClass().getName() + ")");
                result = nextTransformer.transform(result);
            }
        }
        return result;
    }

    public UMOImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.transformer.UMOTransformer#setConnector(org.mule.umo.provider.UMOConnector)
     */
    public void setEndpoint(UMOImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
        UMOTransformer trans = nextTransformer;
        while (trans != null && endpoint != null) {
            trans.setEndpoint(endpoint);
            trans = trans.getNextTransformer();
        }

    }

    protected abstract Object doTransform(Object src, String encoding) throws TransformerException;

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.transformer.UMOTransformer#getNextTransformer()
     */
    public UMOTransformer getNextTransformer()
    {
        return nextTransformer;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.transformer.UMOTransformer#setNextTransformer(org.mule.umo.transformer.UMOTransformer)
     */
    public void setNextTransformer(UMOTransformer nextTransformer)
    {
        this.nextTransformer = nextTransformer;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException
    {
        try {
            Object c = BeanUtils.cloneBean(this);
            return c;
        } catch (Exception e) {
            throw new CloneNotSupportedException("Failed to clone transformer: " + e.getMessage());
        }
    }

    /**
     * Will return the return type for the last transformer in the chain
     *
     * @return the last transformers return type
     */
    public Class getFinalReturnClass()
    {
        UMOTransformer tempTrans = this;
        UMOTransformer returnTrans = this;
        while (tempTrans != null) {
            returnTrans = tempTrans;
            tempTrans = tempTrans.getNextTransformer();
        }
        return returnTrans.getReturnClass();
    }

    /**
     * Template method were deriving classes can do any initialisation after the
     * properties have been set on this transformer
     *
     * @throws InitialisationException
     */
    public void initialise() throws InitialisationException
    {
        // nothing to do
    }

    protected String generateTransformerName()
    {
        String name = getClass().getName();
        int i = name.lastIndexOf(".");
        if (i > -1) {
            name = name.substring(i + 1);
        }
        return name;
    }

    /**
     * Convenience method to register source types using a bean property setter
     *
     * @param type the fully qualified class name
     * @throws ClassNotFoundException is thrown if the class is not on
     *             theclasspath
     */
    public void setSourceType(String type) throws ClassNotFoundException
    {
        Class clazz = ClassUtils.loadClass(type, getClass());
        registerSourceType(clazz);
    }

    public boolean isIgnoreBadInput() {
        return ignoreBadInput;
    }

    public void setIgnoreBadInput(boolean ignoreBadInput) {
        this.ignoreBadInput = ignoreBadInput;
    }

    public String toString()
    {
        return "Transformer{" + "name='" + name + "'" + ", returnClass=" + ignoreBadInput + ", returnClass=" +
                ignoreBadInput + ", sourceTypes=" + sourceTypes + "}";
    }

    public boolean isAcceptNull() {
        return false;
    }
}
