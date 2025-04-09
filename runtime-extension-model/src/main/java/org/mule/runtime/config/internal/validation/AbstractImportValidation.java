/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.UNKNOWN;
import static org.mule.runtime.ast.api.ImportedResource.COULD_NOT_RESOLVE_IMPORTED_RESOURCE;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_PREFIX;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentGenerationInformation;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.util.BaseComponentAst;
import org.mule.runtime.ast.api.validation.ArtifactValidation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Generic support for validation of `import` tags.
 * <p/>
 * Extending validations don't do the actual resource resolution, but instead generate the failed validations for any errors that
 * happened during the resolution of the imports during the artifact parsing.
 *
 * @since 4.8
 */
public abstract class AbstractImportValidation implements ArtifactValidation {

  @Override
  public List<ValidationResultItem> validateMany(ArtifactAst artifact) {
    return artifact.getImportedResources()
        .stream()
        .filter(imp -> imp.getResolutionFailure().isPresent())
        .filter(imp -> appliesValidationFor(imp.getResolutionFailure().get()))
        .map(imp -> {
          String message = imp.getResolutionFailure().get();
          return create(new ImportComponentAst(imp.getMetadata()),
                        this,
                        message,
                        message.startsWith(COULD_NOT_RESOLVE_IMPORTED_RESOURCE));
        })
        .collect(toList());
  }

  /**
   * Determines whether this resolution failure is to be handled by this validator or not.
   *
   * @param resolutionFailureMessage
   * @return whether this resolution failure is to be handled by this validator or not.
   */
  protected abstract boolean appliesValidationFor(String resolutionFailureMessage);

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return h -> true;
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    return this.validate(artifact);
  }

  private static final class ImportComponentAst extends BaseComponentAst {

    private final ComponentMetadataAst metadata;

    public ImportComponentAst(ComponentMetadataAst metadata) {
      this.metadata = metadata;
    }

    @Override
    public ComponentMetadataAst getMetadata() {
      return metadata;
    }

    @Override
    public MetadataType getType() {
      return null;
    }

    @Override
    public Collection<ComponentParameterAst> getParameters() {
      return emptyList();
    }

    @Override
    public <M> Optional<M> getModel(Class<M> modelClass) {
      return empty();
    }

    @Override
    public ComponentLocation getLocation() {
      return null;
    }

    @Override
    public ComponentIdentifier getIdentifier() {
      return builder().namespace(CORE_PREFIX).name("import").build();
    }

    @Override
    public ComponentGenerationInformation getGenerationInformation() {
      return null;
    }

    @Override
    public ExtensionModel getExtensionModel() {
      return null;
    }

    @Override
    public ComponentType getComponentType() {
      return UNKNOWN;
    }

    @Override
    public Optional<String> getComponentId() {
      return empty();
    }

    @Override
    public Map<String, Object> getAnnotations() {
      return emptyMap();
    }

    @Override
    public List<ComponentAst> directChildren() {
      return emptyList();
    }
  }

}
