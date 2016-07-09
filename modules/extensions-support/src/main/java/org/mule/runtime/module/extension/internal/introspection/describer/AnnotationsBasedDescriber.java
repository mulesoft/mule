/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.THREADING_PROFILE_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.TLS_ATTRIBUTE_NAME;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.addConfigTypeModelProperty;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.addConnectionTypeModelProperty;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getExtension;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getMemberName;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseDisplayAnnotations;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseMetadataAnnotations;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseParameters;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExposedFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getExpressionSupport;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getInterfaceGenerics;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnAttributesType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodReturnType;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getOperationMethods;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getParameterContainers;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getParameterFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSourceName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSuperClassGenerics;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isMultiLevelMetadataKeyId;
import org.mule.api.MuleVersion;
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
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.ExtensionOf;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.NoRef;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ComponentDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasConnectionProviderDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasModelProperties;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.HasSourceDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OutputDeclarer;
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
import org.mule.runtime.extension.api.introspection.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.manifest.DescriberManifest;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.introspection.version.VersionResolver;
import org.mule.runtime.module.extension.internal.metadata.MetadataScopeAdapter;
import org.mule.runtime.module.extension.internal.model.property.ExtendingOperationModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.NoReferencesModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.runtime.module.extension.internal.runtime.exception.DefaultExceptionEnricherFactory;
import org.mule.runtime.module.extension.internal.runtime.executor.ReflectiveOperationExecutorFactory;
import org.mule.runtime.module.extension.internal.runtime.source.DefaultSourceFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    private final Multimap<Class<?>, OperationDeclarer> operationDeclarers = LinkedListMultimap.create();
    private final Map<Class<?>, SourceDeclarer> sourceDeclarers = new HashMap<>();
    private final Map<Class<?>, ConnectionProviderDeclarer> connectionProviderDeclarers = new HashMap<>();

    /**
     * An ordered {@link List} used to locate a {@link FieldDescriber} that can handle
     * an specific {@link Field}
     */
    private List<FieldDescriber> fieldDescribers;

    public AnnotationsBasedDescriber(Class<?> extensionType, VersionResolver versionResolver)
    {
        checkArgument(extensionType != null, format("describer %s does not specify an extension type", getClass().getName()));
        this.extensionType = extensionType;
        this.versionResolver = versionResolver;
        typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(extensionType.getClassLoader());

        initialiseFieldDescribers();
    }

    private void initialiseFieldDescribers()
    {
        fieldDescribers = ImmutableList.of(new InfrastructureFieldDescriber(TlsContextFactory.class, TLS_ATTRIBUTE_NAME, typeLoader),
                                           new InfrastructureFieldDescriber(ThreadingProfile.class, THREADING_PROFILE_ATTRIBUTE_NAME, typeLoader),
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
                .withCategory(extension.category())
                .withMinMuleVersion(new MuleVersion(extension.minMuleVersion()))
                .describedAs(extension.description())
                .withExceptionEnricherFactory(getExceptionEnricherFactory(extensionType))
                .withModelProperty(new ImplementingTypeModelProperty(extensionType));

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

        configurationDeclarer.createdWith(new TypeAwareConfigurationFactory(configurationType, extensionType.getClassLoader()))
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
                throw new IllegalConfigurationModelDefinitionException(format("Configuration class '%s' cannot be the same class (nor a derivative) of any operation class '%s",
                                                                              configurationType.getName(), operationClass.getName()));
            }
        }
    }

    private void checkOperationIsNotAnExtension(Class<?> operationType)
    {
        if (operationType.isAssignableFrom(extensionType) || extensionType.isAssignableFrom(operationType))
        {
            throw new IllegalOperationModelDefinitionException(format("Operation class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
                                                                      operationType.getName(), extensionType.getName()));
        }
    }

    //TODO: MULE-9220: Add a Syntax validator which checks that a Source class doesn't try to declare operations, configs, etc
    private void declareMessageSource(HasSourceDeclarer declarer, Class<? extends Source> sourceType)
    {
        SourceDeclarer source = sourceDeclarers.get(sourceType);
        if (source != null)
        {
            declarer.withMessageSource(source);
            return;
        }

        source = declarer.withMessageSource(getSourceName(sourceType));

        List<Type> sourceGenerics = getSuperClassGenerics(sourceType, Source.class);

        if (sourceGenerics.size() != 2)
        {
            //TODO: MULE-9220: Add a syntax validator for this
            throw new IllegalModelDefinitionException(format("Message source class '%s' was expected to have 2 generic types " +
                                                             "(one for the Payload type and another for the Attributes type) but %d were found",
                                                             sourceType.getName(), sourceGenerics.size()));
        }

        MetadataScopeAdapter metadataScope = new MetadataScopeAdapter(getMetadataScope(sourceType));
        MetadataResolverFactory metadataResolverFactory = getMetadataResolverFactory(metadataScope);

        source.sourceCreatedBy(new DefaultSourceFactory(sourceType))
                .withExceptionEnricherFactory(getExceptionEnricherFactory(sourceType))
                .withModelProperty(new ImplementingTypeModelProperty(sourceType))
                .withMetadataResolverFactory(metadataResolverFactory);

        declareOutputType(source, metadataScope, typeLoader.load(sourceGenerics.get(0)));
        declareOutputAttributesType(source, metadataScope, typeLoader.load(sourceGenerics.get(1)));
        if (metadataScope.isCustomScope())
        {
            declareMetadataKeyId(sourceType, source);
        }

        sourceDeclarers.put(sourceType, source);
        declareMetadataKeyId(sourceType, source);
        declareSingleParameters(getParameterFields(sourceType)
                                        .stream()
                                        .filter(field -> !isMultiLevelMetadataKeyId(field, field.getType(), typeLoader))
                                        .collect(toCollection(LinkedHashSet::new)),
                                source, MuleExtensionAnnotationParser::parseMetadataAnnotations);

        declareSourceConnection(sourceType, source);
        declareSourceConfig(sourceType, source);
        declareParameterGroups(sourceType, source);

        sourceDeclarers.put(sourceType, source);
    }

    private void declareSourceConfig(Class<? extends Source> sourceType, SourceDeclarer source)
    {
        getAnnotatedFields(sourceType, UseConfig.class).stream()
                .forEach(f -> addConfigTypeModelProperty(typeLoader.load(f.getDeclaringClass()), source));
    }

    private void declareSourceConnection(Class<? extends Source> sourceType, SourceDeclarer source)
    {
        getAnnotatedFields(sourceType, Connection.class).stream()
                .forEach(f -> addConnectionTypeModelProperty(typeLoader.load(f.getDeclaringClass()), source));
    }

    private void declareMetadataKeyId(Class<?> sourceType, SourceDeclarer source)
    {
        final List<Field> annotatedFields = getAnnotatedFields(sourceType, MetadataKeyId.class);

        if (!annotatedFields.isEmpty())
        {
            if (annotatedFields.size() > 1)
            {
                throw new IllegalModelDefinitionException(String.format("A Source cannot define more than one MetadataKeyId. Affecting Source: [%s]", sourceType.getSimpleName()));
            }

            source.withModelProperty(new MetadataKeyIdModelProperty(typeLoader.load(annotatedFields.get(0).getType())));
        }
    }

    private void declareAnnotatedParameters(Class<?> annotatedType, ParameterizedDeclarer parameterDeclarer)
    {
        declareSingleParameters(getAnnotatedFields(annotatedType, Parameter.class), parameterDeclarer);
        declareParameterGroups(annotatedType, parameterDeclarer);
    }

    private List<ParameterGroup> declareParameterGroups(Class<?> annotatedType, ParameterizedDeclarer parameterDeclarer)
    {
        List<ParameterGroup> groups = declareConfigurationParametersGroups(annotatedType, parameterDeclarer, null);
        if (!CollectionUtils.isEmpty(groups) && parameterDeclarer instanceof HasModelProperties)
        {
            ((HasModelProperties) parameterDeclarer).withModelProperty(new ParameterGroupModelProperty(groups));
        }
        return groups;
    }

    private Optional<ExceptionEnricherFactory> getExceptionEnricherFactory(AnnotatedElement element)
    {
        OnException onExceptionAnnotation = element.getAnnotation(OnException.class);
        if (onExceptionAnnotation != null)
        {
            return Optional.of(new DefaultExceptionEnricherFactory(onExceptionAnnotation.value()));
        }
        return Optional.empty();
    }

    private List<ParameterGroup> declareConfigurationParametersGroups(Class<?> annotatedType, ParameterizedDeclarer parameterDeclarer, ParameterGroup parent)
    {
        List<ParameterGroup> groups = new LinkedList<>();
        for (Field field : getParameterContainers(annotatedType, typeLoader))
        {
            //TODO: MULE-9220
            if (field.isAnnotationPresent(org.mule.runtime.extension.api.annotation.param.Optional.class))
            {
                throw new IllegalParameterModelDefinitionException(format("@%s can not be applied along with @%s. Affected field [%s] in [%s].",
                                                                          org.mule.runtime.extension.api.annotation.param.Optional.class.getSimpleName(),
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

                    group.addParameter(getField(field.getType(), getMemberName(parameter, parameter.getName())));
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
        Optional<DisplayModelProperty> parameterDisplayProperty = parameterDeclarer.getDeclaration().getModelProperty(DisplayModelProperty.class);

        DisplayModelPropertyBuilder builder = parameterDisplayProperty.isPresent()
                                              ? DisplayModelPropertyBuilder.create(parameterDisplayProperty.get())
                                              : DisplayModelPropertyBuilder.create();

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

    private Set<ParameterDeclarer> declareSingleParameters(Collection<Field> parameterFields,
                                                           ParameterizedDeclarer parameterizedDeclarer,
                                                           ModelPropertyContributor... contributors)
    {
        return parameterFields.stream()
                .map(field -> {
                    final ParameterDeclarer describe = getFieldDescriber(field).describe(field, parameterizedDeclarer);
                    stream(contributors).forEach(contributor -> contributor.contribute(field, describe));
                    return describe;
                })
                .collect(toCollection(LinkedHashSet::new));
    }

    private FieldDescriber getFieldDescriber(Field field)
    {
        Optional<FieldDescriber> describer = fieldDescribers.stream()
                .filter(fieldDescriber -> fieldDescriber.accepts(field))
                .findFirst();

        if (describer.isPresent())
        {
            return describer.get();
        }

        throw new IllegalModelDefinitionException(format(
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
        if (operationDeclarers.containsKey(actingClass))
        {
            operationDeclarers.get(actingClass).forEach(declarer::withOperation);
            return;
        }

        checkOperationIsNotAnExtension(actingClass);

        for (Method operationMethod : getOperationMethods(actingClass))
        {
            MetadataScopeAdapter metadataScope = new MetadataScopeAdapter(getMetadataScope(operationMethod));
            MetadataResolverFactory metadataResolverFactory = getMetadataResolverFactory(metadataScope);

            final OperationDeclarer operation = declarer.withOperation(operationMethod.getName())
                    .withModelProperty(new ImplementingMethodModelProperty(operationMethod))
                    .executorsCreatedBy(new ReflectiveOperationExecutorFactory<>(actingClass, operationMethod))
                    .withExceptionEnricherFactory(getExceptionEnricherFactory(operationMethod))
                    .withMetadataResolverFactory(metadataResolverFactory);

            declareOutputType(operation, metadataScope, getMethodReturnType(operationMethod, typeLoader));
            declareOutputAttributesType(operation, metadataScope, getMethodReturnAttributesType(operationMethod, typeLoader));
            if (metadataScope.isCustomScope())
            {
                declareOperationMetadataKeyId(operationMethod, operation);
            }

            declareOperationParameters(operationMethod, operation, metadataScope);
            calculateExtendedTypes(actingClass, operationMethod, operation);

            operationDeclarers.put(actingClass, operation);
        }
    }

    private void declareOperationMetadataKeyId(Method method, OperationDeclarer operation)
    {
        stream(method.getParameters())
                .filter(p -> p.isAnnotationPresent(MetadataKeyId.class))
                .findFirst()
                .ifPresent(p -> operation.withModelProperty(new MetadataKeyIdModelProperty(typeLoader.load(p.getType()))));
    }

    private MetadataResolverFactory getMetadataResolverFactory(MetadataScopeAdapter scope)
    {
        return scope.isCustomScope() ? new DefaultMetadataResolverFactory(scope.getKeysResolver(), scope.getContentResolver(),
                                                                          scope.getOutputResolver(), scope.getAttributesResolver())
                                     : new NullMetadataResolverFactory();

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

    /**
     * Checks if the method is annotated with {@link MetadataScope}, if not looks whether the
     * operation class containing the method is annotated or not. And lastly, if no annotation
     * was found so far, checks if the extension class is annotated.
     */
    private MetadataScope getMetadataScope(Method method)
    {
        MetadataScope scope = method.getAnnotation(MetadataScope.class);
        return scope != null ? scope : getMetadataScope(method.getDeclaringClass());
    }

    /**
     * Checks if the {@link ComponentModel Component's} type is annotated with {@link MetadataScope},
     * if it doesn't then looks if the {@link ExtensionModel Extension's} type is annotated.
     */
    private MetadataScope getMetadataScope(Class<?> componentClass)
    {
        MetadataScope scope = getAnnotation(componentClass, MetadataScope.class);
        return scope != null ? scope : getAnnotation(extensionType, MetadataScope.class);
    }

    private <T> void declareConnectionProvider(HasConnectionProviderDeclarer declarer, Class<T> providerClass)
    {
        ConnectionProviderDeclarer providerDeclarer = connectionProviderDeclarers.get(providerClass);
        if (providerDeclarer != null)
        {
            declarer.withConnectionProvider(providerDeclarer);
            return;
        }

        String name = DEFAULT_CONNECTION_PROVIDER_NAME;
        String description = EMPTY;

        Alias aliasAnnotation = providerClass.getAnnotation(Alias.class);
        if (aliasAnnotation != null)
        {
            name = aliasAnnotation.value() + CUSTOM_CONNECTION_PROVIDER_SUFFIX;
            description = aliasAnnotation.description();
        }

        List<Class<?>> providerGenerics = getInterfaceGenerics(providerClass, ConnectionProvider.class);

        if (providerGenerics.size() != 1)
        {
            //TODO: MULE-9220: Add a syntax validator for this
            throw new IllegalConnectionProviderModelDefinitionException(format("Connection provider class '%s' was expected to have 1 generic type " +
                                                                               "(for the connection type) but %d were found",
                                                                               providerClass.getName(), providerGenerics.size()));
        }

        providerDeclarer = declarer.withConnectionProvider(name)
                .describedAs(description)
                .createdWith(new DefaultConnectionProviderFactory<>(providerClass, extensionType.getClassLoader()))
                .whichGivesConnectionsOfType(providerGenerics.get(0))
                .withModelProperty(new ImplementingTypeModelProperty(providerClass));

        connectionProviderDeclarers.put(providerClass, providerDeclarer);
        declareAnnotatedParameters(providerClass, providerDeclarer);
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

    private void declareOperationParameters(Method method, OperationDeclarer operation, MetadataScopeAdapter metadataScope)
    {
        List<ParsedParameter> descriptors = parseParameters(method, typeLoader);

        //TODO: MULE-9220
        checkAnnotationsNotUsedMoreThanOnce(method, operation, UseConfig.class, Connection.class, MetadataKeyId.class, Content.class);

        for (ParsedParameter parsedParameter : descriptors)
        {
            if (parsedParameter.isAdvertised())
            {
                ParameterDeclarer parameter = parsedParameter.isRequired()
                                              ? operation.withRequiredParameter(parsedParameter.getName())
                                              : operation.withOptionalParameter(parsedParameter.getName()).defaultingTo(parsedParameter.getDefaultValue());

                parameter = parsedParameter.isAnnotationPresent(Content.class)
                            ? declareContentType(parameter, metadataScope, parsedParameter.getType())
                            : parameter.ofType(parsedParameter.getType());

                parameter.withExpressionSupport(getExpressionSupport(parsedParameter.getAnnotation(Expression.class)));
                parameter.describedAs(EMPTY);

                addTypeRestrictions(parameter, parsedParameter);
                DisplayModelProperty displayModelProperty = parseDisplayAnnotations(parsedParameter, parsedParameter.getName());
                if (displayModelProperty != null)
                {
                    parameter.withModelProperty(displayModelProperty);
                }

                parseMetadataAnnotations(parsedParameter, parameter);

                if (parsedParameter.isAnnotationPresent(NoRef.class))
                {
                    parameter.withModelProperty(new NoReferencesModelProperty());
                }
            }

            if (parsedParameter.isAnnotationPresent(Connection.class))
            {
                addConnectionTypeModelProperty(parsedParameter.getType(), operation);
            }

            if (parsedParameter.isAnnotationPresent(UseConfig.class))
            {
                addConfigTypeModelProperty(parsedParameter.getType(), operation);
            }
        }
    }

    private ParameterDeclarer declareContentType(ParameterDeclarer parameter, MetadataScopeAdapter metadataScope, MetadataType type)
    {
        return metadataScope.hasContentResolver() ? parameter.ofDynamicType(type)
                                                  : parameter.ofType(type);
    }

    private OutputDeclarer declareOutputType(ComponentDeclarer component, MetadataScopeAdapter metadataScope, MetadataType type)
    {
        return metadataScope.hasOutputResolver() ? component.withOutput().ofDynamicType(type)
                                                 : component.withOutput().ofType(type);
    }

    private OutputDeclarer declareOutputAttributesType(ComponentDeclarer component, MetadataScopeAdapter metadataScope, MetadataType type)
    {
        return metadataScope.hasAttributesResolver() ? component.withOutputAttributes().ofDynamicType(type)
                                                     : component.withOutputAttributes().ofType(type);
    }

    private void checkAnnotationsNotUsedMoreThanOnce(Method method, OperationDeclarer operation, Class<? extends Annotation>... annotations)
    {
        stream(annotations).forEach(annotation -> {
            List<java.lang.reflect.Parameter> annotatedParameters =
                    stream(method.getParameters())
                            .filter(parameter -> parameter.isAnnotationPresent(annotation))
                            .collect(new ImmutableListCollector<>());

            if (annotatedParameters.size() > 1)
            {
                throw new IllegalModelDefinitionException(format("Method [%s] defined in Class [%s] of extension [%s] uses the annotation @%s more than once",
                                                                 method.getName(),
                                                                 method.getDeclaringClass(),
                                                                 operation.getDeclaration().getName(),
                                                                 annotation.getSimpleName()));
            }
        });
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
