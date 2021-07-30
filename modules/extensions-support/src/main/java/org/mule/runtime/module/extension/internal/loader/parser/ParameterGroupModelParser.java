package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ParameterGroupModelParser {

  String getName();

  String getDescription();

  List<ParameterModelParser> getParameterParsers();

  Optional<DisplayModel> getDisplayModel();

  Optional<LayoutModel> getLayoutModel();

  Optional<ExclusiveOptionalDescriptor> getExclusiveOptionals();

  boolean showsInDsl();

  List<ModelProperty> getAdditionalModelProperties();

  class ExclusiveOptionalDescriptor {

    private final Set<String> exclusiveOptionals;
    private final boolean oneRequired;

    public ExclusiveOptionalDescriptor(Set<String> exclusiveOptionals, boolean oneRequired) {
      this.exclusiveOptionals = exclusiveOptionals;
      this.oneRequired = oneRequired;
    }

    public Set<String> getExclusiveOptionals() {
      return exclusiveOptionals;
    }

    public boolean isOneRequired() {
      return oneRequired;
    }
  }
}
