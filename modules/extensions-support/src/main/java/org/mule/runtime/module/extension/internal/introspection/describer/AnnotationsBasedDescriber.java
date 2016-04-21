/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer;

import static java.util.Arrays.stream;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.THREADING_PROFILE_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TLS_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getExtension;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getMemberName;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseDisplayAnnotations;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseMetadataAnnotations;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseRepeatableAnnotation;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExposedFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getInterfaceGenerics;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMetadataType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getOperationMethods;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getParameterFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getParameterGroupFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSourceName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSuperClassGenerics;
import org.mule.extension.api.annotation.Import;
import org.mule.extension.api.annotation.ImportedTypes;
import org.mule.extension.api.introspection.property.ImportedTypesModelProperty;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.core.util.ArrayUtils;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.core.util.collection.ImmutableMapCollector;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.ExtensionOf;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.SubTypesMapping;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasConnectionProviderDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasModelProperties;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasSourceDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.introspection.property.DisplayModelProperty;
import org.mule.runtime.extension.api.introspection.property.DisplayModelPropertyBuilder;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.extension.api.manifest.DescriberManifest;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.introspection.version.VersionResolver;
import org.mule.runtime.module.extension.internal.model.property.ConfigTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ExtendingOperationModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.runtime.module.extension.internal.runtime.exception.DefaultExceptionEnricherFactory;
import org.mule.runtime.module.extension.internal.runtime.executor.ReflectiveOperationExecutorFactory;
import org.mule.runtime.module.extension.internal.runtime.source.DefaultSourceFactory;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link Describer} which generates a {@link ExtensionDeclarer} by
 * scanning annotations on a type provided in the constructor
 *
 * @since 3.7.0
 */
public final class AnnotationsBasedDescriber implements Describer
{

    /**
     * The ID which represents {@code this} {@link Describer} in a
     * {@link DescriberManifest}
     */
    public static final String DESCRIBER_ID = "annotations";

    /**
     * A {@link DescriberManifest} property key which points to the class
     * which should be introspected by instances of this class
     */
    public static final String TYPE_PROPERTY_NAME = "type";

    public static final String DEFAULT_CONNECTION_PROVIDER_NAME = "connection";
    private static final String CUSTOM_CONNECTION_PROVIDER_SUFFIX = "-" + DEFAULT_CONNECTION_PROVIDER_NAME;

    private final Class<?> extensionType;
    private final VersionResolver versionResolver;
    private final ClassTypeLoader typeLoader;

    /**
     * An ordered {@link List} used to locate a {@link FieldDescriber} that can handle
     * an specific {@link Field}
     */
    private List<FieldDescriber> fieldDescribers;

    public AnnotationsBasedDescriber(Class<?> extensionType, VersionResolver versionResolver)
    {
        checkArgument(extensionType != null, String.format("describer %s does not specify an extension type", getClass().getName()));
        this.extensionType = extensionType;
        this.versionResolver = versionResolver;
        typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(extensionType.getClassLoader());

        initialiseFieldDescribers();
    }

    private void initialiseFieldDescribers()
    {
        fieldDescribers = ImmutableList.of(new InfrastructureFieldDescriber(TlsContextFactory.class, TLS_ATTRIBUTE_NAME),
                                           new InfrastructureFieldDescriber(ThreadingProfile.class, THREADING_PROFILE_ATTRIBUTE_NAME),
                                           new DefaultFieldDescriber(typeLoader));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ExtensionDeclarer describe(DescribingContext context)
    {
        Extension extension = getExtension(extensionType);
        ExtensionDeclarer declaration = context.getExtensionDeclarer()
                .named(extension.name())
                .onVersion(getVersion(extension))
                .fromVendor(extension.vendor())
                .describedAs(extension.description())
                .withExceptionEnricherFactory(getExceptionEnricherFactory(extensionType))
                .withModelProperty(new ImplementingTypeModelProperty(extensionType));

        declareSubTypesMapping(declaration, extensionType);
        declareImportedTypes(declaration, extensionType);
        declareConfigurations(declaration, extensionType);
        declareOperations(declaration, extensionType);
        declareConnectionProviders(declaration, extensionType);
        declareMessageSources(declaration, extensionType);

        return declaration;
    }

    private String getVersion(Extension extension)
    {
        return versionResolver.resolveVersion(extension);
    }

    private void declareSubTypesMapping(ExtensionDeclarer declaration, Class<?> extensionType)
    {
        List<SubTypeMapping> typeMappings = parseRepeatableAnnotation(extensionType, SubTypeMapping.class, c -> ((SubTypesMapping) c).value());

        if (!typeMappings.isEmpty())
        {
            Map<MetadataType, List<MetadataType>> subTypesMap = typeMappings.stream().collect(
                    new ImmutableMapCollector<>(mapping -> getMetadataType(mapping.baseType(), typeLoader),
                                                mapping -> stream(mapping.subTypes())
                                                        .map(subType -> getMetadataType(subType, typeLoader))
                                                        .collect(new ImmutableListCollector<>())));

            declaration.withModelProperty(new SubTypesModelProperty(subTypesMap));
        }
    }

    private void declareImportedTypes(ExtensionDeclarer declaration, Class<?> extensionType)
    {
        List<Import> importTypes = parseRepeatableAnnotation(extensionType, Import.class, c -> ((ImportedTypes) c).value());

        if (!importTypes.isEmpty())
        {
            Map<MetadataType, MetadataType> importedTypes = importTypes.stream().collect(
                    new ImmutableMapCollector<>(imports -> getMetadataType(imports.type(), typeLoader),
                                                imports -> getMetadataType(imports.from(), typeLoader)));

            declaration.withModelProperty(new ImportedTypesModelProperty(importedTypes));
        }
    }

    private void declareConfigurations(ExtensionDeclarer declaration, Class<?> extensionType)
    {
        Class<?>[] configurationClasses = getConfigurationClasses(extensionType);
        if (ArrayUtils.isEmpty(configurationClasses))
        {
            declareConfiguration(declaration, extensionType, extensionType);
        }
        else
        {
            for (Class<?> configurationClass : configurationClasses)
            {
                declareConfiguration(declaration, extensionType, configurationClass);
            }
        }
    }

    private Class<?>[] getConfigurationClasses(Class<?> extensionType)
    {
        Configurations configs = extensionType.getAnnotation(Configurations.class);
        return configs == null ? ArrayUtils.EMPTY_CLASS_ARRAY : configs.value();
    }

    private void declareMessageSources(HasSourceDeclarer declarer, Class<?> extensionType)
    {
        Sources sources = extensionType.getAnnotation(Sources.class);
        if (sources != null)
        {
            for (Class<? extends Source> declaringClass : sources.value())
            {
                declareMessageSource(declarer, declaringClass);
            }
        }
    }

    private void declareConfiguration(ExtensionDeclarer declaration, Class<?> extensionType, Class<?> configurationType)
    {
        checkConfigurationIsNotAnOperation(configurationType);
        ConfigurationDeclarer configurationDeclarer;

        Configuration configurationAnnotation = configurationType.getAnnotation(Configuration.class);
        if (configurationAnnotation != null)
        {
            configurationDeclarer = declaration.withConfig(configurationAnnotation.name()).describedAs(configurationAnnotation.description());
        }
        else
        {
            configurationDeclarer = declaration.withConfig(Extension.DEFAULT_CONFIG_NAME).describedAs(Extension.DEFAULT_CONFIG_DESCRIPTION);
        }

        configurationDeclarer.createdWith(new TypeAwareConfigurationFactory(configurationType))
                .withModelProperty(new ImplementingTypeModelProperty(configurationType));

        declareAnnotatedParameters(configurationType, configurationDeclarer);

        if (!extensionType.equals(configurationType))
        {
            declareOperations(configurationDeclarer, configurationType);
            declareMessageSources(configurationDeclarer, configurationType);
            declareConnectionProviders(configurationDeclarer, configurationType);
        }
    }

    private void checkConfigurationIsNotAnOperation(Class<?> configurationType)
    {
        Class<?>[] operationClasses = getOperationClasses(extensionType);
        for (Class<?> operationClass : operationClasses)
        {
            if (configurationType.isAssignableFrom(operationClass) || operationClass.isAssignableFrom(configurationType))
            {
                throw new IllegalConfigurationModelDefinitionException(String.format("Configuration class '%s' cannot be the same class (nor a derivative) of any operation class '%s",
                                                                                     configurationType.getName(), operationClass.getName()));
            }
        }
    }

    private void checkOperationIsNotAnExtension(Class<?> operationType)
    {
        if (operationType.isAssignableFrom(extensionType) || extensionType.isAssignableFrom(operationType))
        {
            throw new IllegalOperationModelDefinitionException(String.format("Operation class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
                                                                             operationType.getName(), extensionType.getName()));
        }
    }

    private void declareMessageSource(HasSourceDeclarer declarer, Class<? extends Source> sourceType)
    {
        //TODO: MULE-9220: Add a Syntax validator which checks that a Source class doesn't try to declare operations, configs, etc
        SourceDeclarer source = declarer.withMessageSource(getSourceName(sourceType));

        List<Type> sourceGenerics = getSuperClassGenerics(sourceType, Source.class);

        if (sourceGenerics.size() != 2)
        {
            //TODO: MULE-9220: Add a syntax validator for this
            throw new IllegalModelDefinitionException(String.format("Message source class '%s' was expected to have 2 generic types " +
                                                                    "(one for the Payload type and another for the Attributes type) but %d were found",
                                                                    sourceType.getName(), sourceGenerics.size()));
        }

        source.sourceCreatedBy(new DefaultSourceFactory(sourceType))
                .whichReturns(typeLoader.load(sourceGenerics.get(0)))
                .withAttributesOfType(typeLoader.load(sourceGenerics.get(1)))
                .withExceptionEnricherFactory(getExceptionEnricherFactory(sourceType))
                .withModelProperty(new ImplementingTypeModelProperty(sourceType))
                .withMetadataResolverFactory(getMetadataResolverFactoryFromClass(extensionType, sourceType));

        declareSingleParameters(getParameterFields(sourceType), source, (ModelPropertyContributor) MuleExtensionAnnotationParser::parseMetadataAnnotations);
        declareParameterGroups(sourceType, source);

    }

    private void declareAnnotatedParameters(Class<?> annotatedType, ParameterizedDeclarer parameterDeclarer)
    {
        declareSingleParameters(getParameterFields(annotatedType), parameterDeclarer);
        declareParameterGroups(annotatedType, parameterDeclarer);
    }

    private void declareParameterGroups(Class<?> annotatedType, ParameterizedDeclarer parameterDeclarer)
    {
        List<ParameterGroup> groups = declareConfigurationParametersGroups(annotatedType, parameterDeclarer, null);
        if (!CollectionUtils.isEmpty(groups) && parameterDeclarer instanceof HasModelProperties)
        {
            ((HasModelProperties) parameterDeclarer).withModelProperty(new ParameterGroupModelProperty(groups));
        }
    }

    private java.util.Optional<ExceptionEnricherFactory> getExceptionEnricherFactory(AnnotatedElement element)
    {
        OnException onExceptionAnnotation = element.getAnnotation(OnException.class);
        if (onExceptionAnnotation != null)
        {
            return java.util.Optional.of(new DefaultExceptionEnricherFactory(onExceptionAnnotation.value()));
        }
        return java.util.Optional.empty();
    }

    private List<ParameterGroup> declareConfigurationParametersGroups(Class<?> annotatedType, ParameterizedDeclarer parameterDeclarer, ParameterGroup parent)
    {
        List<ParameterGroup> groups = new LinkedList<>();
        for (Field field : getParameterGroupFields(annotatedType))
        {
            //TODO: MULE-9220
            if (field.isAnnotationPresent(Optional.class))
            {
                throw new IllegalParameterModelDefinitionException(String.format("@%s can not be applied along with @%s. Affected field [%s] in [%s].",
                                                                                 Optional.class.getSimpleName(),
                                                                                 org.mule.runtime.extension.api.annotation.ParameterGroup.class.getSimpleName(),
                                                                                 field.getName(),
                                                                                 annotatedType));
            }

            Set<ParameterDeclarer> parameters = declareSingleParameters(getExposedFields(field.getType()), parameterDeclarer);

            if (!parameters.isEmpty())
            {
                ParameterGroup group = new ParameterGroup(field.getType(), field);
                groups.add(group);

                for (ParameterDeclarer descriptor : parameters)
                {
                    ParameterDeclaration parameter = inheritGroupParentDisplayProperties(parent, field, group, descriptor);

                    group.addParameter(parameter.getName(), getField(field.getType(),
                                                                     getMemberName(parameter, parameter.getName()),
                                                                     getType(parameter.getType())));
                }

                List<ParameterGroup> childGroups = declareConfigurationParametersGroups(field.getType(), parameterDeclarer, group);
                if (!CollectionUtils.isEmpty(childGroups))
                {
                    group.addModelProperty(new ParameterGroupModelProperty(childGroups));
                }
            }
        }

        return groups;
    }

    private ParameterDeclaration inheritGroupParentDisplayProperties(ParameterGroup parent, Field field, ParameterGroup group, ParameterDeclarer parameterDeclarer)
    {
        ParameterDeclaration parameter = parameterDeclarer.getDeclaration();
        DisplayModelProperty parameterDisplayProperty = parameterDeclarer.getDeclaration().getModelProperty(DisplayModelProperty.class).orElse(null);

        DisplayModelPropertyBuilder builder = parameterDisplayProperty == null
                                              ? DisplayModelPropertyBuilder.create()
                                              : DisplayModelPropertyBuilder.create(parameterDisplayProperty);

        // Inherit parent placement model properties
        DisplayModelProperty groupDisplay;
        DisplayModelProperty parentDisplay = parent != null ? parent.getModelProperty(DisplayModelProperty.class).orElse(null) : null;
        if (parentDisplay != null)
        {
            builder.groupName(parentDisplay.getGroupName())
                    .tabName(parentDisplay.getTabName())
                    .order(parentDisplay.getOrder());

            groupDisplay = builder.build();
        }
        else
        {
            groupDisplay = parseDisplayAnnotations(field, field.getName(), builder);
        }

        if (groupDisplay != null)
        {
            parameterDeclarer.withModelProperty(groupDisplay);
            group.addModelProperty(groupDisplay);
        }
        return parameter;
    }

    private Set<ParameterDeclarer> declareSingleParameters(Collection<Field> parameterFields, ParameterizedDeclarer parameterDeclarer, ModelPropertyContributor... contributors)
    {
        return parameterFields.stream()
                .map(field -> {
                    final ParameterDeclarer describe = getFieldDescriber(field).describe(field, parameterDeclarer);
                    Arrays.stream(contributors).forEach(contributor -> contributor.contribute(field, describe));
                    return describe;
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private FieldDescriber getFieldDescriber(Field field)
    {
        java.util.Optional<FieldDescriber> describer = fieldDescribers.stream()
                .filter(fieldDescriber -> fieldDescriber.accepts(field))
                .findFirst();

        if (describer.isPresent())
        {
            return describer.get();
        }

        throw new IllegalModelDefinitionException(String.format(
                "Could not find a %s capable of parsing the field '%s' on class '%s'",
                FieldDescriber.class.getSimpleName(), field.getName(), field.getDeclaringClass().getName()));
    }

    private void declareOperations(HasOperationDeclarer declarer, Class<?> extensionType)
    {
        Class<?>[] operations = getOperationClasses(extensionType);
        for (Class<?> actingClass : operations)
        {
            declareOperation(declarer, actingClass);
        }
    }

    private Class<?>[] getOperationClasses(Class<?> extensionType)
    {
        Operations operations = extensionType.getAnnotation(Operations.class);
        return operations == null ? ArrayUtils.EMPTY_CLASS_ARRAY : operations.value();
    }

    private <T> void declareOperation(HasOperationDeclarer declarer, Class<T> actingClass)
    {
        checkOperationIsNotAnExtension(actingClass);

        for (Method method : getOperationMethods(actingClass))
        {
            OperationDeclarer operation = declarer.withOperation(method.getName())
                    .withModelProperty(new ImplementingMethodModelProperty(method))
                    .executorsCreatedBy(new ReflectiveOperationExecutorFactory<>(actingClass, method))
                    .whichReturns(IntrospectionUtils.getMethodReturnType(method, typeLoader))
                    .withAttributesOfType(IntrospectionUtils.getMethodReturnAttributesType(method, typeLoader))
                    .withExceptionEnricherFactory(getExceptionEnricherFactory(method))
                    .withMetadataResolverFactory(getMetadataResolverFactoryFromMethod(extensionType, method.getDeclaringClass(), method));

            declareOperationParameters(method, operation);
            calculateExtendedTypes(actingClass, method, operation);
        }
    }

    private MetadataResolverFactory getMetadataResolverFactory(MetadataScope scopeAnnotation) // (Class<?> extensionType, Class<?> declaringClass)
    {
        return scopeAnnotation == null ? new NullMetadataResolverFactory() :
               new DefaultMetadataResolverFactory(scopeAnnotation.keysResolver(),
                                                  scopeAnnotation.contentResolver(),
                                                  scopeAnnotation.outputResolver());
    }

    /**
     * Checks if the method is annotated with {@link MetadataScope}, if not looks whether the
     * operation class containing the method is annotated or not. And lastly, if no annotation
     * was found so far, checks if the extension class is annotated.
     */
    private MetadataResolverFactory getMetadataResolverFactoryFromMethod(Class<?> extensionType, Class<?> declaringClass, Method method)
    {
        MetadataScope scopeAnnotation = method.getAnnotation(MetadataScope.class);
        return scopeAnnotation != null ? getMetadataResolverFactory(scopeAnnotation) : getMetadataResolverFactoryFromClass(extensionType, declaringClass);
    }

    /**
     * Checks if the class (may be source or operation class) is annotated with {@link MetadataScope},
     * if it doesn't then looks if the extension class is annotated.
     */
    private MetadataResolverFactory getMetadataResolverFactoryFromClass(Class<?> extensionType, Class<?> declaringClass)
    {
        MetadataScope scopeAnnotation = IntrospectionUtils.getAnnotation(declaringClass, MetadataScope.class);
        scopeAnnotation = scopeAnnotation != null ? scopeAnnotation :
                          IntrospectionUtils.getAnnotation(extensionType, MetadataScope.class);
        return getMetadataResolverFactory(scopeAnnotation);
    }

    private void declareConnectionProviders(HasConnectionProviderDeclarer declarer, Class<?> extensionType)
    {
        Providers providers = extensionType.getAnnotation(Providers.class);
        if (providers != null)
        {
            for (Class<?> providerClass : providers.value())
            {
                declareConnectionProvider(declarer, providerClass);
            }
        }
    }

    private <T> void declareConnectionProvider(HasConnectionProviderDeclarer declarer, Class<T> providerClass)
    {
        String name = DEFAULT_CONNECTION_PROVIDER_NAME;
        String description = EMPTY;

        Alias aliasAnnotation = providerClass.getAnnotation(Alias.class);
        if (aliasAnnotation != null)
        {
            name = aliasAnnotation.value() + CUSTOM_CONNECTION_PROVIDER_SUFFIX;
            description = aliasAnnotation.description();
        }

        List<Class<?>> providerGenerics = getInterfaceGenerics(providerClass, ConnectionProvider.class);

        if (providerGenerics.size() != 2)
        {
            //TODO: MULE-9220: Add a syntax validator for this
            throw new IllegalConnectionProviderModelDefinitionException(String.format("Connection provider class '%s' was expected to have 2 generic types " +
                                                                                      "(one for the config type and another for the connection type) but %d were found",
                                                                                      providerClass.getName(), providerGenerics.size()));
        }

        ConnectionProviderDeclarer providerDescriptor = declarer.withConnectionProvider(name)
                .describedAs(description)
                .createdWith(new DefaultConnectionProviderFactory<>(providerClass))
                .forConfigsOfType(providerGenerics.get(0))
                .whichGivesConnectionsOfType(providerGenerics.get(1))
                .withModelProperty(new ImplementingTypeModelProperty(providerClass));

        declareAnnotatedParameters(providerClass, providerDescriptor);
    }

    private void calculateExtendedTypes(Class<?> actingClass, Method method, OperationDeclarer operation)
    {
        ExtensionOf extensionOf = method.getAnnotation(ExtensionOf.class);
        if (extensionOf == null)
        {
            extensionOf = actingClass.getAnnotation(ExtensionOf.class);
        }

        if (extensionOf != null)
        {
            operation.withModelProperty(new ExtendingOperationModelProperty(extensionOf.value()));
        }
        else if (isExtensible())
        {
            operation.withModelProperty(new ExtendingOperationModelProperty(extensionType));
        }
    }

    private boolean isExtensible()
    {
        return extensionType.getAnnotation(Extensible.class) != null;
    }

    private void declareOperationParameters(Method method, OperationDeclarer operation)
    {
        List<ParsedParameter> descriptors = MuleExtensionAnnotationParser.parseParameters(method, typeLoader);

        //TODO: MULE-9220
        checkAnnotationIsNotUsedMoreThanOnce(method, operation, UseConfig.class);
        checkAnnotationIsNotUsedMoreThanOnce(method, operation, Connection.class);

        for (ParsedParameter parsedParameter : descriptors)
        {
            final Class<?> parameterType = getType(parsedParameter.getType(), typeLoader.getClassLoader());

            if (parsedParameter.isAdvertised())
            {
                ParameterDeclarer parameter = parsedParameter.isRequired()
                                              ? operation.withRequiredParameter(parsedParameter.getName())
                                              : operation.withOptionalParameter(parsedParameter.getName()).defaultingTo(parsedParameter.getDefaultValue());

                parameter.withExpressionSupport(IntrospectionUtils.getExpressionSupport(parsedParameter.getAnnotation(Expression.class)));
                parameter.describedAs(EMPTY).ofType(parsedParameter.getType());
                addTypeRestrictions(parameter, parsedParameter);
                DisplayModelProperty displayModelProperty = parseDisplayAnnotations(parsedParameter, parsedParameter.getName());
                if (displayModelProperty != null)
                {
                    parameter.withModelProperty(displayModelProperty);
                }

                parseMetadataAnnotations(parsedParameter, parameter);

            }

            Connection connectionAnnotation = parsedParameter.getAnnotation(Connection.class);
            if (connectionAnnotation != null)
            {
                operation.withModelProperty(new ConnectionTypeModelProperty(parameterType));
            }

            UseConfig useConfig = parsedParameter.getAnnotation(UseConfig.class);
            if (useConfig != null)
            {
                operation.withModelProperty(new ConfigTypeModelProperty(parameterType));
            }
        }
    }

    private void checkAnnotationIsNotUsedMoreThanOnce(Method method, OperationDeclarer operation, Class annotationClass)
    {
        Stream<java.lang.reflect.Parameter> parametersStream =
                stream(method.getParameters())
                        .filter(parameter -> parameter.isAnnotationPresent(annotationClass));

        List<java.lang.reflect.Parameter> parameterList = parametersStream.collect(new ImmutableListCollector<>());

        if (parameterList.size() > 1)
        {
            throw new IllegalModelDefinitionException(String.format("Method [%s] defined in Class [%s] of extension [%s] uses the annotation @%s more than once",
                                                                    method.getName(),
                                                                    method.getDeclaringClass(),
                                                                    operation.getDeclaration().getName(),
                                                                    annotationClass.getSimpleName()));
        }
    }

    private void addTypeRestrictions(ParameterDeclarer parameter, ParsedParameter descriptor)
    {
        Class<?> restriction = descriptor.getTypeRestriction();
        if (restriction != null)
        {
            parameter.withModelProperty(new TypeRestrictionModelProperty<>(restriction));
        }
    }

    private interface ModelPropertyContributor
    {

        void contribute(AnnotatedElement annotatedElement, HasModelProperties descriptor);
    }
}
