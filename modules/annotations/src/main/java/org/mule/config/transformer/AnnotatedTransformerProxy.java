/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.annotations.param.Payload;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.expression.ExpressionAnnotationsHelper;
import org.mule.config.i18n.AnnotationsMessages;
import org.mule.expression.transformers.ExpressionTransformer;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;
import org.mule.util.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Creates a Mule {@link org.mule.api.transformer.Transformer} proxy around a transform method. The
 * transformer will be given a generated name which is the short name of the class and the method name
 * separated with a '.' i.e. 'MyTransformers.fooToBar'
 */
public class AnnotatedTransformerProxy extends AbstractMessageTransformer implements DiscoverableTransformer
{
    private int weighting;

    private Object proxy;

    private Method transformMethod;
    private boolean messageAware = false;
    private ExpressionTransformer paramTransformer = null;
    private Collection<TransformerArgumentResolver> resolvers;
    private Map<Class<?>, Object> cachedObjects = new WeakHashMap<Class<?>, Object>();
    private boolean sourceAnnotated = false;

    public AnnotatedTransformerProxy(int weighting, Object proxy, Method transformMethod, Class[] additionalSourceTypes, String sourceMimeType, String resultMimeType) throws TransformerException, InitialisationException
    {
        this.weighting = weighting;
        this.proxy = proxy;

        //By default we allow a transformer to return null
        setAllowNullReturn(true);

        validateMethod(transformMethod, additionalSourceTypes);

        this.transformMethod = transformMethod;
        setReturnDataType(DataTypeFactory.createFromReturnType(transformMethod, resultMimeType));

        messageAware = MuleMessage.class.isAssignableFrom(transformMethod.getParameterTypes()[0]);
        this.transformMethod = transformMethod;
        if (additionalSourceTypes.length > 0)
        {
            if (messageAware)
            {
                logger.error("Transformer: " + getName() + " is MuleMessage aware, this means additional source types configured on the transformer will be ignored. Source types are: " + Arrays.toString(additionalSourceTypes));
            }
            else
            {
                for (int i = 0; i < additionalSourceTypes.length; i++)
                {
                    registerSourceType(new SimpleDataType(additionalSourceTypes[i], sourceMimeType));

                }
            }
        }
        //The first Param is the always the object to transform
        Class<?> source = transformMethod.getParameterTypes()[0];
        registerSourceType(DataTypeFactory.create(source, sourceMimeType));
        sourceAnnotated = (transformMethod.getParameterAnnotations()[0].length > 0 &&
                transformMethod.getParameterAnnotations()[0][0] instanceof Payload);

        setName(proxy.getClass().getSimpleName() + "." + transformMethod.getName());
    }

    protected void validateMethod(Method method, Class[] sourceTypes) throws IllegalArgumentException
    {
        int mods = method.getModifiers();
        if(Modifier.isAbstract(mods) || Modifier.isInterface(mods) || !Modifier.isPublic(mods)
                || method.getReturnType().equals(Void.TYPE) || method.getParameterTypes().length==0 ||
                method.getReturnType().equals(Object.class) || Arrays.asList(method.getParameterTypes()).contains(Object.class) ||
                Arrays.asList(sourceTypes).contains(Object.class))
        {
            //May lift the ban on Object return and source types
            //The details as to why the method is invalid are in the message
            throw new IllegalArgumentException(AnnotationsMessages.transformerMethodNotValid(method).getMessage());
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (AnnotationUtils.methodHasParamAnnotations(transformMethod))
        {
            try
            {
                paramTransformer = ExpressionAnnotationsHelper.getTransformerForMethodWithAnnotations(transformMethod, muleContext);
            }
            catch (TransformerException e)
            {
                throw new InitialisationException(e, this);
            }
        }
        resolvers = muleContext.getRegistry().lookupObjects(TransformerArgumentResolver.class);
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Object firstArg = null;
        Object[] params = new Object[transformMethod.getParameterTypes().length];
        int paramCounter = 0;

        //the param transformer will convert the message to one or more objects depending on the annotations on the method
        //parameter
        if (paramTransformer != null)
        {
            Object paramArgs = paramTransformer.transformMessage(message, outputEncoding);

            if (paramArgs != null && paramArgs.getClass().isArray())
            {
                Object[] temp = (Object[]) paramArgs;
                //if the source is annotated, the paramTransformer will figure out the first parameter
                if (!sourceAnnotated)
                {
                    paramCounter++;
                }
                for (int i = 0; i < temp.length; i++)
                {
                    params[paramCounter++] = temp[i];
                }
            }
            else
            {
                //if the source is annotated, the paramTransformer will figure out the first parameter
                if (!sourceAnnotated)
                {
                    paramCounter++;
                }
                params[paramCounter++] = paramArgs;
            }
        }

        if (messageAware)
        {
            firstArg = message;
        }
        else if (!sourceAnnotated)
        {
            //This will perform any additional transformation from the source type to the method parameter type
            try
            {
                firstArg = message.getPayload(DataTypeFactory.create(transformMethod.getParameterTypes()[0]));
            }
            catch (TransformerException e)
            {
                throw new TransformerException(e.getI18nMessage(), this, e);
            }
        }

        //if the source is annotated, the paramTransformer will figure out the first parameter
        if (!sourceAnnotated)
        {
            params[0] = firstArg;
            if (paramCounter == 0)
            {
                paramCounter++;
            }
        }
        //Lets see if there are any context objects to inject (i.e. JAXB)
        if (paramCounter < transformMethod.getParameterTypes().length)
        {
            for (int i = paramCounter; i < transformMethod.getParameterTypes().length; i++)
            {
                Object o;
                Class<?> type = transformMethod.getParameterTypes()[i];
                o = cachedObjects.get(type);
                if (o != null)
                {
                    params[paramCounter++] = o;
                    continue;
                }
                DataType<?> source = DataTypeFactory.createFromParameterType(transformMethod, 0);

                for (TransformerArgumentResolver resolver : resolvers)
                {
                    try
                    {
                        o = resolver.resolve(type, source, this.returnType, muleContext);
                        if (o != null)
                        {
                            params[paramCounter++] = o;
                            cachedObjects.put(type, o);
                            break;
                        }
                    }
                    catch (Exception e)
                    {
                        throw new TransformerException(this, e);
                    }

                }
                if (o == null)
                {
                    throw new IllegalArgumentException("Failed to find resolver for object type: " + type + " for transform method: " + transformMethod);
                }
            }
        }
        try
        {
            return transformMethod.invoke(proxy, params);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new TransformerException(this, e);
        }
    }

    public int getPriorityWeighting()
    {
        return weighting;
    }

    public void setPriorityWeighting(int weighting)
    {
        throw new UnsupportedOperationException("setPriorityWeighting");
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        AnnotatedTransformerProxy that = (AnnotatedTransformerProxy) o;

        if (messageAware != that.messageAware)
        {
            return false;
        }
        if (weighting != that.weighting)
        {
            return false;
        }
        if (proxy != null ? !proxy.equals(that.proxy) : that.proxy != null)
        {
            return false;
        }
        if (transformMethod != null ? !transformMethod.equals(that.transformMethod) : that.transformMethod != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = weighting;
        result = 31 * result + (proxy != null ? proxy.hashCode() : 0);
        result = 31 * result + (transformMethod != null ? transformMethod.hashCode() : 0);
        result = 31 * result + (messageAware ? 1 : 0);
        return result;
    }

}
