/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.java.utils.JavaTypeUtils.getType;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.connection.HasConnectionProviderModels;
import org.mule.runtime.extension.api.introspection.connection.RuntimeConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.parameter.ParametrizedModel;
import org.mule.runtime.extension.api.introspection.property.ExportModelProperty;
import org.mule.runtime.extension.api.introspection.property.XmlModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class which calculates the default set of java package
 * names and resources that a given extension should export in order
 * to properly function
 *
 * @since 4.0
 */
final class ExportedArtifactsCollector
{

    private static final String META_INF_PREFIX = "/META-INF";
    private final Set<String> filteredPackages = ImmutableSet.<String>builder()
            .add("java.", "javax.", "org.mule.runtime.").build();

    private final ExtensionModel extensionModel;
    private final ImmutableSet.Builder<Class> exportedClasses = ImmutableSet.builder();
    private final ImmutableSet.Builder<String> exportedResources = ImmutableSet.builder();

    /**
     * Creates a new instance
     *
     * @param extensionModel the {@link ExtensionModel model} for the analyzed extension
     */
    ExportedArtifactsCollector(ExtensionModel extensionModel)
    {
        this.extensionModel = extensionModel;
    }

    /**
     * @return The {@link Set} of default resource paths that the extension should export
     */
    Set<String> getExportedResources()
    {
        // TODO: remove at Kraan's notice
        addMetaInfResource("");

        addMetaInfResource(EXTENSION_MANIFEST_FILE_NAME);
        collectXmlSupportResources();

        return exportedResources.build();
    }

    /**
     * @return The {@link Set} of default java package names that the extension should export
     */
    Set<String> getExportedPackages()
    {
        collectDefault();
        collectManuallyExportedPackages();

        Set<String> exportedPackages = exportedClasses.build().stream()
                .filter(type -> type.getPackage() != null)
                .map(type -> type.getPackage().getName())
                .collect(toSet());

        return filterExportedPackages(exportedPackages);
    }

    private void collectXmlSupportResources()
    {
        XmlModelProperty xml = extensionModel.getModelProperty(XmlModelProperty.class).orElse(null);
        if (xml == null)
        {
            return;
        }

        addMetaInfResource(xml.getXsdFileName());
        addMetaInfResource("spring.handlers");
        addMetaInfResource("spring.schemas");

        Optional<ExportModelProperty> exportProperty = getExportModelProperty();
        if (exportProperty.isPresent())
        {
            exportedResources.addAll(exportProperty.get().getExportedResources());
        }
    }

    private void addMetaInfResource(String resource)
    {
        exportedResources.add(META_INF_PREFIX + "/" + resource);
    }

    private Set<String> filterExportedPackages(Set<String> exportedPackages)
    {
        return exportedPackages.stream()
                .filter(packageName -> filteredPackages.stream().noneMatch(filtered -> packageName.startsWith(filtered)))
                .collect(toSet());
    }

    private void collectManuallyExportedPackages()
    {
        Optional<ExportModelProperty> exportProperty = getExportModelProperty();
        if (exportProperty.isPresent())
        {
            exportedClasses.addAll(exportProperty.get().getExportedClasses());
        }
    }

    private Optional<ExportModelProperty> getExportModelProperty()
    {
        return extensionModel.getModelProperty(ExportModelProperty.class);
    }

    private void collectDefault()
    {
        collectImplementingClasses();
        collectParameterClasses();
        collectReturnTypes();
        collectConnectionTypes();
    }

    private void collectReturnTypes()
    {
        collectReturnTypes(extensionModel.getOperationModels(), extensionModel.getSourceModels());
        extensionModel.getConfigurationModels().forEach(configuration ->
                                                                collectReturnTypes(configuration.getOperationModels(),
                                                                                   configuration.getSourceModels())
        );
    }

    private void collectConnectionTypes()
    {
        collectConnectionTypes(extensionModel);
        extensionModel.getConfigurationModels().forEach(this::collectConnectionTypes);
    }

    private void collectConnectionTypes(HasConnectionProviderModels model)
    {
        model.getConnectionProviders().stream()
                .map(provider -> ((RuntimeConnectionProviderModel) provider).getConnectionType())
                .forEach(exportedClasses::add);
    }

    private void collectReturnTypes(Collection<? extends ComponentModel>... componentModelsArray)
    {
        stream(componentModelsArray).forEach(componentList -> componentList.forEach(component -> {
            exportedClasses.add(getType(component.getReturnType()));
            exportedClasses.add(getType(component.getAttributesType()));
        }));
    }

    private void collectParameterClasses()
    {
        collectParameterClasses(
                extensionModel.getConnectionProviders(),
                extensionModel.getConfigurationModels(),
                extensionModel.getOperationModels(),
                extensionModel.getSourceModels());

        extensionModel.getConfigurationModels().forEach(configuration ->
                                                                collectParameterClasses(
                                                                        configuration.getConnectionProviders(),
                                                                        configuration.getOperationModels(),
                                                                        configuration.getSourceModels())
        );
    }

    private void collectParameterClasses(Collection<? extends ParametrizedModel>... parametrizedModelsArray)
    {
        MetadataTypeVisitor visitor = new MetadataTypeVisitor()
        {
            @Override
            public void visitDictionary(DictionaryType dictionaryType)
            {
                dictionaryType.getKeyType().accept(this);
                dictionaryType.getValueType().accept(this);
            }

            @Override
            public void visitArrayType(ArrayType arrayType)
            {
                arrayType.getType().accept(this);
            }

            @Override
            public void visitObject(ObjectType objectType)
            {
                exportedClasses.add(getType(objectType));
            }
        };

        stream(parametrizedModelsArray).forEach(modelList -> modelList.forEach(
                model -> model.getParameterModels().forEach(p -> p.getType().accept(visitor))));
    }

    private void collectImplementingClasses()
    {
        collectImplementingClasses(collectEnrichableModels());
    }


    private void collectImplementingClasses(Collection<EnrichableModel> models)
    {
        models.stream()
                .map(model -> model.getModelProperty(ImplementingTypeModelProperty.class))
                .map(property -> property.isPresent() ? property.get().getType() : null)
                .filter(property -> property != null)
                .forEach(exportedClasses::add);
    }

    private Collection<EnrichableModel> collectEnrichableModels()
    {
        Set<EnrichableModel> enrichableModels = new HashSet<>();

        enrichableModels.add(extensionModel);
        enrichableModels.addAll(extensionModel.getConfigurationModels());
        enrichableModels.addAll(extensionModel.getOperationModels());
        enrichableModels.addAll(extensionModel.getSourceModels());
        enrichableModels.addAll(extensionModel.getConnectionProviders());
        extensionModel.getConfigurationModels().forEach(configuration -> {
            enrichableModels.addAll(configuration.getOperationModels());
            enrichableModels.addAll(configuration.getOperationModels());
            enrichableModels.addAll(configuration.getSourceModels());
            enrichableModels.addAll(configuration.getConnectionProviders());
        });

        return enrichableModels;
    }
}
