/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_PROVIDER;
import static org.mule.runtime.core.internal.config.DefaultCustomizationService.HA_MIGRATION_ENABLED_PROPERTY;
import static org.mule.test.allure.AllureConstants.CustomizationServiceFeature.CUSTOMIZATION_SERVICE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static java.lang.System.setProperty;
import static org.mule.test.allure.AllureConstants.HaMigrationFeature.HA_MIGRATION_FEATURE;
import static org.mule.test.allure.AllureConstants.HaMigrationFeature.LocksStory.LOCKS_MIGRATION;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
import org.mule.runtime.api.lock.LockProvider;
import org.mule.runtime.core.internal.config.DefaultCustomizationService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.junit.jupiter.api.Test;
import io.qameta.allure.Feature;

import java.util.concurrent.locks.Lock;

@Features({@Feature(CUSTOMIZATION_SERVICE), @Feature(HA_MIGRATION_FEATURE)})
@Story(LOCKS_MIGRATION)
public class HaMigrationServicesOverrideTestCase extends AbstractMuleTestCase {

  private DefaultCustomizationService customizationService = new DefaultCustomizationService();

  @Test
  void doNotCombineServicesIfNotInMigration() {
    setProperty(HA_MIGRATION_ENABLED_PROPERTY, "false");
    TestLockProvider lock1 = new TestLockProvider();
    TestLockProvider lock2 = new TestLockProvider();
    customizationService.overrideDefaultServiceImpl(OBJECT_LOCK_PROVIDER, lock1);
    customizationService.overrideDefaultServiceImpl(OBJECT_LOCK_PROVIDER, lock2);
    LockProvider finalProvider =
        (LockProvider) customizationService.getOverriddenService(OBJECT_LOCK_PROVIDER).get().getServiceImpl().get();
    finalProvider.createLock("cow");
    assertThat(lock1.invoked, is(false));
    assertThat(lock2.invoked, is(true));
  }

  @Test
  void combineServicesIfInMigration() {
    setProperty(HA_MIGRATION_ENABLED_PROPERTY, "true");
    TestLockProvider lock1 = new TestLockProvider();
    TestLockProvider lock2 = new TestLockProvider();
    customizationService.interceptDefaultServiceImpl(OBJECT_LOCK_PROVIDER, interceptor -> interceptor.overrideServiceImpl(lock1));
    customizationService.interceptDefaultServiceImpl(OBJECT_LOCK_PROVIDER, interceptor -> interceptor.overrideServiceImpl(lock2));
    LockProvider finalProvider =
        (LockProvider) customizationService.getOverriddenService(OBJECT_LOCK_PROVIDER).get().getServiceImpl().get();
    finalProvider.createLock("cow");
    try {
      assertThat(lock1.invoked, is(true));
      assertThat(lock2.invoked, is(true));
    } finally {
      setProperty(HA_MIGRATION_ENABLED_PROPERTY, "false");
    }
  }

  private static final class TestLockProvider implements LockProvider {

    public boolean invoked = false;

    @Override
    public Lock createLock(String lockId) {
      invoked = true;
      return null;
    }
  }

}
