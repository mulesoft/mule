/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import static org.mule.test.allure.AllureConstants.LeakPrevention.LEAK_PREVENTION;
import static org.mule.test.allure.AllureConstants.LeakPrevention.LeakPreventionMetaspace.METASPACE_LEAK_PREVENTION_ON_REDEPLOY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.module.artifact.api.classloader.ResourceReleaser;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(LEAK_PREVENTION)
@Story(METASPACE_LEAK_PREVENTION_ON_REDEPLOY)
public class ResourceReleaserExecutorTestCase extends AbstractMuleTestCase {

  private static final RuntimeException BROKEN_SUPPLIER_EXCEPTION = new RuntimeException("Releaser supplier error");

  private final ErrorRecorder errorRecorder = new ErrorRecorder();
  private final ResourceReleaserExecutor resourceReleaserExecutor = new ResourceReleaserExecutor(errorRecorder);

  @Test
  public void executeOneReleaser() {
    TestResourceReleaser resourceReleaser = new TestResourceReleaser();
    resourceReleaserExecutor.addResourceReleaser(() -> resourceReleaser);
    resourceReleaserExecutor.executeResourceReleasers();

    assertThat(resourceReleaser.getExecCount(), is(1));
    assertThat(errorRecorder.getErrors(), is(empty()));
  }

  @Test
  public void executeManyReleasers() {
    TestResourceReleaser resourceReleaser1 = new TestResourceReleaser();
    TestResourceReleaser resourceReleaser2 = new TestResourceReleaser();
    resourceReleaserExecutor.addResourceReleaser(() -> resourceReleaser1);
    resourceReleaserExecutor.addResourceReleaser(() -> resourceReleaser2);
    resourceReleaserExecutor.executeResourceReleasers();

    assertThat(resourceReleaser1.getExecCount(), is(1));
    assertThat(resourceReleaser2.getExecCount(), is(1));
    assertThat(errorRecorder.getErrors(), is(empty()));
  }

  @Test
  public void whenOneReleaserFailsThenOtherReleasersAreStillExecuted() {
    TestResourceReleaser resourceReleaser = new TestResourceReleaser();
    resourceReleaserExecutor.addResourceReleaser(BrokenResourceReleaser::new);
    resourceReleaserExecutor.addResourceReleaser(() -> resourceReleaser);
    resourceReleaserExecutor.addResourceReleaser(BrokenResourceReleaser::new);
    resourceReleaserExecutor.executeResourceReleasers();

    assertThat(resourceReleaser.getExecCount(), is(1));
    assertThat(errorRecorder.getErrors(), contains(is(BrokenResourceReleaser.EXCEPTION),
                                                   is(BrokenResourceReleaser.EXCEPTION)));
  }

  @Test
  public void whenOneReleaserSupplierFailsThenOtherReleasersAreStillExecuted() {
    TestResourceReleaser resourceReleaser = new TestResourceReleaser();
    resourceReleaserExecutor.addResourceReleaser(this::throwSupplierError);
    resourceReleaserExecutor.addResourceReleaser(() -> resourceReleaser);
    resourceReleaserExecutor.addResourceReleaser(this::throwSupplierError);
    resourceReleaserExecutor.executeResourceReleasers();

    assertThat(resourceReleaser.getExecCount(), is(1));
    assertThat(errorRecorder.getErrors(), contains(is(BROKEN_SUPPLIER_EXCEPTION),
                                                   is(BROKEN_SUPPLIER_EXCEPTION)));
  }

  private ResourceReleaser throwSupplierError() {
    throw BROKEN_SUPPLIER_EXCEPTION;
  }

  private static class TestResourceReleaser implements ResourceReleaser {

    private int execCount = 0;

    @Override
    public void release() {
      execCount++;
    }

    public int getExecCount() {
      return execCount;
    }
  }

  private static class BrokenResourceReleaser implements ResourceReleaser {

    public static final RuntimeException EXCEPTION = new RuntimeException("I'm broken");

    @Override
    public void release() {
      throw EXCEPTION;
    }
  }

  private static class ErrorRecorder implements Consumer<Throwable> {

    private final List<Throwable> errors = new ArrayList<>();

    @Override
    public void accept(Throwable throwable) {
      errors.add(throwable);
    }

    public List<Throwable> getErrors() {
      return errors;
    }
  }
}

