package org.mule.runtime.core.api.type.catalog;

import static org.mule.metadata.message.api.el.ModuleDefinition.builder;

import org.mule.metadata.message.api.el.ModuleDefinition;
import org.mule.runtime.api.meta.model.ExtensionModel;

import java.util.function.Function;

public class ExtensionModelToModuleDefinitionTransformer implements Function<ExtensionModel, ModuleDefinition> {

    @Override
    public ModuleDefinition apply(ExtensionModel extensionModel) {
        return builder(extensionModel.getName()).build();
    }
}
