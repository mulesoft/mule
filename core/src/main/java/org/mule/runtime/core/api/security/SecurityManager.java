/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;

import java.util.Collection;

/**
 * {@code SecurityManager} is responsible for managing one or more {@link SecurityProvider}s.
 *
 * @since 4.0
 */
public interface SecurityManager extends Initialisable {

  /**
   * Performs the authentication of a security request using one of the available {@link SecurityProvider}s
   *
   * @param authentication The {@link Authentication} request
   * @return The authenticated response
   * @throws SecurityException In case authentication fails
   * @see SecurityProvider#authenticate(Authentication)
   */
  Authentication authenticate(Authentication authentication) throws SecurityException, SecurityProviderNotFoundException;

  /**
   * Adds a {@link SecurityProvider} to the set of available providers for authentication.
   * 
   * @param provider the new {@link SecurityProvider}
   */
  void addProvider(SecurityProvider provider);

  /**
   * @return a {@link SecurityProvider} managed by this {@link SecurityManager} with the given {@code name} or {@code null} if
   *         none was found
   *
   * @see SecurityProvider#getName()
   */
  SecurityProvider getProvider(String name);

  /**
   * Removes the {@link SecurityProvider} with the given name from the pool of managed {@link SecurityProvider}s
   * 
   * @param name the {@link SecurityProvider#getName} to remove
   * @return the {@link SecurityProvider} with the given {@code name}, or {@code null} if none was found
   */
  SecurityProvider removeProvider(String name);

  /**
   * @return all the {@link SecurityProvider}s managed by {@code this} {@link SecurityManager}
   */
  Collection<SecurityProvider> getProviders();

  /**
   * Sets the available {@link SecurityProvider}s for authentication.
   *
   * @param providers the providers available for {@code this} {@link SecurityManager}
   */
  void setProviders(Collection<SecurityProvider> providers);

  /**
   * Uses one of the available {@link SecurityProvider}s that {@link SecurityProvider#supports} the given {@link Authentication}
   * to {@link SecurityProvider#createSecurityContext create a new security context}.
   *
   * @param authentication the {@link Authentication} used to create the new {@link SecurityContext}
   * @return a new {@link SecurityContext} created by the {@link SecurityProvider} with the given {@code authentication}
   * @throws UnknownAuthenticationTypeException if no {@link SecurityProvider} is found that {@link SecurityProvider#supports} the
   *         given {@code authentication}
   */
  SecurityContext createSecurityContext(Authentication authentication) throws UnknownAuthenticationTypeException;

  /**
   * @param name the {@link EncryptionStrategy#getName name} of the {@link EncryptionStrategy} that needs to be retrieved.
   * @return the {@link EncryptionStrategy} with the given {@code name}, from the ones available in {@code this}
   *         {@link SecurityManager}
   */
  EncryptionStrategy getEncryptionStrategy(String name);

  /**
   * Adds the {@link EncryptionStrategy} to the ones available from this {@link SecurityManager}
   * 
   * @param strategy the {@link EncryptionStrategy} to be added
   */
  void addEncryptionStrategy(EncryptionStrategy strategy);

  /**
   * Removes the {@link EncryptionStrategy} with the given {@code name} from the set of managed {@link EncryptionStrategy}s
   * 
   * @param name the {@link EncryptionStrategy#getName} to remove
   * @return the {@link EncryptionStrategy} with the given {@code name}, or {@code null} if none was found
   */
  EncryptionStrategy removeEncryptionStrategy(String name);

  /**
   * @return all the {@link EncryptionStrategy}s managed by {@code this} {@link SecurityManager}
   */
  Collection<EncryptionStrategy> getEncryptionStrategies();

  /**
   * Sets the available {@link EncryptionStrategy encryption strategies} for {@code this} {@link SecurityManager}.
   *
   * @param strategies the prstrategiesoviders available for {@code this} {@link EncryptionStrategy}
   */
  void setEncryptionStrategies(Collection<EncryptionStrategy> strategies);

}
