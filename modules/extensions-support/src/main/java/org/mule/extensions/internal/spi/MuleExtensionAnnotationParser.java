/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal.spi;

import static org.mule.util.Preconditions.checkState;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.extensions.api.annotation.Configurable;
import org.mule.extensions.api.annotation.Module;
import org.mule.extensions.api.annotation.Operation;
import org.mule.extensions.api.annotation.param.Optional;
import org.mule.extensions.api.annotation.param.Payload;
import org.mule.extensions.spi.NestedProcessor;
import org.mule.util.ClassUtils;
import org.mule.util.ParamReader;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;

final class MuleExtensionAnnotationParser
{

    private static final Set<Class<?>> notParametrizableTypes = ImmutableSet.<Class<?>>builder()
            .add(MuleEvent.class)
            .add(MuleMessage.class)
            .add(NestedProcessor.class)
            .build();

    static ExtensionDescriptor parseExtensionDescriptor(Class<?> extensionType)
    {
        ExtensionDescriptor extension = resolveExtensionType(extensionType);
        extension.addConfigurableFields(getConfigurableFields(extensionType));
        extension.addOperationMethods(ClassUtils.getMethodsAnnotatedWith(extensionType, Operation.class));

        return extension;
    }

    private static ExtensionDescriptor resolveExtensionType(Class<?> extensionType)
    {
        Module module = extensionType.getAnnotation(Module.class);
        ExtensionDescriptor extension = null;

        if (module != null)
        {
            extension = new ExtensionDescriptor(module);
        } // TODO: add branch for connector type and throw exception if no annotation at all

        return extension;
    }

    private static Collection<Field> getConfigurableFields(Class<?> extensionType)
    {
        List<Field> fields = ClassUtils.getDeclaredFields(extensionType, true);
        return CollectionUtils.select(fields, new Predicate()
        {
            @Override
            public boolean evaluate(Object object)
            {
                Field field = (Field) object;
                return field.getAnnotation(Configurable.class) != null;
            }
        });
    }


    public static List<ParameterDescriptor> parseParameter(Method method, ExtensionDescriptor extension)
    {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes == null || parameterTypes.length == 0)
        {
            return Collections.emptyList();
        }

        String[] paramNames = getParamNames(method, extension);
        checkState(parameterTypes.length == paramNames.length, "Invalid class information. Parameter counts don't match");

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        List<ParameterDescriptor> parameters = new ArrayList<ParameterDescriptor>(paramNames.length);

        for (int i = 0; i < paramNames.length; i++)
        {
            if (!isParametrizable(parameterTypes[i], parameterAnnotations[i]))
            {
                continue;
            }

            ParameterDescriptor parameter = new ParameterDescriptor();
            parameter.setName(paramNames[i]);
            parameter.setType(parameterTypes[i]);

            Map<Class<? extends Annotation>, Annotation> annotations = parseAnnotations(parameterAnnotations[i]);

            Optional optional = (Optional) annotations.get(Optional.class);
            if (optional != null)
            {
                parameter.setRequired(false);
                String defaultValue = optional.defaultValue();
                if (!StringUtils.isEmpty(defaultValue))
                {
                    parameter.setDefaultValue(defaultValue);
                }
            }
            else
            {
                parameter.setRequired(true);
            }

            parameters.add(parameter);
        }

        return parameters;
    }

    private static boolean isParametrizable(Class<?> type, Annotation[] annotations)
    {
        if (notParametrizableTypes.contains(type))
        {
            return false;
        }

        return !contains(annotations, Payload.class);
    }

    private static <T extends Annotation> boolean contains(Annotation[] annotations, Class<T> annotationType)
    {
        for (Annotation annotation : annotations)
        {
            if (annotationType.isInstance(annotation))
            {
                return true;
            }
        }

        return false;
    }

    private static String[] getParamNames(Method method, ExtensionDescriptor extension)
    {
        String[] paramNames;
        try
        {
            paramNames = new ParamReader(method.getDeclaringClass()).getParameterNames(method);
        }
        catch (IOException e)
        {
            throw new IllegalStateException(
                    String.format("Could not read parameter names from method %s of class %s", method.getName(), extension.getName())
                    , e);
        }

        return paramNames;
    }

    private static Map<Class<? extends Annotation>, Annotation> parseAnnotations(Annotation[] annotations)
    {

        Map<Class<? extends Annotation>, Annotation> map = new HashMap<Class<? extends Annotation>, Annotation>();

        for (Annotation annotation : annotations)
        {
            map.put(resolveAnnotationClass(annotation), annotation);
        }

        return map;
    }

    private static Class<? extends Annotation> resolveAnnotationClass(Annotation annotation)
    {
        if (Proxy.isProxyClass(annotation.getClass()))
        {
            return (Class<Annotation>) annotation.getClass().getInterfaces()[0];
        }
        else
        {
            return annotation.getClass();
        }
    }


}
