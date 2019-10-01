/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.utils;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.config.preferred.Preferred;

import javax.inject.Inject;
import javax.inject.Named;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;

public class Utils {

  public static Object augmentedParam;

  public interface BaseService extends Service {

    void augmented();
  }

  public interface BaseOverloadedService extends BaseService {

    void augmented(int i);
  }

  public interface BaseOverloadedService2 extends BaseService {

    void augmented(MuleContext context);

    void augmented(MuleContext context, int i);
  }

  public static class BasicService implements BaseService {


    @Override
    public String getName() {
      return "BasicService";
    }

    @Override
    public void augmented() {
      augmentedParam = true;
    }

  }

  public static class AugmentedMethodService implements BaseService {

    @Override
    public String getName() {
      return "AugmentedMethodService";
    }

    @Override
    public void augmented() {}

    @Inject
    public void augmented(MuleContext context) {
      augmentedParam = context;
    }
  }

  public static class AugmentedSubclassMethodService extends AugmentedMethodService {

    @Override
    public String getName() {
      return "AugmentedSubclassMethodService";
    }

  }

  public static class AugmentedSubclassOverridesMethodService extends AugmentedMethodService {

    @Override
    public String getName() {
      return "AugmentedSubclassOverridesMethodService";
    }

    @Override
    @Inject
    public void augmented(MuleContext context) {
      augmentedParam = true;
    }
  }

  public static class AugmentedWithPreferredMethodService implements BaseService {

    @Override
    public String getName() {
      return "AugmentedWithPreferredMethodService";
    }

    @Override
    public void augmented() {}

    @Inject
    public void augmented(MyBean context) {
      augmentedParam = context;
    }
  }

  public static class MyBean {

  }

  @Preferred
  public static class MyPreferredBean extends MyBean {

  }

  public static class NamedAugmentedMethodService implements BaseService {

    @Override
    public String getName() {
      return "NamedAugmentedMethodService";
    }

    @Override
    public void augmented() {}

    @Inject
    public void augmented(@Named(OBJECT_MULE_CONTEXT) Object param) {
      augmentedParam = param;
    }
  }

  public static class InvalidNamedAugmentedMethodService implements BaseService {

    @Override
    public String getName() {
      return "InvalidNamedAugmentedMethodService";
    }

    @Override
    public void augmented() {}

    @Inject
    public void augmented(@Named("!@#$%&*_" + OBJECT_MULE_CONTEXT) Object param) {
      augmentedParam = param;
    }
  }

  public static class HiddenAugmentedMethodService implements BaseService {

    @Override
    public String getName() {
      return "HiddenAugmentedMethodService";
    }

    @Override
    public void augmented() {
      augmentedParam = true;
    }

    @Inject
    private void augmented(MuleContext context) {
      augmentedParam = context;
    }
  }

  public static class OverloadedAugmentedMethodService implements BaseOverloadedService {

    @Override
    public String getName() {
      return "OverloadedAugmentedMethodService";
    }

    @Override
    public void augmented() {}

    @Override
    public void augmented(int i) {}

    @Inject
    public void augmented(MuleContext context) {
      augmentedParam = true;
    }

    @Inject
    public void augmented(int i, MuleContext context) {
      augmentedParam = context;
    }
  }

  public static class OverloadedAugmentedMethodService2 implements BaseOverloadedService2 {

    @Override
    public String getName() {
      return "OverloadedAugmentedMethodService2";
    }

    @Override
    public void augmented() {}

    @Override
    @Inject
    public void augmented(MuleContext context) {}

    @Override
    @Inject
    public void augmented(MuleContext context, int i) {
      augmentedParam = context;
    }

  }

  public static class AmbiguousAugmentedMethodService implements BaseService {

    @Override
    public String getName() {
      return "AmbiguousAugmentedMethodService";
    }

    @Override
    public void augmented() {}

    @Inject
    public void augmented(MuleContext context) {
      augmentedParam = context;
    }

    @Inject
    public void augmented(MuleContext context, MuleContext contextB) {
      augmentedParam = context;
    }
  }

  public static class InvalidAugmentedMethodService implements BaseService {

    @Override
    public String getName() {
      return "InvalidAugmentedMethodService";
    }

    @Override
    public void augmented() {
      augmentedParam = true;
    }

    @Inject
    void augmented(int i) {}
  }


}
