/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.config;

import static org.mule.module.extensions.internal.util.MuleExtensionUtils.isExpression;
import static org.mule.module.extensions.internal.util.NameUtils.getGlobalPojoTypeName;
import static org.mule.module.extensions.internal.util.NameUtils.hyphenize;
import static org.mule.module.extensions.internal.util.NameUtils.singularize;
import org.mule.api.NestedProcessor;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.extensions.introspection.DataQualifierVisitor;
import org.mule.extensions.introspection.DataType;
import org.mule.extensions.introspection.Parameter;
import org.mule.module.extensions.internal.capability.xml.schema.model.SchemaConstants;
import org.mule.module.extensions.internal.introspection.BaseDataQualifierVisitor;
import org.mule.module.extensions.internal.introspection.SimpleTypeDataQualifierVisitor;
import org.mule.module.extensions.internal.runtime.DefaultObjectBuilder;
import org.mule.module.extensions.internal.runtime.ObjectBuilder;
import org.mule.module.extensions.internal.runtime.resolver.CachingValueResolverWrapper;
import org.mule.module.extensions.internal.runtime.resolver.CollectionValueResolver;
import org.mule.module.extensions.internal.runtime.resolver.EvaluateAndTransformValueResolver;
import org.mule.module.extensions.internal.runtime.resolver.NestedProcessorValueResolver;
import org.mule.module.extensions.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.module.extensions.internal.runtime.resolver.RegistryLookupValueResolver;
import org.mule.module.extensions.internal.runtime.resolver.ResolverSet;
import org.mule.module.extensions.internal.runtime.resolver.StaticValueResolver;
import org.mule.module.extensions.internal.runtime.resolver.ValueResolver;
import org.mule.module.extensions.internal.util.IntrospectionUtils;
import org.mule.module.extensions.internal.util.NameUtils;
import org.mule.util.TemplateParser;
import org.mule.util.ValueHolder;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Utility methods for XML parsers capable of handling objects described by the extensions introspection API
 *
 * @since 3.7.0
 */
final class XmlExtensionParserUtils
{

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss";
    private static final String CALENDAR_FORMAT = "yyyy-MM-dd'T'hh:mm:ssX";

    private static final TemplateParser parser = TemplateParser.createMuleStyleParser();
    private static final ConversionService conversionService = new DefaultConversionService();

    static void parseConfigName(Element element, BeanDefinitionBuilder builder)
    {
        String name = AutoIdUtils.getUniqueName(element, "mule-bean");
        element.setAttribute("name", name);
        builder.addConstructorArgValue(name);
    }

    private static ValueResolver parseCollectionAsInnerElement(ElementDescriptor collectionElement,
                                                               String childElementName,
                                                               DataType collectionType)
    {
        final DataType itemsType = collectionType.getGenericTypes().length > 0 ? collectionType.getGenericTypes()[0] : DataType.of(Object.class);
        final List<ValueResolver<Object>> resolvers = new LinkedList<>();

        for (final ElementDescriptor item : collectionElement.getChildsByName(childElementName))
        {
            DataQualifierVisitor visitor = new BaseDataQualifierVisitor()
            {
                @Override
                public void onPojo()
                {
                    resolvers.add(new ObjectBuilderValueResolver(recursePojoProperties(itemsType.getRawType(), item)));
                }

                @Override
                protected void defaultOperation()
                {
                    String value = item.getAttribute(SchemaConstants.ATTRIBUTE_NAME_VALUE);
                    resolvers.add(getResolverFromValue(value, itemsType));
                }
            };

            itemsType.getQualifier().accept(visitor);
        }

        return CollectionValueResolver.of((Class<Collection>) collectionType.getRawType(), resolvers);
    }

    private static ValueResolver parseCollection(ElementDescriptor element,
                                                 String fieldName,
                                                 String parentElementName,
                                                 String childElementName,
                                                 Object defaultValue,
                                                 DataType collectionDataType)
    {
        ValueResolver resolver = getResolverFromAttribute(element, fieldName, collectionDataType, defaultValue);
        if (resolver == null)
        {
            ElementDescriptor collectionElement = element.getChildByName(parentElementName);
            if (collectionElement != null)
            {
                resolver = parseCollectionAsInnerElement(collectionElement, childElementName, collectionDataType);
            }
            else
            {
                resolver = new StaticValueResolver(defaultValue);
            }
        }

        return resolver;
    }

    private static ValueResolver getResolverFromAttribute(ElementDescriptor element, String attributeName, DataType expectedDataType, Object defaultValue)
    {
        return getResolverFromValue(getAttributeValue(element, attributeName, defaultValue), expectedDataType);
    }

    static Object getAttributeValue(ElementDescriptor element, String attributeName, Object defaultValue)
    {
        return element.hasAttribute(attributeName)
               ? element.getAttribute(attributeName)
               : defaultValue;
    }

    private static ValueResolver getResolverFromValue(final Object value, final DataType expectedDataType)
    {
        if (isExpression(value, parser))
        {
            return new EvaluateAndTransformValueResolver((String) value, expectedDataType);
        }

        if (value != null)
        {
            final ValueHolder<ValueResolver> resolverValueHolder = new ValueHolder<>();
            DataQualifierVisitor visitor = new SimpleTypeDataQualifierVisitor()
            {

                @Override
                protected void onSimpleType()
                {
                    if (conversionService.canConvert(value.getClass(), expectedDataType.getRawType()))
                    {
                        resolverValueHolder.set(new StaticValueResolver(conversionService.convert(value, expectedDataType.getRawType())));
                    }
                    else
                    {
                        defaultOperation();
                    }
                }

                @Override
                protected void defaultOperation()
                {
                    resolverValueHolder.set(new CachingValueResolverWrapper(new RegistryLookupValueResolver(value.toString())));
                }
            };

            expectedDataType.getQualifier().accept(visitor);
            return resolverValueHolder.get();
        }

        return null;
    }

    /**
     * parses a pojo which type is described by {@code pojoType},
     * recursively moving through the pojo's properties.
     *
     * @param element          the XML element which has the bean as a child
     * @param fieldName        the name of the field in which the parsed pojo is going to be assigned
     * @param childElementName the name of the the bean's top level XML element
     * @param pojoType         a {@link DataType} describing the bean's type
     * @return a {@link org.springframework.beans.factory.config.BeanDefinition} if the bean could be parsed, {@code null}
     * if the bean is not present on the XML definition
     */
    private static ValueResolver parsePojo(ElementDescriptor element,
                                           String fieldName,
                                           String childElementName,
                                           DataType pojoType,
                                           Object defaultValue)
    {

        // check if the pojo is referenced as an attribute
        ValueResolver resolver = getResolverFromAttribute(element, fieldName, pojoType, defaultValue);

        if (resolver != null)
        {
            return resolver;
        }

        // check if the pojo is defined inline as a child element
        ElementDescriptor childElement = element.getChildByName(childElementName);

        if (childElement != null)
        {
            return getPojoValueResolver(pojoType, childElement);
        }

        // last chance, check if this is a top level element
        if (getGlobalPojoTypeName(pojoType).equals(element.getName()))
        {
            return getPojoValueResolver(pojoType, element);
        }

        // pojo was not specified, take the coward's route and return a null resolver
        return new StaticValueResolver(null);
    }

    private static ValueResolver getPojoValueResolver(DataType pojoType, ElementDescriptor element)
    {
        return new ObjectBuilderValueResolver(recursePojoProperties(pojoType.getRawType(), element));
    }

    private static ObjectBuilder<Object> recursePojoProperties(Class<?> declaringClass, ElementDescriptor element)
    {
        ObjectBuilder builder = new DefaultObjectBuilder(declaringClass);

        for (Map.Entry<Method, DataType> entry : IntrospectionUtils.getSettersDataTypes(declaringClass).entrySet())
        {
            Method setter = entry.getKey();

            if (IntrospectionUtils.isIgnored(setter))
            {
                continue;
            }

            String parameterName = NameUtils.getFieldNameFromSetter(setter.getName());
            DataType dataType = entry.getValue();

            ValueResolver resolver = getResolverFromAttribute(element, parameterName, dataType, null);

            if (resolver == null)
            {
                parameterName = hyphenize(parameterName);
                ElementDescriptor childElement = element.getChildByName(parameterName);
                if (childElement != null)
                {
                    ObjectBuilder childBuilder = recursePojoProperties(dataType.getRawType(), childElement);
                    resolver = new ObjectBuilderValueResolver(childBuilder);
                }
            }

            if (resolver != null)
            {
                builder.addPropertyResolver(setter, resolver);
            }
        }

        return builder;
    }

    private static Date doParseDate(ElementDescriptor element,
                                    String attributeName,
                                    String parseFormat,
                                    Object defaultValue)
    {

        Object value = getAttributeValue(element, attributeName, defaultValue);

        if (value == null)
        {
            return null;
        }

        if (value instanceof String)
        {
            SimpleDateFormat format = new SimpleDateFormat(parseFormat);
            try
            {
                return format.parse((String) value);
            }
            catch (ParseException e)
            {
                throw new IllegalArgumentException(String.format("Could not transform value '%s' into a Date using pattern %s", value, parseFormat));
            }
        }

        if (value instanceof Date)
        {
            return (Date) value;
        }

        throw new IllegalArgumentException(
                String.format("Could not transform value of type '%s' to Date", value != null ? value.getClass().getName() : "null"));
    }

    private static ValueResolver parseCalendar(ElementDescriptor element, String attributeName, DataType dataType, Object defaultValue)
    {
        Object value = getAttributeValue(element, attributeName, defaultValue);
        if (isExpression(value, parser))
        {
            return new EvaluateAndTransformValueResolver((String) value, dataType);
        }

        Date date = doParseDate(element, attributeName, CALENDAR_FORMAT, defaultValue);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return new StaticValueResolver(calendar);
    }

    private static ValueResolver parseDate(ElementDescriptor element, String attributeName, DataType dataType, Object defaultValue)
    {
        Object value = getAttributeValue(element, attributeName, defaultValue);
        if (isExpression(value, parser))
        {
            return new EvaluateAndTransformValueResolver((String) value, dataType);
        }
        else
        {
            return new StaticValueResolver(doParseDate(element, attributeName, DATE_FORMAT, defaultValue));
        }
    }

    static void setNoRecurseOnDefinition(BeanDefinition definition)
    {
        definition.setAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE, Boolean.TRUE);
    }

    static ValueResolver parseParameter(ElementDescriptor element, Parameter parameter)
    {
        return parseElement(element, parameter.getName(), parameter.getType(), parameter.getDefaultValue());
    }

    static ValueResolver parseElement(final ElementDescriptor element,
                                      final String fieldName,
                                      final DataType dataType,
                                      final Object defaultValue)
    {
        final String hyphenizedFieldName = hyphenize(fieldName);
        final String singularName = singularize(hyphenizedFieldName);
        final ValueHolder<ValueResolver> resolverReference = new ValueHolder<>();

        DataQualifierVisitor visitor = new BaseDataQualifierVisitor()
        {

            /**
             * An attribute of a supported or unknown type
             */
            @Override
            public void defaultOperation()
            {
                resolverReference.set(getResolverFromValue(getAttributeValue(element, fieldName, defaultValue), dataType));
            }

            /**
             * A collection type. Might be defined in an inner element or referenced
             * from an attribute
             */
            @Override
            public void onList()
            {
                resolverReference.set(parseCollection(element, fieldName, hyphenizedFieldName, singularName, defaultValue, dataType));
            }

            @Override
            public void onOperation()
            {
                super.onOperation();
            }

            @Override
            public void onPojo()
            {
                resolverReference.set(parsePojo(element, fieldName, hyphenizedFieldName, dataType, defaultValue));
            }

            @Override
            public void onDateTime()
            {
                if (Calendar.class.isAssignableFrom(dataType.getRawType()))
                {
                    resolverReference.set(parseCalendar(element, fieldName, dataType, defaultValue));
                }
                else
                {
                    resolverReference.set(parseDate(element, fieldName, dataType, defaultValue));
                }
            }
        };

        dataType.getQualifier().accept(visitor);
        return resolverReference.get();
    }

    static BeanDefinition toElementDescriptorBeanDefinition(Element element)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ElementDescriptor.class);
        builder.addConstructorArgValue(element.getLocalName());
        parseElementDescriptorAttributes(element, builder);
        parseElementDescriptorChilds(element, builder);

        return builder.getBeanDefinition();
    }

    static ResolverSet getResolverSet(ElementDescriptor element, List<Parameter> parameters)
    {
        return getResolverSet(element, parameters, ImmutableMap.<String, List<MessageProcessor>>of());
    }

    static ResolverSet getResolverSet(ElementDescriptor element, List<Parameter> parameters, Map<String, List<MessageProcessor>> nestedOperations)
    {
        ResolverSet resolverSet = new ResolverSet();

        for (Parameter parameter : parameters)
        {
            List<MessageProcessor> nestedProcessors = nestedOperations.get(parameter.getName());
            if (!CollectionUtils.isEmpty(nestedProcessors))
            {
                addNestedProcessorResolver(resolverSet, parameter, nestedProcessors);
            }
            else
            {
                ValueResolver<?> resolver = parseParameter(element, parameter);
                resolverSet.add(parameter, resolver != null ? resolver : new StaticValueResolver(null));
            }
        }

        return resolverSet;
    }

    private static void addNestedProcessorResolver(ResolverSet resolverSet, Parameter parameter, List<MessageProcessor> nestedProcessors)
    {
        if (nestedProcessors.size() == 1)
        {
            resolverSet.add(parameter, new NestedProcessorValueResolver(nestedProcessors.get(0)));
        }
        else
        {
            List<ValueResolver<NestedProcessor>> nestedProcessorResolvers = new ArrayList<>(nestedProcessors.size());
            for (MessageProcessor nestedProcessor : nestedProcessors)
            {
                nestedProcessorResolvers.add(new NestedProcessorValueResolver(nestedProcessor));
            }

            resolverSet.add(parameter, CollectionValueResolver.of(ArrayList.class, nestedProcessorResolvers));
        }
    }

    static void applyLifecycle(BeanDefinitionBuilder builder)
    {
        Class<?> declaringClass = builder.getBeanDefinition().getBeanClass();
        if (Initialisable.class.isAssignableFrom(declaringClass))
        {
            builder.setInitMethodName(Initialisable.PHASE_NAME);
        }

        if (Disposable.class.isAssignableFrom(declaringClass))
        {
            builder.setDestroyMethodName(Disposable.PHASE_NAME);
        }
    }

    private static void parseElementDescriptorChilds(Element element, BeanDefinitionBuilder builder)
    {
        ManagedList<BeanDefinition> managedChilds = new ManagedList<>();
        for (Element child : DomUtils.getChildElements(element))
        {
            managedChilds.add(toElementDescriptorBeanDefinition(child));
        }

        builder.addConstructorArgValue(managedChilds);
    }

    private static void parseElementDescriptorAttributes(Element element, BeanDefinitionBuilder builder)
    {
        ManagedMap<String, String> managedAttributes = new ManagedMap<>();
        NamedNodeMap attributes = element.getAttributes();

        for (int i = 0; i < attributes.getLength(); i++)
        {
            String name = attributes.item(i).getLocalName();
            managedAttributes.put(name, element.getAttribute(name));
        }

        builder.addConstructorArgValue(managedAttributes);
    }
}
