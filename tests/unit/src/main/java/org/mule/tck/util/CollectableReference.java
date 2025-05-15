/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import static java.lang.String.format;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Utility class created for leak testing. It extends a PhantomReference and implements a Hamcrest Matcher which checks that the
 * reference is enqueued, because it means that there isn't any strong reference pointing to the referent object.
 *
 * Usage example:
 *
 * <pre>
 *
 * {
 *   &#64;code
 *   CollectableReference<String> collectableReference = new CollectableReference<>(new String("Hello world"));
 *   System.out.println(collectableReference.get());
 *   assertThat(collectableReference, is(eventually(collectedByGc())));
 * }
 * </pre>
 *
 * @param <T> The referent type.
 */
public class CollectableReference<T> extends PhantomReference<T> {

  private T strongReference;
  private final String strongReferenceAsString;

  public CollectableReference(T referent) {
    super(referent, new ReferenceQueue<>());
    strongReference = referent;
    strongReferenceAsString = referent.toString();
  }

  /**
   * @return the referent.
   */
  @Override
  public T get() {
    return strongReference;
  }

  /**
   * Drops the strong reference.
   */
  private void dereference() {
    strongReference = null;
  }

  public static Matcher<CollectableReference> collectedByGc() {
    return new CollectedByGC();
  }

  @Override
  public String toString() {
    return strongReferenceAsString;
  }

  /**
   * This matcher checks that there aren't strong references to the object passed to the CollectableReference constructor. It
   * drops the internal CollectableReference strong reference and runs the garbage collector, so the user only needs to use it
   * within a Prober.
   */
  private static class CollectedByGC extends TypeSafeMatcher<CollectableReference> {

    private String referencedAsString;

    @Override
    protected boolean matchesSafely(CollectableReference reference) {
      if (reference.get() != null) {
        referencedAsString = reference.get().toString();
      }

      reference.dereference();
      System.gc();
      return reference.isEnqueued();
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(format("no strong reference to '%s' is being maintained", referencedAsString));
    }
  }
}
