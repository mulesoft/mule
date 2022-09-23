/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.type.catalog;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.util.Collections.unmodifiableMap;

import org.mule.metadata.api.ListableTypeLoader;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ListableTypeLoadersRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link ListableTypeLoader}. If you use the {@link #getTypeLoaderByPrefix(String)} method only, this
 * implementation will load each {@link ListableTypeLoader} lazily, only when it's requested. If you use
 * {@link #getAllTypeLoaders()} instead, all the {@link ListableTypeLoader} instances will be loaded in that moment.
 *
 * @since 4.5.0
 */
public final class DefaultListableTypeLoadersRepository implements ListableTypeLoadersRepository {

  private final Map<String, ListableTypeLoader> allTypeLoaders;
  private final Map<String, ExtensionModel> extensionModelsByPrefix;

  public static DefaultListableTypeLoadersRepository from(Collection<ExtensionModel> extensionModels) {
    return new DefaultListableTypeLoadersRepository(extensionModels);
  }

  private DefaultListableTypeLoadersRepository(Collection<ExtensionModel> extensionModels) {
    allTypeLoaders = new HashMap<>(extensionModels.size());
    extensionModelsByPrefix = new HashMap<>(extensionModels.size());
    for (ExtensionModel em : extensionModels) {
      extensionModelsByPrefix.put(getPrefix(em), em);
    }
  }

  private static String getPrefix(ExtensionModel extensionModel) {
    return extensionModel.getXmlDslModel().getPrefix();
  }

  @Override
  public Map<String, ListableTypeLoader> getAllTypeLoaders() {
    computeAbsentTypeLoaders();
    return unmodifiableMap(allTypeLoaders);
  }

  private void computeAbsentTypeLoaders() {
    for (String prefix : extensionModelsByPrefix.keySet()) {
      computeLoaderForPrefixIfAbsent(prefix);
    }
  }

  private ListableTypeLoader computeLoaderForPrefixIfAbsent(String prefix) {
    return allTypeLoaders.computeIfAbsent(prefix, this::loadFromExtension);
  }

  @Override
  public ListableTypeLoader getTypeLoaderByPrefix(String prefix) throws TypeLoaderNotFoundException {
    ListableTypeLoader loader = computeLoaderForPrefixIfAbsent(prefix);
    if (loader == null) {
      throw new TypeLoaderNotFoundException(prefix, extensionModelsByPrefix.keySet());
    }
    return loader;
  }

  private ListableTypeLoader loadFromExtension(String extensionPrefix) {
    ExtensionModel extensionModel = extensionModelsByPrefix.get(extensionPrefix);
    if (extensionModel == null) {
      return null;
    }

    return new ListableTypeLoaderByIdOrAlias(extensionModel.getTypes());
  }

  private static final class TypeLoaderNotFoundException extends MuleException {

    private static final long serialVersionUID = 6036607661578937292L;

    private TypeLoaderNotFoundException(String notFoundPrefix, Set<String> availablePrefixes) {
      super(createStaticMessage("Type loader not found for prefix '%s'. Available prefixes are: %s", notFoundPrefix,
                                availablePrefixes));
    }
  }

}
