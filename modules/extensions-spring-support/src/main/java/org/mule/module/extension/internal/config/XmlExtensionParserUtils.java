/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.ATTRIBUTE_NAME_KEY;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.ATTRIBUTE_NAME_VALUE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.CONFIG_ATTRIBUTE;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getAlias;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getExposedFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getFieldDataType;
import static org.mule.module.extension.internal.util.NameUtils.getTopLevelTypeName;
import static org.mule.module.extension.internal.util.NameUtils.hyphenize;
import static org.mule.module.extension.internal.util.NameUtils.pluralize;
import static org.mule.module.extension.internal.util.NameUtils.singularize;
import static org.springframework.util.xml.DomUtils.getChildElements;
import org.mule.api.MuleEvent;
import org.mule.api.NestedProcessor;
import org.mule.api.config.ConfigurationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.extension.api.introspection.DataQualifier;
import org.mule.extension.api.introspection.DataQualifierVisitor;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.module.extension.internal.introspection.AbstractDataQualifierVisitor;
import org.mule.module.extension.internal.introspection.SimpleTypeDataQualifierVisitor;
import org.mule.module.extension.internal.runtime.DefaultObjectBuilder;
import org.mule.module.extension.internal.runtime.ObjectBuilder;
import org.mule.module.extension.internal.runtime.resolver.CollectionValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ExpressionFunctionValueResolver;
import org.mule.module.extension.internal.runtime.resolver.MapValueResolver;
import org.mule.module.extension.internal.runtime.resolver.NestedProcessorValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.module.extension.internal.runtime.resolver.RegistryLookupValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.module.extension.internal.util.IntrospectionUtils;
import org.mule.module.extension.internal.util.NameUtils;
import org.mule.util.ArrayUtils;
import org.mule.util.TemplateParser;
import org.mule.util.ValueHolder;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
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

    /**
     * Parses the given {@code element} for an attribute named {@code name}. If not found,
     * a name is auto generated for the element. In either case, the obtained name
     * is set on the {@code builder} using the {@link BeanDefinitionBuilder#addConstructorArgValue(Object)}
     * method
     *
     * @param element an {@link Element} being parsed
     * @param builder a {@link BeanDefinitionBuilder}
     * @return the parsed name
     */
    static String parseConfigName(Element element, BeanDefinitionBuilder builder)
    {
        return parseName(element, "config", builder);
    }

    //TODO: Discuss EE-4683
    static void parseConnectionProviderName(Element element, BeanDefinitionBuilder builder)
    {
        parseName(element, "connection-provider", builder);
    }

    static String parseName(Element element, String type, BeanDefinitionBuilder builder)
    {
        String name = AutoIdUtils.getUniqueName(element, type);
        element.setAttribute("name", name);
        builder.addConstructorArgValue(name);

        return name;
    }

    /**
     * Sets the {@link MuleHierarchicalBeanDefinitionParserDelegate#MULE_NO_RECURSE} attribute
     * on the given {@code definition}
     *
     * @param definition a {@link BeanDefinition}
     */
    static void setNoRecurseOnDefinition(BeanDefinition definition)
    {
        definition.setAttribute(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE, Boolean.TRUE);
    }

    /**
     * Parses an {@code element} which is assumed to be a representation of the given
     * {@code parameter}. It then returns a {@link ValueResolver} which will provide
     * the actual value extracted from the {@code element}
     *
     * @param element        a {@link ElementDescriptor}
     * @param parameterModel a {@link ParameterModel}
     * @return a {@link ValueResolver}
     */
    static ValueResolver parseParameter(ElementDescriptor element, ParameterModel parameterModel)
    {
        if (parameterModel.getExpressionSupport() == ExpressionSupport.LITERAL)
        {
            return new StaticValueResolver<>(getAttributeValue(element, parameterModel.getName(), parameterModel.getDefaultValue()));
        }

        return parseElement(element, parameterModel.getName(), parameterModel.getType(), parameterModel.getDefaultValue());
    }

    /**
     * Parses an {@code element} which is assumed to be a representation of the given
     * {@code dataType}. It then returns a {@link ValueResolver} which will provide
     * the actual value extracted from the {@code element}
     *
     * @param element      a {@link ElementDescriptor}
     * @param fieldName    the name of the variable in which the value is to be stored
     * @param dataType     the {@link DataType} that describes the type of the value to be extracted
     * @param defaultValue a default value in case that the {@code element} does not contain an actual value
     * @return a {@link ValueResolver}
     */
    static ValueResolver parseElement(final ElementDescriptor element,
                                      final String fieldName,
                                      final DataType dataType,
                                      final Object defaultValue)
    {
        final String hyphenizedFieldName = hyphenize(fieldName);
        final String singularName = singularize(hyphenizedFieldName);
        final String pluralName = pluralize(hyphenizedFieldName);
        final ValueHolder<ValueResolver> resolverReference = new ValueHolder<>();

        DataQualifierVisitor visitor = new AbstractDataQualifierVisitor()
        {

            /**
             * An attribute of a generic type
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

            /**
             * A map type. Might be defined in an inner element or referenced
             * from an attribute
             */
            @Override
            public void onMap()
            {
                String parentname;
                String childname;
                if (StringUtils.equals(pluralName, hyphenizedFieldName))
                {
                    parentname = hyphenizedFieldName;
                    childname = singularName;
                }
                else
                {
                    parentname = pluralName;
                    childname = hyphenizedFieldName;
                }

                resolverReference.set(
                        parseMap(element, fieldName,
                                 parentname, childname,
                                 defaultValue, dataType));
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

    /**
     * Creates a {@link BeanDefinition} which generates a {@link ElementDescriptor}
     * that describes the given {@code element}
     *
     * @param element the {@link Element} you want to describe
     * @return a {@link BeanDefinition}
     */
    static BeanDefinition toElementDescriptorBeanDefinition(Element element)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ElementDescriptor.class);
        builder.addConstructorArgValue(element.getLocalName());
        parseElementDescriptorAttributes(element, builder);
        parseElementDescriptorChilds(element, builder);
        builder.addConstructorArgValue(element.getParentNode());

        return builder.getBeanDefinition();
    }

    /**
     * Creates a new {@link ResolverSet} which provides a {@link ValueResolver valueResolvers}
     * for each {@link ParameterModel} in the {@code parameters} list,
     * taking the same {@code element} as source
     *
     * @param element         a {@link ElementDescriptor} from which the {@link ValueResolver valueResolvers} will be taken from
     * @param parameterModels a {@link List} of {@link ParameterModel parameterModels}
     * @return a {@link ResolverSet}
     * @throws ConfigurationException in case of invalid configuration
     */
    static ResolverSet getResolverSet(ElementDescriptor element, List<ParameterModel> parameterModels) throws ConfigurationException
    {
        return getResolverSet(element, parameterModels, ImmutableMap.<String, List<MessageProcessor>>of());
    }

    /**
     * Creates a new {@link ResolverSet} which provides a {@link ValueResolver valueResolvers}
     * for each {@link ParameterModel} in the {@code parameters} list, taking as source the given {@code element}
     * an a {@code nestedOperations} {@link Map} which has {@link ParameterModel} names as keys, and {@link List}s
     * of {@link MessageProcessor} as values.
     * <p>
     * For each {@link ParameterModel} in the {@code parameters} list, if an entry exists in the {@code nestedOperations}
     * {@link Map}, a {@link ValueResolver} that generates {@link NestedProcessor} instances will be added to the
     * {@link ResolverSet}. Otherwise, a {@link ValueResolver} will be inferred from the given {@code element} just
     * like the {@link #getResolverSet(ElementDescriptor, List)} method does
     *
     * @param element          a {@link ElementDescriptor} from which the {@link ValueResolver valueResolvers} will be taken from
     * @param parameterModels  a {@link List} of {@link ParameterModel parameterModels}
     * @param nestedOperations a {@link Map} which has {@link ParameterModel} names as keys, and {@link List}s
     *                         of {@link MessageProcessor} as values
     * @return a {@link ResolverSet}
     * @throws ConfigurationException in case of invalid configuration
     */
    static ResolverSet getResolverSet(ElementDescriptor element, List<ParameterModel> parameterModels, Map<String, List<MessageProcessor>> nestedOperations) throws ConfigurationException
    {
        ResolverSet resolverSet = new ResolverSet();

        for (ParameterModel parameterModel : parameterModels)
        {
            List<MessageProcessor> nestedProcessors = nestedOperations.get(parameterModel.getName());
            if (!CollectionUtils.isEmpty(nestedProcessors))
            {
                addNestedProcessorResolver(resolverSet, parameterModel, nestedProcessors);
            }
            else
            {
                ValueResolver<?> resolver = getValueResolver(element, parameterModel);

                if (resolver.isDynamic() && parameterModel.getExpressionSupport() == ExpressionSupport.NOT_SUPPORTED)
                {
                    throw new ConfigurationException(createStaticMessage(String.format(
                            "An expression value was given for parameter '%s' but it doesn't support expressions", parameterModel.getName())));
                }

                if (!resolver.isDynamic() && parameterModel.getExpressionSupport() == ExpressionSupport.REQUIRED && parameterModel.isRequired())
                {
                    throw new ConfigurationException(createStaticMessage(String.format(
                            "A fixed value was given for parameter '%s' but it only supports expressions", parameterModel.getName())));
                }

                resolverSet.add(parameterModel, resolver);
            }
        }

        return resolverSet;
    }

    /**
     * Returns a value associated with the {@code element}. If the {@code element} has an attribute
     * named {@code attributeName}, then it returns the value of such attribute. Otherwise, it returns
     * {@code defaultValue}
     *
     * @param element       a {@link ElementDescriptor}
     * @param attributeName the name of an attribute presumed to exist in the {@code element}
     * @param defaultValue  a default value in case that the {@code element} does not have the presumed attribute
     * @return the value of the {@code element}
     */
    static Object getAttributeValue(ElementDescriptor element, String attributeName, Object defaultValue)
    {
        return element.hasAttribute(attributeName)
               ? element.getAttribute(attributeName)
               : defaultValue;
    }

    static void parseConfigRef(Element element, BeanDefinitionBuilder builder)
    {
        String configRef = element.getAttribute(CONFIG_ATTRIBUTE);
        if (StringUtils.isBlank(configRef))
        {
            configRef = null;
        }

        builder.addConstructorArgValue(configRef);
    }

    private static ValueResolver parseCollectionAsInnerElement(ElementDescriptor collectionElement,
                                                               String childElementName,
                                                               final DataType collectionType)
    {
        final DataType itemsType = ArrayUtils.isEmpty(collectionType.getGenericTypes()) ? DataType.of(Object.class) : collectionType.getGenericTypes()[0];
        final List<ValueResolver<Object>> resolvers = new LinkedList<>();

        for (final ElementDescriptor item : collectionElement.getChildsByName(childElementName))
        {
            DataQualifierVisitor visitor = new AbstractDataQualifierVisitor()
            {
                @Override
                public void onPojo()
                {
                    resolvers.add(new ObjectBuilderValueResolver(recursePojoProperties(itemsType.getRawType(), item)));
                }

                @Override
                protected void defaultOperation()
                {
                    resolvers.add(getResolverFromValue(item.getAttribute(ATTRIBUTE_NAME_VALUE), itemsType));
                }
            };

            itemsType.getQualifier().accept(visitor);
        }

        return CollectionValueResolver.of((Class<Collection>) collectionType.getRawType(), resolvers);
    }

    private static ValueResolver parseMapAsInnerElement(ElementDescriptor mapElement,
                                                        String childElementName,
                                                        DataType mapType)
    {
        final DataType keyType = mapType.getGenericTypes().length > 1 ? mapType.getGenericTypes()[0] : DataType.of(Object.class);
        final DataType valueType = mapType.getGenericTypes().length > 1 ? mapType.getGenericTypes()[1] : DataType.of(Object.class);
        final List<ValueResolver<Object>> keyResolvers = new LinkedList<>();
        final List<ValueResolver<Object>> valueResolvers = new LinkedList<>();

        for (final ElementDescriptor item : mapElement.getChildsByName(childElementName))
        {
            keyResolvers.add(getResolverFromValue(item.getAttribute(ATTRIBUTE_NAME_KEY), keyType));

            valueType.getQualifier().accept(new AbstractDataQualifierVisitor()
            {
                @Override
                public void onPojo()
                {
                    valueResolvers.add(parsePojo(item,
                                                 ATTRIBUTE_NAME_VALUE,
                                                 NameUtils.getTopLevelTypeName(valueType),
                                                 valueType,
                                                 null));
                }

                @Override
                public void onList()
                {
                    ValueResolver<Object> resolver;
                    String valueAsExpression = item.getAttribute(ATTRIBUTE_NAME_VALUE);
                    if (!StringUtils.isBlank(valueAsExpression))
                    {
                        resolver = getResolverFromValue(valueAsExpression, valueType);
                    }
                    else
                    {
                        String itemName = hyphenize(NameUtils.singularize(childElementName)).concat("-item");
                        resolver = parseCollectionAsInnerElement(item, itemName, valueType);
                    }

                    valueResolvers.add(resolver);
                }

                @Override
                protected void defaultOperation()
                {
                    valueResolvers.add(getResolverFromValue(item.getAttribute(ATTRIBUTE_NAME_VALUE), valueType));
                }
            });
        }

        return MapValueResolver.of((Class<Map>) mapType.getRawType(), keyResolvers, valueResolvers);
    }

    private static ValueResolver parseCollection(ElementDescriptor element,
                                                 String fieldName,
                                                 String parentElementName,
                                                 String childElementName,
                                                 Object defaultValue,
                                                 DataType collectionType)
    {
        ValueResolver resolver = getResolverFromAttribute(element, fieldName, collectionType, defaultValue);
        if (resolver == null)
        {
            ElementDescriptor collectionElement = element.getChildByName(parentElementName);
            if (collectionElement != null)
            {
                resolver = parseCollectionAsInnerElement(collectionElement, childElementName, collectionType);
            }
            else
            {
                resolver = new StaticValueResolver(defaultValue);
            }
        }

        return resolver;
    }

    private static ValueResolver parseMap(ElementDescriptor element,
                                          String fieldName,
                                          String parentElementName,
                                          String childElementName,
                                          Object defaultValue,
                                          DataType mapDataType)
    {
        ValueResolver resolver = getResolverFromAttribute(element, fieldName, mapDataType, defaultValue);
        if (resolver == null)
        {
            ElementDescriptor mapElement = element.getChildByName(parentElementName);
            if (mapElement != null)
            {
                resolver = parseMapAsInnerElement(mapElement, childElementName, mapDataType);
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

    private static ValueResolver getResolverFromValue(final Object value, final DataType expectedDataType)
    {
        if (isExpressionFunction(expectedDataType) && value != null)
        {
            return new ExpressionFunctionValueResolver<>((String) value, expectedDataType.getGenericTypes()[1]);
        }

        if (isExpression(value, parser))
        {
            return new TypeSafeExpressionValueResolver((String) value, expectedDataType);
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
                    resolverValueHolder.set(new RegistryLookupValueResolver(value.toString()));
                }
            };

            expectedDataType.getQualifier().accept(visitor);
            return resolverValueHolder.get();
        }

        return null;
    }

    private static boolean isExpressionFunction(DataType dataType)
    {
        return dataType.getRawType().equals(Function.class)
               && dataType.getGenericTypes().length == 2
               && dataType.getGenericTypes()[0].getRawType().equals(MuleEvent.class);
    }

    /**
     * Parses a pojo which type is described by {@code pojoType},
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
        if (getTopLevelTypeName(pojoType).equals(element.getName()))
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

        for (Field field : getExposedFields(declaringClass))
        {
            if (IntrospectionUtils.isIgnored(field))
            {
                continue;
            }

            String parameterName = getAlias(field);
            DataType dataType = getFieldDataType(field);

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
                builder.addPropertyResolver(field, resolver);
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
                throw new IllegalArgumentException(String.format("Could not transform value '%s' into a Date using pattern '%s'", value, parseFormat));
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
            return new TypeSafeExpressionValueResolver((String) value, dataType);
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
            return new TypeSafeExpressionValueResolver((String) value, dataType);
        }
        else
        {
            return new StaticValueResolver(doParseDate(element, attributeName, DATE_FORMAT, defaultValue));
        }
    }

    private static void addNestedProcessorResolver(ResolverSet resolverSet, ParameterModel parameterModel, List<MessageProcessor> nestedProcessors)
    {
        List<ValueResolver<NestedProcessor>> nestedProcessorResolvers = new ArrayList<>(nestedProcessors.size());
        for (MessageProcessor nestedProcessor : nestedProcessors)
        {
            nestedProcessorResolvers.add(new NestedProcessorValueResolver(nestedProcessor));
        }

        if (nestedProcessors.size() == 1 && parameterModel.getType().getQualifier() != DataQualifier.LIST)
        {
            resolverSet.add(parameterModel, new NestedProcessorValueResolver(nestedProcessors.get(0)));
        }
        else
        {
            resolverSet.add(parameterModel, CollectionValueResolver.of(ArrayList.class, nestedProcessorResolvers));
        }
    }

    private static void parseElementDescriptorChilds(Element element, BeanDefinitionBuilder builder)
    {
        ManagedList<BeanDefinition> managedChilds = getChildElements(element).stream()
                .map(XmlExtensionParserUtils::toElementDescriptorBeanDefinition)
                .collect(Collectors.toCollection(() -> new ManagedList<>()));

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

    private static boolean isExpression(Object value, TemplateParser parser)
    {
        return value instanceof String && parser.isContainsTemplate((String) value);
    }

    private static ValueResolver<?> getValueResolver(ElementDescriptor element, ParameterModel parameterModel)
    {
        if (parameterModel.getExpressionSupport() == ExpressionSupport.LITERAL)
        {
            return new StaticValueResolver<>(getAttributeValue(element, parameterModel.getName(), parameterModel.getDefaultValue()));
        }

        ValueResolver<?> resolver = parseParameter(element, parameterModel);
        return resolver == null ? new StaticValueResolver(null) : resolver;
    }
}
