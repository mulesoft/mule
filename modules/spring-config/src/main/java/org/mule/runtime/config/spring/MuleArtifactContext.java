/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.spring.parsers.generic.AutoIdUtils.uniqueValue;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.config.spring.dsl.api.xml.StaticXmlNamespaceInfo;
import org.mule.runtime.config.spring.dsl.api.xml.StaticXmlNamespaceInfoProvider;
import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.config.spring.dsl.api.xml.XmlNamespaceInfoProvider;
import org.mule.runtime.config.spring.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.spring.dsl.model.ComponentIdentifier;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.spring.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.config.spring.dsl.spring.BeanDefinitionFactory;
import org.mule.runtime.config.spring.editors.MulePropertyEditorRegistrar;
import org.mule.runtime.config.spring.processors.ContextExclusiveInjectorProcessor;
import org.mule.runtime.config.spring.processors.DiscardedOptionalBeanPostProcessor;
import org.mule.runtime.config.spring.processors.LifecycleStatePostProcessor;
import org.mule.runtime.config.spring.processors.MuleInjectorProcessor;
import org.mule.runtime.config.spring.processors.PostRegistrationActionsPostProcessor;
import org.mule.runtime.config.spring.util.LaxInstantiationStrategyWrapper;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.config.ConfigResource;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.collection.ImmutableListCollector;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.xml.dsl.api.property.XmlModelProperty;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <code>MuleArtifactContext</code> is a simple extension application context
 * that allows resources to be loaded from the Classpath of file system using the
 * MuleBeanDefinitionReader.
 */
public class MuleArtifactContext extends AbstractXmlApplicationContext
{

    /**
     * Indicates that XSD validation should be used (found no "DOCTYPE" declaration).
     */
    private static final int VALIDATION_XSD = 3;
    private static final ThreadLocal<MuleContext> currentMuleContext = new ThreadLocal<>();
    public static final String INNER_BEAN_PREFIX = "(inner bean)";

    private final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry = new ComponentBuildingDefinitionRegistry();
    private final OptionalObjectsController optionalObjectsController;
    private final Map<String, String> artifactProperties;
    private final ArtifactConfiguration artifactConfiguration;
    private ApplicationModel applicationModel;
    private MuleContext muleContext;
    private Resource[] artifactConfigResources;
    private BeanDefinitionFactory beanDefinitionFactory;
    private MuleXmlBeanDefinitionReader beanDefinitionReader;
    private final ServiceRegistry serviceRegistry = new SpiServiceRegistry();
    private boolean useNewParsingMechanism = true;
    protected final XmlApplicationParser xmlApplicationParser;
    private ArtifactType artifactType;

    /**
     * Parses configuration files creating a spring ApplicationContext which is used
     * as a parent registry using the SpringRegistry registry implementation to wraps
     * the spring ApplicationContext
     *
     * @param muleContext               the {@link MuleContext} that own this context
     * @param artifactConfiguration     the mule configuration defined programmatically
     * @param optionalObjectsController the {@link OptionalObjectsController} to use. Cannot be {@code null}  @see org.mule.runtime.config.spring.SpringRegistry
     * @since 3.7.0
     */
    public MuleArtifactContext(MuleContext muleContext, ConfigResource[] artifactConfigResources, ArtifactConfiguration artifactConfiguration, OptionalObjectsController optionalObjectsController, Map<String, String> artifactProperties, ArtifactType artifactType) throws BeansException
    {
        this(muleContext, convert(artifactConfigResources), artifactConfiguration, optionalObjectsController, artifactProperties, artifactType);
    }

    public MuleArtifactContext(MuleContext muleContext, Resource[] artifactConfigResources, ArtifactConfiguration artifactConfiguration, OptionalObjectsController optionalObjectsController, Map<String, String> artifactProperties, ArtifactType artifactType)
    {
        checkArgument(optionalObjectsController != null, "optionalObjectsController cannot be null");
        this.muleContext = muleContext;
        this.artifactConfigResources = artifactConfigResources;
        this.optionalObjectsController = optionalObjectsController;
        this.artifactProperties = artifactProperties;
        this.artifactType = artifactType;
        this.artifactConfiguration = artifactConfiguration;

        serviceRegistry.lookupProviders(ComponentBuildingDefinitionProvider.class).forEach(componentBuildingDefinitionProvider -> {
            componentBuildingDefinitionProvider.init(muleContext);
            componentBuildingDefinitionProvider.getComponentBuildingDefinitions().stream().forEach(componentBuildingDefinitionRegistry::register);
        });

        xmlApplicationParser = new XmlApplicationParser(new XmlServiceRegistry(serviceRegistry, muleContext));

        this.beanDefinitionFactory = new BeanDefinitionFactory(componentBuildingDefinitionRegistry);

        createApplicationModel();
        determineIfOnlyNewParsingMechanismCanBeUsed();
    }

    private void determineIfOnlyNewParsingMechanismCanBeUsed()
    {
        if (applicationModel.hasSpringConfig())
        {
            useNewParsingMechanism = false;
            return;
        }
        applicationModel.executeOnEveryComponentTree(componentModel -> {
            Optional<ComponentIdentifier> parentIdentifierOptional = ofNullable(componentModel.getParent())
                    .flatMap(parentComponentModel ->
                                     Optional.ofNullable(parentComponentModel.getIdentifier())
                    );
            if (!beanDefinitionFactory.hasDefinition(componentModel.getIdentifier(), parentIdentifierOptional))
            {
                useNewParsingMechanism = false;
            }
        });
    }

    private void createApplicationModel()
    {
        try
        {
            ArtifactConfig.Builder applicationConfigBuilder = new ArtifactConfig.Builder();
            applicationConfigBuilder.setApplicationProperties(this.artifactProperties);
            for (Resource springResource : artifactConfigResources)
            {
                Document document = getXmlDocument(springResource);
                ConfigLine mainConfigLine = xmlApplicationParser.parse(document.getDocumentElement()).get();
                applicationConfigBuilder.addConfigFile(new ConfigFile(getFilename(springResource), asList(mainConfigLine)));
            }
            applicationConfigBuilder.setApplicationName(muleContext.getConfiguration().getId());
            applicationModel = new ApplicationModel(applicationConfigBuilder.build(), artifactConfiguration, Optional.of(componentBuildingDefinitionRegistry));
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private String getFilename(Resource resource)
    {
        if (resource instanceof ByteArrayResource)
        {
            return resource.getDescription();
        }
        return resource.getFilename();
    }

    private Document getXmlDocument(Resource artifactResource)
    {
        try
        {
            Document document = new MuleDocumentLoader()
                    .loadDocument(new InputSource(artifactResource.getInputStream()), new DelegatingEntityResolver(Thread.currentThread().getContextClassLoader()), new DefaultHandler(), VALIDATION_XSD, true);
            return document;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory)
    {
        super.prepareBeanFactory(beanFactory);

        registerEditors(beanFactory);

        addBeanPostProcessors(beanFactory,
                              new MuleContextPostProcessor(muleContext),
                              new GlobalNamePostProcessor(),
                              new PostRegistrationActionsPostProcessor((MuleRegistryHelper) muleContext.getRegistry()),
                              new DiscardedOptionalBeanPostProcessor(optionalObjectsController, (DefaultListableBeanFactory) beanFactory),
                              new LifecycleStatePostProcessor(muleContext.getLifecycleManager().getState())
        );

        beanFactory.registerSingleton(OBJECT_MULE_CONTEXT, muleContext);
    }

    private void registerEditors(ConfigurableListableBeanFactory beanFactory)
    {
        MulePropertyEditorRegistrar registrar = new MulePropertyEditorRegistrar();
        registrar.setMuleContext(muleContext);
        beanFactory.addPropertyEditorRegistrar(registrar);
    }

    private void addBeanPostProcessors(ConfigurableListableBeanFactory beanFactory, BeanPostProcessor... processors)
    {
        for (BeanPostProcessor processor : processors)
        {
            beanFactory.addBeanPostProcessor(processor);
        }
    }

    private static Resource[] convert(ConfigResource[] resources)
    {
        Resource[] configResources = new Resource[resources.length];
        for (int i = 0; i < resources.length; i++)
        {
            ConfigResource resource = resources[i];
            if (resource.getUrl() != null)
            {
                configResources[i] = new UrlResource(resource.getUrl());
            }
            else
            {
                try
                {
                    configResources[i] = new ByteArrayResource(IOUtils.toByteArray(resource.getInputStream()), resource.getResourceName());
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        return configResources;
    }

    @Override
    protected Resource[] getConfigResources()
    {
        return addAll(artifactConfigResources);
    }

    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException
    {
        BeanDefinitionReader beanDefinitionReader = createBeanDefinitionReader(beanFactory);
        // Communicate mule context to parsers
        try
        {
            currentMuleContext.set(muleContext);
            if (useNewParsingMechanism)
            {
                applicationModel.executeOnEveryMuleComponentTree(componentModel -> {
                    if (componentModel.isRoot())
                    {
                        beanDefinitionFactory.resolveComponentRecursively(applicationModel.getRootComponentModel(), componentModel, beanFactory,
                                                                          (resolvedComponentModel, registry) -> {
                                                                              if (resolvedComponentModel.isRoot())
                                                                              {
                                                                                  String nameAttribute = resolvedComponentModel.getNameAttribute();
                                                                                  if (resolvedComponentModel.getIdentifier().equals(CONFIGURATION_IDENTIFIER))
                                                                                  {
                                                                                      nameAttribute = OBJECT_MULE_CONFIGURATION;
                                                                                  }
                                                                                  else if (nameAttribute == null)
                                                                                  {
                                                                                      //This may be a configuration that does not requires a name.
                                                                                      nameAttribute = uniqueValue(resolvedComponentModel.getBeanDefinition().getBeanClassName());
                                                                                  }
                                                                                  registry.registerBeanDefinition(nameAttribute, resolvedComponentModel.getBeanDefinition());
                                                                              }
                                                                          }, null);
                    }
                });
            }
            else
            {
                beanDefinitionReader.loadBeanDefinitions(getConfigResources());
            }
        }
        finally
        {
            currentMuleContext.remove();
        }
    }

    @Override
    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory)
    {
        super.customizeBeanFactory(beanFactory);
        new SpringMuleContextServiceConfigurator(muleContext, artifactType, optionalObjectsController, beanFactory).createArtifactServices();
    }

    @Override
    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
    {
        Optional<ComponentModel> configurationOptional = applicationModel.findComponentDefinitionModel(ApplicationModel.CONFIGURATION_IDENTIFIER);
        if (configurationOptional.isPresent())
        {
            return;
        }
        BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
        beanDefinitionRegistry.registerBeanDefinition(OBJECT_MULE_CONFIGURATION, genericBeanDefinition(MuleConfigurationConfigurator.class).getBeanDefinition());
    }

    protected BeanDefinitionReader createBeanDefinitionReader(DefaultListableBeanFactory beanFactory)
    {
        beanDefinitionReader = new MuleXmlBeanDefinitionReader(beanFactory, createBeanDefinitionDocumentReader(beanDefinitionFactory));
        // annotate parsed elements with metadata
        beanDefinitionReader.setDocumentLoader(createLoader());
        // hook in our custom hierarchical reader
        beanDefinitionReader.setDocumentReaderClass(getBeanDefinitionDocumentReaderClass());
        // add error reporting
        beanDefinitionReader.setProblemReporter(new MissingParserProblemReporter());
        registerAnnotationConfigProcessors(beanDefinitionReader.getRegistry(), null);

        return beanDefinitionReader;
    }

    protected MuleBeanDefinitionDocumentReader createBeanDefinitionDocumentReader(BeanDefinitionFactory beanDefinitionFactory)
    {
        if (artifactType.equals(ArtifactType.DOMAIN))
        {
            return new MuleDomainBeanDefinitionDocumentReader(beanDefinitionFactory, xmlApplicationParser);
        }
        return new MuleBeanDefinitionDocumentReader(beanDefinitionFactory, xmlApplicationParser);
    }

    protected MuleDocumentLoader createLoader()
    {
        return new MuleDocumentLoader();
    }

    private void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry, Object source)
    {
        registerAnnotationConfigProcessor(registry, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME, ConfigurationClassPostProcessor.class, source);
        registerAnnotationConfigProcessor(registry, REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME, RequiredAnnotationBeanPostProcessor.class, source);
        registerInjectorProcessor(registry);
    }

    protected void registerInjectorProcessor(BeanDefinitionRegistry registry)
    {
        if (artifactType.equals(ArtifactType.APP))
        {
            registerAnnotationConfigProcessor(registry, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME, MuleInjectorProcessor.class, null);
        }
        else if (artifactType.equals(ArtifactType.DOMAIN))
        {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ContextExclusiveInjectorProcessor.class);
            builder.addConstructorArgValue(this);
            registerPostProcessor(registry, (RootBeanDefinition) builder.getBeanDefinition(), AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME);
        }
    }

    private void registerAnnotationConfigProcessor(BeanDefinitionRegistry registry, String key, Class<?> type, Object source)
    {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(type);
        beanDefinition.setSource(source);
        registerPostProcessor(registry, beanDefinition, key);
    }

    protected void registerPostProcessor(BeanDefinitionRegistry registry, RootBeanDefinition definition, String beanName)
    {
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(beanName, definition);
    }

    protected Class<? extends MuleBeanDefinitionDocumentReader> getBeanDefinitionDocumentReaderClass()
    {
        if (artifactType.equals(ArtifactType.DOMAIN))
        {
            return MuleDomainBeanDefinitionDocumentReader.class;
        }
        return MuleBeanDefinitionDocumentReader.class;
    }

    @Override
    protected DefaultListableBeanFactory createBeanFactory()
    {
        //Copy all postProcessors defined in the defaultMuleConfig so that they get applied to the child container
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(getInternalParentBeanFactory());
        beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
        beanFactory.setInstantiationStrategy(new LaxInstantiationStrategyWrapper(new CglibSubclassingInstantiationStrategy(), optionalObjectsController));

        return beanFactory;
    }

    /**
     * {@inheritDoc}
     * This implementation returns {@code false} if the
     * context hasn't been initialised yet, in opposition
     * to the default implementation which throws
     * an exception
     */
    @Override
    public boolean isRunning()
    {
        try
        {
            return super.isRunning();
        }
        catch (IllegalStateException e)
        {
            return false;
        }
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    protected OptionalObjectsController getOptionalObjectsController()
    {
        return optionalObjectsController;
    }

    public static ThreadLocal<MuleContext> getCurrentMuleContext()
    {
        return currentMuleContext;
    }

    private class XmlServiceRegistry implements ServiceRegistry
    {

        private final ServiceRegistry delegate;
        private final XmlNamespaceInfoProvider extensionsXmlInfoProvider;

        public XmlServiceRegistry(ServiceRegistry delegate, MuleContext muleContext)
        {
            this.delegate = delegate;
            final ExtensionManager extensionManager = muleContext.getExtensionManager();
            List<XmlNamespaceInfo> extensionNamespaces;
            if (extensionManager != null)
            {
                extensionNamespaces = extensionManager.getExtensions().stream()
                        .map(ext -> {
                            XmlModelProperty xmlModelProperty = ext.getModelProperty(XmlModelProperty.class).orElse(null);
                            return xmlModelProperty != null ? new StaticXmlNamespaceInfo(xmlModelProperty.getNamespaceUri(), xmlModelProperty.getNamespace()) : null;
                        })
                        .filter(info -> info != null)
                        .collect(new ImmutableListCollector<>());
            }
            else
            {
                extensionNamespaces = ImmutableList.of();
            }

            extensionsXmlInfoProvider = new StaticXmlNamespaceInfoProvider(extensionNamespaces);
        }

        @Override
        public <T> Collection<T> lookupProviders(Class<T> providerClass, ClassLoader loader)
        {
            Collection<T> providers = delegate.lookupProviders(providerClass, loader);
            if (XmlNamespaceInfoProvider.class.equals(providerClass))
            {
                providers = ImmutableList.<T>builder().addAll(providers).add((T) extensionsXmlInfoProvider).build();
            }

            return providers;
        }

        @Override
        public <T> Collection<T> lookupProviders(Class<T> providerClass)
        {
            return lookupProviders(providerClass, Thread.currentThread().getContextClassLoader());
        }
    }
}
