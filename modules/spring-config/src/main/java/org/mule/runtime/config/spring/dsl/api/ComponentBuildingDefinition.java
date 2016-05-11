/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api;

import static org.mule.runtime.core.util.Preconditions.checkState;
import org.mule.runtime.config.spring.dsl.processor.TypeDefinition;
import org.mule.runtime.config.spring.dsl.model.ComponentIdentifier;
import org.mule.runtime.core.util.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines the mapping between a component configuration and how the object that represents
 * that model in runtime is created.
 *
 * @since 4.0
 */
public class ComponentBuildingDefinition
{

    private TypeDefinition typeDefinition;
    private boolean scope;
    private List<AttributeDefinition> constructorAttributeDefinition = new ArrayList<>();
    private Map<String, AttributeDefinition> setterParameterDefinitions = new HashMap<>();
    //TODO MULE-9638 Use generics. Generics cannot be used right now because this method colides with the ones defined in FactoryBeans.
    private Class<?> objectFactoryType;
    private boolean prototype;
    private ComponentIdentifier componentIdentifier;

    private ComponentBuildingDefinition()
    {
    }

    /**
     * @return a definition for the object type that must be created for this component
     */
    public TypeDefinition getTypeDefinition()
    {
        return typeDefinition;
    }

    /**
     * @return true if the building definition is an scope of message processors
     */
    public boolean isScope()
    {
        return scope;
    }

    /**
     * @return an ordered list of the constructor parameters that must be set to create the domain object
     */
    public List<AttributeDefinition> getConstructorAttributeDefinition()
    {
        return constructorAttributeDefinition;
    }

    /**
     * @return a map of the attributes that may contain configuration for the domain object to be created. The map key is the attribute name.
     */
    public Map<String, AttributeDefinition> getSetterParameterDefinitions()
    {
        return setterParameterDefinitions;
    }

    /**
     * @return the factory for the domain object. For complex object creations it's possible to define an object builder that will end up creating the domain object.
     */
    public Class<?> getObjectFactoryType()
    {
        return objectFactoryType;
    }

    /**
     * @return if the object is a prototype or a singleton
     */
    //TODO MULE-9681: remove for some other semantic. The API should not define something as "prototype" it should declare if it's a reusable component or an instance.
    //Ideally this can be inferred by the language itself. e.g.: Global message processors are always reusable components and do not define entities by them self.
    public boolean isPrototype()
    {
        return prototype;
    }

    /**
     * @return the unique identifier for this component
     */
    public ComponentIdentifier getComponentIdentifier()
    {
        return componentIdentifier;
    }

    /**
     * Builder for {@code ComponentBuildingDefinition}
     */
    public static class Builder
    {
        private String namespace;
        private String identifier;
        private ComponentBuildingDefinition definition = new ComponentBuildingDefinition();

        /**
         * Adds a new constructor parameter to be used during the object instantiation.
         *
         * @param attributeDefinition the constructor argument definition.
         * @return the builder
         */
        public Builder withConstructorParameterDefinition(AttributeDefinition attributeDefinition)
        {
            definition.constructorAttributeDefinition.add(attributeDefinition);
            return this;
        }

        /**
         * Adds a new parameter to be added to the object by using a setter method.
         *
         * @param fieldName the name of the field in which the value must be injected
         * @param attributeDefinition the setter parameter definition
         * @return the builder
         */
        public Builder withSetterParameterDefinition(String fieldName, AttributeDefinition attributeDefinition)
        {
            definition.setterParameterDefinitions.put(fieldName, attributeDefinition);
            return this;
        }

        /**
         * Sets the identifier of the configuration element that this building definition is for.
         * For instance, a config element <http:listener> has as identifier listener
         *
         * @param identifier configuration element identifier
         * @return the builder
         */
        public Builder withIdentifier(String identifier)
        {
            this.identifier = identifier;
            return this;
        }

        /**
         * Sets the namespace of the configuration element that this building definition is for.
         * For instance, a config element <http:listener> has as namespace http
         *
         * @param namespace configuration element namespace
         * @return the builder
         */
        public Builder withNamespace(String namespace)
        {
            this.namespace = namespace;
            return this;
        }

        /**
         * Sets the {@link org.mule.runtime.config.spring.dsl.processor.TypeDefinition} to discover the object type.
         * It may be created from {@link org.mule.runtime.config.spring.dsl.processor.TypeDefinition#fromType(Class)} which
         * means the type is predefined. Or it may be created from {@link org.mule.runtime.config.spring.dsl.processor.TypeDefinition#fromConfigurationAttribute(String)}
         * which means that the object type is declared within the configuration using a config attribute.
         *
         * @param typeDefinition the type definition to discover the objecvt type
         * @return the builder
         */
        public Builder withTypeDefinition(TypeDefinition typeDefinition)
        {
            definition.typeDefinition = typeDefinition;
            return this;
        }

        /**
         * Used to declare that object to be created is an scope.
         *
         * @return the builder
         */
        public Builder asScope()
        {
            definition.scope = true;
            return this;
        }

        /**
         * Defines a factory class to be used for creating the object. This method can be used
         * when the object to be build required complex logic.
         *
         * @param objectFactoryType {@code Class} for the factory to use to create the object
         * @return the builder
         */
        public Builder withObjectFactoryType(Class<?> objectFactoryType)
        {
            definition.objectFactoryType = objectFactoryType;
            return this;
        }

        /**
         * Makes a deep copy of the builder so it's current configuration can be reused.
         * @return a {@code Builder} copy.
         */
        public Builder copy()
        {
            Builder builder = new Builder();
            builder.definition.setterParameterDefinitions = new HashMap<>(this.definition.setterParameterDefinitions);
            builder.definition.constructorAttributeDefinition = new ArrayList<>(this.definition.constructorAttributeDefinition);
            builder.identifier = this.identifier;
            builder.namespace = this.namespace;
            builder.definition.scope = this.definition.scope;
            builder.definition.typeDefinition = this.definition.typeDefinition;
            return builder;
        }

        /**
         * Builds a {@link org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition} with the parameters set in the builder.
         *
         * At least the identifier, namespace and type definition must be configured or this method will fail.
         *
         * @return a fully configured {@link org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition}
         */
        public ComponentBuildingDefinition build()
        {
            checkState(definition.typeDefinition != null, "You must specify the type");
            checkState(identifier != null, "You must specify the identifier");
            checkState(namespace != null, "You must specify the namespace");
            definition.componentIdentifier = new ComponentIdentifier.Builder().withName(identifier).withNamespace(namespace).build();
            return definition;
        }

        //TODO MULE-9681: remove for some other semantic. The API should not define something as "prototype" it should declare if it's a reusable component or an instance.
        //Ideally this can be inferred by the language itself. e.g.: Global message processors are always reusable components and do not define entities by them self.
        public Builder asPrototype()
        {
            definition.prototype = true;
            return this;
        }
    }
}
