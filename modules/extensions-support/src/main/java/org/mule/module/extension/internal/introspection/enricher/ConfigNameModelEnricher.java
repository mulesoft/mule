/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.enricher;

import static java.util.stream.Collectors.toList;
import static org.reflections.ReflectionUtils.getAllFields;
import static org.reflections.ReflectionUtils.withAnnotation;
import org.mule.extension.api.annotation.param.ConfigName;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.module.extension.internal.model.property.RequireNameField;
import org.mule.util.CollectionUtils;

import com.google.common.base.Joiner;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * A {@link ModelEnricher} which looks for configurations with fields
 * annotated with {@link ConfigName}.
 * <p>
 * It validates that the annotations is used properly and if so it adds
 * a {@link RequireNameField} on the {@link ConfigurationDeclaration}.
 * <p>
 * If the {@link ConfigName} annotation is used in a way which breaks
 * the rules set on its javadoc, an {@link IllegalConfigurationModelDefinitionException}
 * is thrown
 *
 * @since 4.0
 */
public final class ConfigNameModelEnricher implements ModelEnricher
{

    @Override
    public void enrich(DescribingContext describingContext)
    {
        for (ConfigurationDeclaration config : describingContext.getExtensionDeclarer().getExtensionDeclaration().getConfigurations())
        {
            ImplementingTypeModelProperty typeProperty = config.getModelProperty(ImplementingTypeModelProperty.class).orElse(null);
            if (typeProperty == null)
            {
                continue;
            }

            Collection<Field> fields = getAllFields(typeProperty.getType(), withAnnotation(ConfigName.class));
            if (CollectionUtils.isEmpty(fields))
            {
                continue;
            }

            if (fields.size() > 1)
            {
                throw new IllegalConfigurationModelDefinitionException(String.format(
                        "Only one configuration field is allowed to be annotated with @%s, but class '%s' has %d fields " +
                        "with such annotation. Offending fields are: [%s]",
                        ConfigName.class.getSimpleName(),
                        typeProperty.getType().getName(),
                        fields.size(),
                        Joiner.on(", ").join(fields.stream().map(Field::getName).collect(toList()))));
            }

            final Field configNameField = fields.iterator().next();
            if (!String.class.equals(configNameField.getType()))
            {
                throw new IllegalConfigurationModelDefinitionException(String.format(
                        "Config class '%s' declares the field '%s' which is annotated with @%s and is of type '%s'. Only " +
                        "fields of type String are allowed to carry such annotation",
                        typeProperty.getType().getName(),
                        configNameField.getName(),
                        ConfigName.class.getSimpleName(),
                        configNameField.getType().getName()));
            }

            config.addModelProperty(new RequireNameField(configNameField));
        }
    }
}
