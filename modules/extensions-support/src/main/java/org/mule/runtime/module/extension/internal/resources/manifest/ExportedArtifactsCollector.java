/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.metadata.utils.MetadataTypeUtils.getSingleAnnotation;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MANIFEST_FILE_NAME;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.extension.api.ExtensionWalker;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.operation.HasOperationModels;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.property.ExportModelProperty;
import org.mule.runtime.extension.api.introspection.source.HasSourceModels;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.extension.xml.dsl.api.property.XmlModelProperty;

import com.google.common.collect.ImmutableSet;

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
        getExportModelProperty().map(ExportModelProperty::getExportedTypes)
                .ifPresent(types -> types.forEach( c -> exportedClasses.add(getType(c))));
    }

    private Optional<ExportModelProperty> getExportModelProperty()
    {
        return extensionModel.getModelProperty(ExportModelProperty.class);
    }

    private void collectDefault()
    {
        new ExtensionWalker()
        {
            @Override
            public void onParameter(ParameterizedModel owner, ParameterModel model)
            {
                getParameterClass(model).ifPresent(exportedClasses::add);
            }

            @Override
            public void onOperation(HasOperationModels owner, OperationModel model)
            {
                collectReturnTypes(model);
            }

            @Override
            public void onSource(HasSourceModels owner, SourceModel model)
            {
                collectReturnTypes(model);
            }

        }.walk(extensionModel);
    }

    private void collectReturnTypes(ComponentModel model)
    {
        exportedClasses.add(getType(model.getOutput().getType()));
        exportedClasses.add(getType(model.getOutputAttributes().getType()));
    }

    private Optional<Class<?>> getParameterClass(ParameterModel parameter)
    {
        ValueHolder<Class<?>> clazz = new ValueHolder<>(null);

        parameter.getType().accept(new MetadataTypeVisitor()
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
                clazz.set(getType(objectType));
            }

            @Override
            public void visitString(StringType stringType)
            {
                Optional<EnumAnnotation> enumAnnotation = getSingleAnnotation(stringType, EnumAnnotation.class);
                if (enumAnnotation.isPresent())
                {
                    clazz.set(getType(stringType));
                }
            }
        });

        return Optional.ofNullable(clazz.get());
    }
}
