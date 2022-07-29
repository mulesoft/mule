/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.tracing;

import static org.junit.Assert.fail;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A tester verifier for {@link CapturedExportedSpan}'s.
 *
 * It defines an expected hierarchy of exported spans and asserts it against a {@link ExportedSpanCapturer}
 *
 * @since 4.5.0
 */
public class ExportedSpansVerifier {

  private final String name;
  private final Map<String, String> attributesToVerifyInSpan = new HashMap<>();
  private final Map<String, String> attributesToVerifyInSpanAndChildren = new HashMap<>();
  private final Set<String> attributesThatMustExist = new HashSet<>();
  private final Set<String> attributesThatMustExistInSpanAndAllChildren = new HashSet<>();
  private final List<ExportedSpansVerifier> spanChildren = new ArrayList<>();

  private ExportedSpansVerifier(String name) {
    this.name = name;
  }

  /**
   * Gets a verifier for a {@link CapturedExportedSpan} where the root has the {@param rootsSpanName}.
   *
   * @param rootSpanName the name of the root of the hierarchy to verify
   * @return the {@link ExportedSpansVerifier}.
   */
  public static ExportedSpansVerifier getExportedSpansVerifier(String rootSpanName) {
    return new ExportedSpansVerifier(rootSpanName);
  }

  /**
   * Indicates to the {@link ExportedSpansVerifier} that the attribute should be present with the corresponding value.
   *
   * @param key   the attribute key.
   * @param value the attribute value.
   *
   * @return the resulting {@link ExportedSpansVerifier}.
   */
  public ExportedSpansVerifier withAttribute(String key, String value) {
    attributesToVerifyInSpan.put(key, value);
    return this;
  }


  /**
   * Indicates to the {@link ExportedSpansVerifier} that the attribute should be present with the corresponding value in all the
   * span children.
   *
   * @param key   the attribute key.
   * @param value the attribute value.
   *
   * @return the resulting {@link ExportedSpansVerifier}.
   */
  public ExportedSpansVerifier withAttributeInAllChildren(String key, String value) {
    attributesToVerifyInSpanAndChildren.put(key, value);
    return this;
  }

  /**
   * Indicates that the span has to have a child span that can be verified with the {@param childExportedSpanVerifier}
   *
   * @param childExportedSpanVerifier the {@link ExportedSpansVerifier}
   *
   * @return the resulting {@link ExportedSpansVerifier}.
   */
  public ExportedSpansVerifier withChildExportedSpan(ExportedSpansVerifier childExportedSpanVerifier) {
    spanChildren.add(childExportedSpanVerifier);
    return this;
  }

  /**
   * Indicates to the {@link ExportedSpansVerifier} that the attribute should be present in the captured span.
   *
   * @param attribute the attribute key.
   *
   * @return the resulting {@link ExportedSpansVerifier}
   */
  public ExportedSpansVerifier hasAttribute(String attribute) {
    attributesThatMustExist.add(attribute);
    return this;
  }

  /**
   * Indicates to the {@link ExportedSpansVerifier} that the attribute should be present in the captured span and all its
   * children.
   *
   * @param attribute the attribute key.
   *
   * @return the resulting {@link ExportedSpansVerifier}
   */
  public ExportedSpansVerifier hasAttributeInAllChildren(String attribute) {
    attributesThatMustExistInSpanAndAllChildren.add(attribute);
    return this;
  }


  /**
   * Verifies that the {@param exportedSpanCapturer} captured {@link CapturedExportedSpan} according to the hierarchy defined in
   * the verifier.
   *
   * @param exportedSpanCapturer the {@link ExportedSpanCapturer}
   */
  public void verify(ExportedSpanCapturer exportedSpanCapturer) {
    new ExportedSpanExpectedHierarchy(name, attributesToVerifyInSpan, attributesThatMustExist,
                                      spanChildren).verify(exportedSpanCapturer, null,
                                                           attributesToVerifyInSpanAndChildren,
                                                           attributesThatMustExistInSpanAndAllChildren);
  }

  private void verify(ExportedSpanCapturer spanCapturer, String parentId,
                      Map<String, String> attributesInAllChildren) {
    new ExportedSpanExpectedHierarchy(name, attributesToVerifyInSpan, attributesThatMustExist,
                                      spanChildren).verify(spanCapturer, parentId,
                                                           attributesInAllChildren,
                                                           attributesThatMustExistInSpanAndAllChildren);
  }

  private static final class ExportedSpanExpectedHierarchy {

    private final String name;
    private final Map<String, String> attributes;
    private final List<ExportedSpansVerifier> children;
    private final Set<String> existingAttributes;

    private ExportedSpanExpectedHierarchy(String name, Map<String, String> attributes, Set<String> existingAttributes,
                                          List<ExportedSpansVerifier> children) {
      this.name = name;
      this.attributes = attributes;
      this.children = children;
      this.existingAttributes = existingAttributes;
    }

    public void verify(ExportedSpanCapturer spanCapturer, String expectedParentId, Map<String, String> attributesInAllChildren,
                       Set<String> existingAttributesInAllChildren) {
      Optional<CapturedExportedSpan> optionalCapturedExportedSpan =
          spanCapturer.getExportedSpans().stream().filter(span -> span.getName().equals(name)).findFirst();

      if (!optionalCapturedExportedSpan.isPresent()) {
        fail("A Span with name " + name + " was not captured");
      }

      CapturedExportedSpan capturedExportedSpan = optionalCapturedExportedSpan.get();

      if (expectedParentId == null && capturedExportedSpan.getParent().isPresent()) {
        fail("The span " + name + " has parent");
      }

      if (expectedParentId != null) {
        Optional<CapturedExportedSpan> capturedParentSpanOptional = capturedExportedSpan.getParent();
        if (!capturedParentSpanOptional.isPresent()) {
          fail("The span " + name + " has no parent");
        } else {
          String parentSpanId = capturedParentSpanOptional.get().getSpanId();
          if (!parentSpanId.equals(expectedParentId)) {
            fail("The expected parent span is wrong for span " + name + ": expected: " + expectedParentId + ", actual: "
                + parentSpanId);
          }
        }
      }

      assertExistingAttributes(capturedExportedSpan,
                               resolveExistingAttributesToAssert(existingAttributes, existingAttributesInAllChildren));
      assertAttributes(capturedExportedSpan, resolveAttributesToAssert(attributes, attributesInAllChildren));

      children.forEach(hierarchy -> hierarchy.verify(spanCapturer, capturedExportedSpan.getSpanId(), attributesInAllChildren));
    }

    private Set<String> resolveExistingAttributesToAssert(Set<String> existingAttributes,
                                                          Set<String> existingAttributesInAllChildren) {
      Set<String> resolvedExistingAttributes = new HashSet<>(existingAttributes);
      resolvedExistingAttributes.addAll(existingAttributesInAllChildren);
      return resolvedExistingAttributes;
    }

    private void assertExistingAttributes(CapturedExportedSpan capturedExportedSpan,
                                          Set<String> existingAttributes) {
      existingAttributes.forEach(attr -> {
        if (!capturedExportedSpan.getAttributes().containsKey(attr)) {
          fail("The attribute " + attr + " is not present in span " + name);
        }
      });
    }

    private Map<String, String> resolveAttributesToAssert(Map<String, String> attributes,
                                                          Map<String, String> attributesInAllChildren) {
      Map<String, String> resolvedAttributes = new HashMap<>(attributes);
      attributesInAllChildren.forEach(resolvedAttributes::putIfAbsent);
      return resolvedAttributes;
    }

    private void assertAttributes(CapturedExportedSpan capturedSpan, Map<String, String> attrs) {
      attrs.forEach((key, value) -> {
        Map<String, String> attributesToAssert = capturedSpan.getAttributes();
        if (!attributesToAssert.containsKey(key) || !attributesToAssert.get(key).equals(value)) {
          fail("The attribute " + key + " with value " + value + " is not present in span " + name);
        }
      });
    }
  }
}

