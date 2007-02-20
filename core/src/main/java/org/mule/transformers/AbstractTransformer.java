/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers;

import org.mule.RegistryContext;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.config.spring.RegistryFacade;
import org.mule.providers.NullPayload;
import org.mule.registry.DeregistrationException;
import org.mule.registry.RegistrationException;
import org.mule.registry.metadata.ObjectMetadata;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Registerable;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;
import org.mule.util.StringMessageUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractTransformer</code> is a base class for all transformers.
 * Transformations transform one object into another.
 */

public abstract class AbstractTransformer implements UMOTransformer
{
    public static ObjectMetadata objectMetadata = new ObjectMetadata(new String[] { "returnClass", "sourceTypes" });

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(getClass());

    /**
     * The return type that will be returned by the {@link #transform} method is
     * called
     */
    protected Class returnClass = null;

    /**
     * The name that identifies this transformer. If none is set the class name of
     * the transformer is used
     */
    protected String name = null;

    /**
     * The endpoint that this transformer instance is configured on
     */
    protected UMOImmutableEndpoint endpoint = null;

    /**
     * A list of support Class types that the source payload passed into this
     * transformer
     */
    protected List sourceTypes = new ArrayList();

    /**
     * This is the following transformer in the chain of transformers.
     */
    protected UMOTransformer nextTransformer;

    /**
     * Determines whether the transformer will throw an exception if the message
     * passed is is not supported or the return tye is incorrect
     */
    private boolean ignoreBadInput = false;

    /**
     * Registry ID
     */
    protected String registryId = null;

    /**
     * default constructor required for discovery
     */
    public AbstractTransformer()
    {
        name = generateTransformerName();
    }

    protected Object checkReturnClass(Object object) throws TransformerException
    {
        if (returnClass != null)
        {
            if (!returnClass.isInstance(object))
            {
                throw new TransformerException(new Message(Messages.TRANSFORM_X_UNEXPECTED_TYPE_X, object
                    .getClass().getName(), returnClass.getName()), this);
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("The transformed object is of expected type. Type is: "
                            + object.getClass().getName());
        }

        return object;
    }

    protected synchronized void registerSourceType(Class aClass)
    {
        if (aClass.equals(Object.class))
        {
            logger.debug("java.lang.Object has been added as an acceptable sourcetype for this transformer, there will be no source type checking performed");
        }

        sourceTypes.add(aClass);
    }

    protected synchronized void unregisterSourceType(Class aClass)
    {
        sourceTypes.remove(aClass);
    }

    /**
     * @return transformer name
     */
    public String getName()
    {
        if (name == null)
        {
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
        int numTypes = sourceTypes.size();

        if (numTypes == 0)
        {
            return !exactMatch;
        }

        for (int i = 0; i < numTypes; i++)
        {
            Class anotherClass = (Class)sourceTypes.get(i);
            if (exactMatch)
            {
                if (anotherClass.equals(aClass))
                {
                    return true;
                }
            }
            else if (anotherClass.isAssignableFrom(aClass))
            {
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
        String encoding = null;

        if (src instanceof UMOMessage && !isSourceTypeSupported(UMOMessage.class))
        {
            encoding = ((UMOMessage)src).getEncoding();
            src = ((UMOMessage)src).getPayload();
        }

        if (encoding == null && endpoint != null)
        {
            encoding = endpoint.getEncoding();
        }
        else if (encoding == null)
        {
            encoding = FileUtils.DEFAULT_ENCODING;
        }

        if (!isSourceTypeSupported(src.getClass()))
        {
            if (ignoreBadInput)
            {
                logger.debug("Source type is incompatible with this transformer and property 'ignoreBadInput' is set to true, so the transformer chain will continue.");
                return src;
            }
            else
            {
                throw new TransformerException(new Message(Messages.TRANSFORM_X_UNSUPORTED_TYPE_X_ENDPOINT_X,
                    getName(), src.getClass().getName(),
                    (endpoint != null ? endpoint.getEndpointURI() : null)), this);
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Applying transformer " + getName() + " (" + getClass().getName() + ")");
            logger.debug("Object before transform: "
                            + StringMessageUtils.truncate(StringMessageUtils.toString(src), 200, false));
        }

        Object result = doTransform(src, encoding);
        if (result == null)
        {
            result = new NullPayload();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Object after transform: "
                            + StringMessageUtils.truncate(StringMessageUtils.toString(result), 200, false));
        }

        result = checkReturnClass(result);

        if (nextTransformer != null)
        {
            logger.debug("Following transformer in the chain is " + nextTransformer.getName() + " ("
                            + nextTransformer.getClass().getName() + ")");
            result = nextTransformer.transform(result);
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
        while (trans != null && endpoint != null)
        {
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

    /**
     * Will return the return type for the last transformer in the chain
     * 
     * @return the last transformers return type
     */
    public Class getFinalReturnClass()
    {
        UMOTransformer tempTrans = this;
        UMOTransformer returnTrans = this;
        while (tempTrans != null)
        {
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
     * @param managementContext
     */
    public void initialise(UMOManagementContext managementContext) throws InitialisationException
    {
        // nothing to do
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#register()
     */
    public void register() throws RegistrationException
    {
        RegistryFacade registry = RegistryContext.getRegistry();
        if (registry == null) throw new RegistrationException("No registry available");
        Registerable parent = null;
        if (endpoint != null)
        {
            parent = endpoint;
        }
        else 
        {
            //TODO LM: what should the parent be
            parent = null;
        }

        registryId = registry.registerMuleObject(parent, this).getId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#deregister()
     */
    public void deregister() throws DeregistrationException
    {
        RegistryContext.getRegistry().deregisterComponent(registryId);
        registryId = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#getRegistryId()
     */
    public String getRegistryId()
    {
        return registryId;
    }

    protected String generateTransformerName()
    {
        String name = getClass().getName();
        int i = name.lastIndexOf(".");
        if (i > -1)
        {
            name = name.substring(i + 1);
        }
        return name;
    }

    /**
     * Convenience method to register source types using a bean property setter
     * 
     * @param type the fully qualified class name
     * @throws ClassNotFoundException is thrown if the class is not on theclasspath
     * @deprecated Use setSourceTypes(List)
     */
    public void setSourceType(String type) throws ClassNotFoundException
    {
        Class clazz = ClassUtils.loadClass(type, getClass());
        registerSourceType(clazz);
    }

    /**
     * Register a list of source types
     *
     * @param types a list of fully qualified class names
     * @throws ClassNotFoundException is thrown if the class is not on theclasspath
     * @deprecated Use setSourceTypes(List)
     */
    public void setSourceTypes(List types) throws ClassNotFoundException
    {
        for (Iterator iterator = types.iterator(); iterator.hasNext();)
        {
             Class clazz = ClassUtils.loadClass( iterator.next().toString(), getClass());
            registerSourceType(clazz);
        }

    }
    /**
     * Where multiple source types are listed, this method only returns the first
     * one. The full list of supported source types can also be obtained using
     * <code>getSourceTypesIterator()</code>
     * 
     * @return the first SourceType on the transformer or java.lang.Object if there
     *         is no source type set
     */
    public String getSourceType()
    {
        Class c = null;
        if (sourceTypes.size() > 0)
        {
            c = (Class)sourceTypes.get(0);
        }
        if (c == null)
        {
            c = Object.class;
        }
        return c.getName();
    }

    public List getSourceTypes()
    {
        return sourceTypes;
    }

    public boolean isIgnoreBadInput()
    {
        return ignoreBadInput;
    }

    public void setIgnoreBadInput(boolean ignoreBadInput)
    {
        this.ignoreBadInput = ignoreBadInput;
    }

    public String toString()
    {
        return "Transformer{" + "name='" + name + "'" + ", ignoreBadInput=" + ignoreBadInput
                        + ", returnClass=" + returnClass + ", sourceTypes=" + sourceTypes + "}";
    }

    public boolean isAcceptNull()
    {
        return false;
    }

}
