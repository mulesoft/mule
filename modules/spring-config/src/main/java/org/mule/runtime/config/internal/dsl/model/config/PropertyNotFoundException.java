/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver.PLACEHOLDER_PREFIX;
import static org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver.PLACEHOLDER_SUFFIX;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.util.Pair;

import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when a key could not be resolved.
 * 
 * @since 4.0
 */
public class PropertyNotFoundException extends MuleRuntimeException {

  private List<Pair<String, String>> unresolvedKeys;

  /**
   * Creates a new instance. This constructor must be used when the resolver has no parent and was not able to resolve a key
   * 
   * @param resolverKeyPair the resolver descriptor and the key that was not able to resolve.
   */
  public PropertyNotFoundException(Pair<String, String> resolverKeyPair) {
    super(createFailureException(resolverKeyPair));
    unresolvedKeys = Collections.singletonList(resolverKeyPair);
  }

  private static I18nMessage createFailureException(Pair<String, String> resolverKeyPair) {
    return createStaticMessage(createMessageForLeakKey(resolverKeyPair));
  }

  private static String createMessageForLeakKey(Pair<String, String> resolverKeyPair) {
    return format("Couldn't find configuration property value for key %s from properties provider %s",
                  PLACEHOLDER_PREFIX + resolverKeyPair.getSecond() + PLACEHOLDER_SUFFIX,
                  resolverKeyPair.getFirst());
  }

  /**
   * Creates a new instance. This constructor must be used when the resolver invoke the parent and it failed because it wasn't
   * able to resolve a key
   *
   * @param propertyNotFoundException exception thrown by the parent resolver.
   * @param resolverKeyPair the resolver descriptor and the key that was not able to resolve.
   */
  public PropertyNotFoundException(PropertyNotFoundException propertyNotFoundException,
                                   Pair<String, String> resolverKeyPair) {
    super(createFailureException(propertyNotFoundException, resolverKeyPair));
    unresolvedKeys =
        com.google.common.collect.ImmutableList.<Pair<String, String>>builder()
            .addAll(propertyNotFoundException.getUnresolvedKeys())
            .add(resolverKeyPair).build();
  }

  private static I18nMessage createFailureException(PropertyNotFoundException propertyNotFoundException,
                                                    Pair<String, String> resolverKeyPair) {
    StringBuilder messageBuilder =
        new StringBuilder(createMessageForLeakKey(propertyNotFoundException.getUnresolvedKeys().get(0)));
    ImmutableList<Pair<String, String>> allPairs = ImmutableList.<Pair<String, String>>builder()
        .addAll(propertyNotFoundException.getUnresolvedKeys()).add(resolverKeyPair).build();
    allPairs.stream().skip(1).forEach(pair -> {
      messageBuilder.append(format(" - within resolver %s trying to process key %s", pair.getFirst(), pair.getSecond()));
    });
    return createStaticMessage(messageBuilder.toString());
  }

  /**
   * @return a list with the resolvers and the keys that were not resolved. The original key not found is the first element and
   *         the last will be the main resolver.
   */
  public List<Pair<String, String>> getUnresolvedKeys() {
    return unresolvedKeys;
  }


}
