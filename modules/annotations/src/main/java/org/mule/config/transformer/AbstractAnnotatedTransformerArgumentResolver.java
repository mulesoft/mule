/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.transformer;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.transformer.DataType;
import org.mule.transformer.types.CollectionDataType;
import org.mule.util.annotation.AnnotationUtils;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An abstract resolver that can be extend to resolve/create an object based on Annotated classes. These classes can be
 * scanned and used to create the context.  Typically this class will be used to create resolvers for binding frameworks
 * where the data type classes are annotated with binding information.
 */
public abstract class AbstractAnnotatedTransformerArgumentResolver implements TransformerArgumentResolver, Disposable
{
    public static final String[] ignoredPackages = {"java.", "javax.", "org.w3c.", "org.mule.transport.", "org.mule.module."};

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(getClass());

    //We cache the Json classes so we don't scan them each time a context is needed
    private Set<Class> matchingClasses = new CopyOnWriteArraySet<Class>();

    //We also cache the classes that did not match so that we dont scan them again either
    private Set<Class> nonMatchingClasses = new CopyOnWriteArraySet<Class>();

    public <T> T resolve(Class<T> type, DataType source, DataType result, MuleContext context) throws Exception
    {
        //Lets make sure this is the right resolver for the object type and that we haven't scanned these before
        //and determined they are not Json classes
        if(!getArgumentClass().isAssignableFrom(type) || isNonMatching(source, result))
        {
            return null;
        }

        Class annotatedType = (result instanceof CollectionDataType ? ((CollectionDataType)result).getItemType() : result.getType());

        //Check the cache first
        boolean isAnnotated = matchingClasses.contains(annotatedType);
        if(!isAnnotated)
        {
            //then scan the class for annotations
            isAnnotated = findAnnotation(annotatedType);
        }

        if (!isAnnotated)
        {
            annotatedType = source.getType();
            //Check the cache first
            isAnnotated = matchingClasses.contains(annotatedType);
            if(!isAnnotated)
            {
                //then scan the class for annotations
                isAnnotated = AnnotationUtils.hasAnnotationWithPackage(getAnnotationsPackageName(), annotatedType);
            }
        }

        Object argument = context.getRegistry().lookupObject(getArgumentClass());

        if (!isAnnotated)
        {
            //We didn't find Json annotations anywhere, lets cache the classes so we don't need to scan again
            nonMatchingClasses.add(source.getType());
            nonMatchingClasses.add(result.getType());

            //Finally if there is an Object Mapper configured we should return it
            return (T)argument;
        }
        else
        {
            matchingClasses.add(annotatedType);
        }


        if (argument == null)
        {
            logger.info("No common Object of type '" + getArgumentClass() + "' configured, creating a local one for: " + source + ", " + result);
            argument = createArgument(annotatedType, context);
        }
        return (T)argument;

    }

    protected boolean findAnnotation(Class annotatedType) throws IOException
    {
        
        if(annotatedType.getPackage()==null)
        {
            return false;
        }

        for (String ignoredPackage : ignoredPackages)
        {
            if(annotatedType.getPackage().getName().startsWith(ignoredPackage))
            {
                return false;
            }
        }
        return AnnotationUtils.hasAnnotationWithPackage(getAnnotationsPackageName(), annotatedType);
    }

    protected boolean isNonMatching(DataType source, DataType result)
    {
        return nonMatchingClasses.contains(result.getType()) && nonMatchingClasses.contains(source.getType());
    }

    public void dispose()
    {
        nonMatchingClasses.clear();
        matchingClasses.clear();
    }

    public Set<Class> getMatchingClasses()
    {
        return matchingClasses;
    }

    public Set<Class> getNonMatchingClasses()
    {
        return nonMatchingClasses;
    }

    /**
     * The object type that this resolver will discover or create
     * @return  The object type that this resolver will discover or create
     */
    protected abstract Class<?> getArgumentClass();

    /**
     * If the resolver cannot locate the required object of type {@link #getArgumentClass()} this method will be invoked
     * an instance of the object.
     *
     * @param annotatedType the annotated object that was matched
     * @param muleContext the current Mule context.
     * @return a new instance of the object being resolved.  This method may also retain a shared instance and possible add
     * configuration to the instance based on this invocation
     * @throws Exception if the object cannot be created
     */
    protected abstract Object createArgument(Class<?> annotatedType, MuleContext muleContext) throws Exception;

    /**
     * This resolver scans a class for annotations in this package.  Note this behaviour can be changed by overloading
     * the {@link #findAnnotation(Class)} method and search based on your own criteria
     * @return the package of the annotation(s) to scan for
     */
    protected abstract String getAnnotationsPackageName();
}
