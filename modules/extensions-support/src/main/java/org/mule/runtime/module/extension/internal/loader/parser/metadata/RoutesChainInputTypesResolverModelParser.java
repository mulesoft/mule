package org.mule.runtime.module.extension.internal.loader.parser.metadata;

import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

import java.util.Map;

public interface RoutesChainInputTypesResolverModelParser {

  Map<String, ChainInputTypeResolver> getRoutesChainInputResolvers();
}
