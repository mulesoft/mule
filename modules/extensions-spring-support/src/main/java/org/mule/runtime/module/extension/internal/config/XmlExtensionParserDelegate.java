/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static org.mule.metadata.java.utils.JavaTypeUtils.getGenericTypeAt;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.metadata.utils.MetadataTypeUtils.getSingleAnnotation;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import static org.mule.runtime.module.extension.internal.util.NameUtils.pluralize;
import static org.mule.runtime.module.extension.internal.util.NameUtils.singularize;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.ATTRIBUTE_NAME_KEY;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.ATTRIBUTE_NAME_VALUE;
import static org.mule.runtime.module.extension.internal.xml.SchemaConstants.CONFIG_ATTRIBUTE;
import static org.springframework.util.xml.DomUtils.getChildElements;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.metadata.java.annotation.GenericTypesAnnotation;
import org.mule.runtime.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.runtime.config.spring.parsers.generic.AutoIdUtils;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.TemplateParser;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.introspection.BasicTypeMetadataVisitor;
import org.mule.runtime.module.extension.internal.introspection.SubTypesMappingContainer;
import org.mule.runtime.module.extension.internal.runtime.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.ObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.CollectionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExpressionFunctionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.MapValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.NestedProcessorValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.RegistryLookupValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.TypeSafeExpressionValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;
import org.mule.runtime.module.extension.internal.util.NameUtils;

import com.google.common.collect.ImmutableMap;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Helper class with methods for XML parsers capable of handling objects described by the Extensions API
 *
 * @since 4.0
 */
final class XmlExtensionParserDelegate
{

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss";
    private static final String CALENDAR_FORMAT = "yyyy-MM-dd'T'hh:mm:ssX";
    private static final TemplateParser parser = TemplateParser.createMuleStyleParser();
    private static final ConversionService conversionService = new DefaultConversionService();

    private Map<Class<?>, Object> infrastructureParameters = new HashMap<>();
    private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    private SubTypesMappingContainer subTypesMapping = new SubTypesMappingContainer(Collections.emptyMap());

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
    String parseConfigName(Element element, BeanDefinitionBuilder builder)
    {
        return parseName(element, "config", builder);
    }

    void parseConnectionProviderName(Element element, BeanDefinitionBuilder builder)
    {
        parseName(element, "connection-provider", builder);
    }

    String parseName(Element element, String type, BeanDefinitionBuilder builder)
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
    void setNoRecurseOnDefinition(BeanDefinition definition)
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
    ValueResolver parseParameter(ElementDescriptor element, ParameterModel parameterModel)
    {
        if (parameterModel.getExpressionSupport() == ExpressionSupport.LITERAL)
        {
            return new StaticValueResolver<>(getAttributeValue(element, parameterModel.getName(), parameterModel.getDefaultValue()));
        }

        if (!subTypesMapping.getSubTypes(parameterModel.getType()).isEmpty())
        {
            List<MetadataType> subtypes = subTypesMapping.getSubTypes(parameterModel.getType());
            Optional<MetadataType> subTypeChildElement = subtypes.stream()
                    .filter(s -> element.getChildByName(hyphenize(IntrospectionUtils.getAliasName(s))) != null)
                    .findFirst();

            if (subTypeChildElement.isPresent())
            {
                return parseElement(element, parameterModel.getName(),
                                    hyphenize(IntrospectionUtils.getAliasName(subTypeChildElement.get())),
                                    subTypeChildElement.get(),
                                    parameterModel.getDefaultValue());
            }
        }

        return parseElement(element, parameterModel.getName(), hyphenize(parameterModel.getName()),
                            parameterModel.getType(), parameterModel.getDefaultValue());
    }

    /**
     * Parses an {@code element} which is assumed to be a representation of the given
     * {@code metadataType}. It then returns a {@link ValueResolver} which will provide
     * the actual value extracted from the {@code element}
     *
     * @param element      a {@link ElementDescriptor}
     * @param fieldName    the name of the variable in which the value is to be stored
     * @param metadataType the {@link MetadataType} that describes the type of the value to be extracted
     * @param defaultValue a default value in case that the {@code element} does not contain an actual value
     * @return a {@link ValueResolver}
     */
    ValueResolver parseElement(final ElementDescriptor element,
                               final String fieldName,
                               final String elementName,
                               final MetadataType metadataType,
                               final Object defaultValue)
    {
        final String singularName = singularize(elementName);
        final ValueHolder<ValueResolver> resolverReference = new ValueHolder<>();

        metadataType.accept(new MetadataTypeVisitor()
        {
            /**
             * An attribute of a generic type
             */
            @Override
            protected void defaultVisit(MetadataType metadataType)
            {
                resolverReference.set(getResolverFromValue(getAttributeValue(element, fieldName, defaultValue), metadataType));
            }

            /**
             * A collection type. Might be defined in an inner element or referenced
             * from an attribute
             */
            @Override
            public void visitArrayType(ArrayType arrayType)
            {
                resolverReference.set(parseCollection(element, fieldName, elementName, singularName, defaultValue, arrayType));
            }

            /**
             * A map type. Might be defined in an inner element or referenced
             * from an attribute
             */
            @Override
            public void visitDictionary(DictionaryType dictionaryType)
            {
                final String pluralName = pluralize(elementName);
                String parentName;
                String childname;
                if (StringUtils.equals(pluralName, elementName))
                {
                    parentName = elementName;
                    childname = singularName;
                }
                else
                {
                    parentName = pluralName;
                    childname = elementName;
                }

                resolverReference.set(parseMap(element, fieldName, parentName, childname, defaultValue, dictionaryType));
            }

            @Override
            public void visitObject(ObjectType objectType)
            {
                resolverReference.set(parsePojo(element, fieldName, elementName, objectType, defaultValue));
            }

            @Override
            public void visitDateTime(DateTimeType dateTimeType)
            {
                resolverReference.set(parseCalendar(element, fieldName, dateTimeType, defaultValue));
            }

            @Override
            public void visitDate(DateType dateType)
            {
                resolverReference.set(parseDate(element, fieldName, dateType, defaultValue));
            }
        });

        return resolverReference.get();
    }

    /**
     * Creates a {@link BeanDefinition} which generates a {@link ElementDescriptor}
     * that describes the given {@code element}
     *
     * @param element the {@link Element} you want to describe
     * @return a {@link BeanDefinition}
     */
    BeanDefinition toElementDescriptorBeanDefinition(Element element)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ElementDescriptor.class);
        builder.addConstructorArgValue(element.getLocalName());
        parseElementDescriptorAttributes(element, builder);
        parseElementDescriptorChilds(element, builder);
        builder.addConstructorArgValue(element);

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
    ResolverSet getResolverSet(ElementDescriptor element, List<ParameterModel> parameterModels) throws ConfigurationException
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
    ResolverSet getResolverSet(ElementDescriptor element, List<ParameterModel> parameterModels, Map<String, List<MessageProcessor>> nestedOperations) throws ConfigurationException
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
    Object getAttributeValue(ElementDescriptor element, String attributeName, Object defaultValue)
    {
        return element.hasAttribute(attributeName)
               ? element.getAttribute(attributeName)
               : defaultValue;
    }

    void parseConfigRef(Element element, BeanDefinitionBuilder builder)
    {
        String configRef = element.getAttribute(CONFIG_ATTRIBUTE);
        if (StringUtils.isBlank(configRef))
        {
            configRef = null;
        }

        builder.addConstructorArgValue(configRef);
    }

    ValueResolver parseCollectionAsInnerElement(ElementDescriptor collectionElement,
                                                String childElementName,
                                                final ArrayType collectionType)
    {
        final MetadataType itemsType = collectionType.getType();
        final List<ValueResolver<Object>> resolvers = new LinkedList<>();

        for (final ElementDescriptor item : collectionElement.getChildsByName(childElementName))
        {
            itemsType.accept(new MetadataTypeVisitor()
            {
                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    resolvers.add(getResolverFromValue(item.getAttribute(ATTRIBUTE_NAME_VALUE), itemsType));
                }

                @Override
                public void visitObject(ObjectType objectType)
                {
                    resolvers.add(new ObjectBuilderValueResolver(createObjectBuilder(objectType, item)));
                }
            });
        }

        return CollectionValueResolver.of(getType(collectionType), resolvers);
    }

    ValueResolver parseMapAsInnerElement(ElementDescriptor mapElement,
                                         String childElementName,
                                         DictionaryType mapType)
    {
        final MetadataType keyType = mapType.getKeyType();
        final MetadataType valueType = mapType.getValueType();
        final List<ValueResolver<Object>> keyResolvers = new LinkedList<>();
        final List<ValueResolver<Object>> valueResolvers = new LinkedList<>();

        for (final ElementDescriptor item : mapElement.getChildsByName(childElementName))
        {
            keyResolvers.add(getResolverFromValue(item.getAttribute(ATTRIBUTE_NAME_KEY), keyType));

            valueType.accept(new MetadataTypeVisitor()
            {
                @Override
                public void visitObject(ObjectType objectType)
                {
                    valueResolvers.add(parsePojo(item,
                                                 ATTRIBUTE_NAME_VALUE,
                                                 getTopLevelTypeName(objectType),
                                                 objectType,
                                                 null));
                }

                @Override
                public void visitArrayType(ArrayType arrayType)
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
                        resolver = parseCollectionAsInnerElement(item, itemName, arrayType);
                    }

                    valueResolvers.add(resolver);
                }

                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    valueResolvers.add(getResolverFromValue(item.getAttribute(ATTRIBUTE_NAME_VALUE), valueType));
                }
            });
        }

        return MapValueResolver.of(getType(mapType), keyResolvers, valueResolvers);
    }

    ValueResolver parseCollection(ElementDescriptor element,
                                  String fieldName,
                                  String parentElementName,
                                  String childElementName,
                                  Object defaultValue,
                                  ArrayType collectionType)
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

    ValueResolver parseMap(ElementDescriptor element,
                           String fieldName,
                           String parentElementName,
                           String childElementName,
                           Object defaultValue,
                           DictionaryType mapDataType)
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

    ValueResolver getResolverFromAttribute(ElementDescriptor element, String attributeName, MetadataType metadataType, Object defaultValue)
    {
        return getResolverFromValue(getAttributeValue(element, attributeName, defaultValue), metadataType);
    }

    <T> T getInfrastructureParameter(Class<T> type)
    {
        return (T) infrastructureParameters.get(type);
    }

    public void setInfrastructureParameters(Map<Class<?>, Object> infrastructureParameters)
    {
        this.infrastructureParameters = infrastructureParameters;
    }

    private ValueResolver getResolverFromValue(final Object value, final MetadataType expectedType)
    {
        if (isExpressionFunction(expectedType) && value != null)
        {
            return new ExpressionFunctionValueResolver<>((String) value, getGenericTypeAt((ObjectType) expectedType, 1, typeLoader).get());
        }

        final Class<Object> expectedClass = getType(expectedType);
        if (isExpression(value, parser))
        {
            return new TypeSafeExpressionValueResolver((String) value, expectedClass);
        }

        if (value != null)
        {
            final ValueHolder<ValueResolver> resolverValueHolder = new ValueHolder<>();
            expectedType.accept(new BasicTypeMetadataVisitor()
            {
                @Override
                protected void visitBasicType(MetadataType metadataType)
                {
                    if (conversionService.canConvert(value.getClass(), expectedClass))
                    {
                        resolverValueHolder.set(new StaticValueResolver(conversionService.convert(value, expectedClass)));
                    }
                    else
                    {
                        defaultVisit(metadataType);
                    }
                }

                @Override
                protected void defaultVisit(MetadataType metadataType)
                {
                    resolverValueHolder.set(new RegistryLookupValueResolver(value.toString()));
                }
            });

            return resolverValueHolder.get();
        }

        return null;
    }

    private boolean isExpressionFunction(MetadataType metadataType)
    {
        if (!Function.class.isAssignableFrom(getType(metadataType)))
        {
            return false;
        }

        GenericTypesAnnotation generics = getSingleAnnotation(metadataType, GenericTypesAnnotation.class).orElse(null);
        if (generics == null)
        {
            return false;
        }

        if (generics.getGenericTypes().size() != 2)
        {
            return false;
        }

        final String genericClassName = generics.getGenericTypes().get(0);
        try
        {
            return MuleEvent.class.isAssignableFrom(ClassUtils.getClass(genericClassName));
        }
        catch (ClassNotFoundException e)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage("Could not load class " + genericClassName), e);
        }
    }

    /**
     * Parses a pojo which type is described by {@code pojoType},
     * recursively moving through the pojo's properties.
     *
     * @param element          the XML element which has the bean as a child
     * @param fieldName        the name of the field in which the parsed pojo is going to be assigned
     * @param childElementName the name of the the bean's top level XML element
     * @param pojoType         an {@link ObjectType} describing the bean's type
     * @return a {@link BeanDefinition} if the bean could be parsed, {@code null}
     * if the bean is not present on the XML definition
     */
    private ValueResolver parsePojo(ElementDescriptor element,
                                    String fieldName,
                                    String childElementName,
                                    ObjectType pojoType,
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

        // check if this was supplied as an infrastructure parameter
        Object infrastructure = getInfrastructureParameter(getType(pojoType));
        if (infrastructure != null)
        {
            return new StaticValueResolver<>(infrastructure);
        }

        if (childElement != null)
        {
            if (!StringUtils.isBlank(childElement.getAttribute("name")))
            {
                throw new IllegalModelDefinitionException(String.format("Element %s is not allowed to have a [name] attribute", childElement.getName()));
            }

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

    private ValueResolver getPojoValueResolver(ObjectType objectType, ElementDescriptor element)
    {
        return new ObjectBuilderValueResolver(createObjectBuilder(objectType, element));
    }

    protected ObjectBuilder<Object> createObjectBuilder(ObjectType objectType, ElementDescriptor element)
    {
        final Class<Object> objectClass = getType(objectType);
        final ObjectBuilder builder = new DefaultObjectBuilder(objectClass);

        for (ObjectFieldType objectField : objectType.getFields())
        {
            final MetadataType fieldType = objectField.getValue();
            final String parameterName = objectField.getKey().getName().getLocalPart();

            ValueResolver resolver = getResolverFromAttribute(element, parameterName, fieldType, null);
            if (resolver == null && fieldType instanceof ObjectType)
            {
                ElementDescriptor childElement = element.getChildByName(hyphenize(parameterName));
                if (childElement != null)
                {
                    ObjectBuilder childBuilder = createObjectBuilder((ObjectType) fieldType, childElement);
                    resolver = new ObjectBuilderValueResolver(childBuilder);
                }
            }

            if (resolver != null)
            {
                Field field = IntrospectionUtils.getFieldByAlias(objectClass, parameterName, getType(fieldType));
                if (field != null)
                {
                    builder.addPropertyResolver(field, resolver);
                }
            }
        }

        return builder;
    }

    private Date doParseDate(ElementDescriptor element,
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

    private ValueResolver parseCalendar(ElementDescriptor element, String attributeName, DateTimeType dataType, Object defaultValue)
    {
        Object value = getAttributeValue(element, attributeName, defaultValue);
        if (isExpression(value, parser))
        {
            return new TypeSafeExpressionValueResolver((String) value, getType(dataType));
        }

        Date date = doParseDate(element, attributeName, CALENDAR_FORMAT, defaultValue);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return new StaticValueResolver(calendar);
    }

    private ValueResolver parseDate(ElementDescriptor element, String attributeName, DateType dateType, Object defaultValue)
    {
        Object value = getAttributeValue(element, attributeName, defaultValue);
        if (isExpression(value, parser))
        {
            return new TypeSafeExpressionValueResolver((String) value, getType(dateType));
        }
        else
        {
            return new StaticValueResolver(doParseDate(element, attributeName, DATE_FORMAT, defaultValue));
        }
    }

    private void addNestedProcessorResolver(ResolverSet resolverSet, ParameterModel parameterModel, List<MessageProcessor> nestedProcessors)
    {
        List<ValueResolver<NestedProcessor>> nestedProcessorResolvers = nestedProcessors.stream()
                .map(NestedProcessorValueResolver::new)
                .collect(Collectors.toList());

        if (nestedProcessors.size() == 1 && !(parameterModel.getType() instanceof ArrayType))
        {
            resolverSet.add(parameterModel, new NestedProcessorValueResolver(nestedProcessors.get(0)));
        }
        else
        {
            resolverSet.add(parameterModel, CollectionValueResolver.of(ArrayList.class, nestedProcessorResolvers));
        }
    }

    private void parseElementDescriptorChilds(Element element, BeanDefinitionBuilder builder)
    {
        ManagedList<BeanDefinition> managedChilds = getChildElements(element).stream()
                .map(this::toElementDescriptorBeanDefinition)
                .collect(Collectors.toCollection(() -> new ManagedList<>()));

        builder.addConstructorArgValue(managedChilds);
    }

    private void parseElementDescriptorAttributes(Element element, BeanDefinitionBuilder builder)
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

    private boolean isExpression(Object value, TemplateParser parser)
    {
        return value instanceof String && parser.isContainsTemplate((String) value);
    }

    private ValueResolver<?> getValueResolver(ElementDescriptor element, ParameterModel parameterModel)
    {
        if (parameterModel.getExpressionSupport() == ExpressionSupport.LITERAL)
        {
            return new StaticValueResolver<>(getAttributeValue(element, parameterModel.getName(), parameterModel.getDefaultValue()));
        }

        ValueResolver<?> resolver = parseParameter(element, parameterModel);
        return resolver == null ? new StaticValueResolver(null) : resolver;
    }

    void setSubTypesMapping(SubTypesMappingContainer subTypesMapping)
    {
        this.subTypesMapping = subTypesMapping;
    }
}
