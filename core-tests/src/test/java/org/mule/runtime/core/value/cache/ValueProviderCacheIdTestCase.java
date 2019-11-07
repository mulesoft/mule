/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.value.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.core.internal.value.cache.ValueProviderCacheId.ValueProviderCacheIdBuilder.aValueProviderCacheId;
import static org.mule.runtime.core.internal.value.cache.ValueProviderCacheId.ValueProviderCacheIdBuilder.fromElementWithName;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ValueProviderCacheIdTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void noValueFails() {
    expectedException.expect(IllegalArgumentException.class);
    aValueProviderCacheId(fromElementWithName("name"));
  }

  @Test
  public void sameValue() {
    final String value = "value";
    ValueProviderCacheId id = aValueProviderCacheId(fromElementWithName("ElementName").withHashValueFrom(value));
    ValueProviderCacheId id2 = aValueProviderCacheId(fromElementWithName("ElementName").withHashValueFrom(value));
    assertThat(id, is(equalTo(id2)));
  }

  @Test
  public void sameValueDifferentElementName() {
    final String value = "value";
    ValueProviderCacheId id = aValueProviderCacheId(fromElementWithName("ElementNameOne").withHashValueFrom(value));
    ValueProviderCacheId id2 = aValueProviderCacheId(fromElementWithName("ElementNameTwo").withHashValueFrom(value));
    assertThat(id, is(equalTo(id2)));
  }

  @Test
  public void sameValueDifferentAttributes() {
    final String value = "value";
    ValueProviderCacheId id =
        aValueProviderCacheId(fromElementWithName("ElementName").withHashValueFrom(value).withAttribute("attr1", "1"));
    ValueProviderCacheId id2 =
        aValueProviderCacheId(fromElementWithName("ElementName").withHashValueFrom(value).withAttribute("attr2", "2"));
    assertThat(id, is(equalTo(id2)));
  }

  @Test
  public void sameValueDifferentParts() {
    final String value = "value";
    final ValueProviderCacheId part1 = aValueProviderCacheId(fromElementWithName("part1").withHashValueFrom("part1"));
    final ValueProviderCacheId part2 = aValueProviderCacheId(fromElementWithName("part2").withHashValueFrom("part2"));
    ValueProviderCacheId id =
        aValueProviderCacheId(fromElementWithName("ElementName").withHashValueFrom(value).containing(part1));
    ValueProviderCacheId id2 =
        aValueProviderCacheId(fromElementWithName("ElementName").withHashValueFrom(value).containing(part2));
    assertThat(id, is(not(equalTo(id2))));
  }

  @Test
  public void sameParts() {
    final String value = "value";
    final ValueProviderCacheId part = aValueProviderCacheId(fromElementWithName("part").withHashValueFrom("part"));
    ValueProviderCacheId id = aValueProviderCacheId(fromElementWithName("ElementName").withHashValueFrom(value).containing(part));
    ValueProviderCacheId id2 =
        aValueProviderCacheId(fromElementWithName("ElementName").withHashValueFrom(value).containing(part));
    assertThat(id, is(equalTo(id2)));
  }

  @Test
  public void samePartsDifferentValue() {
    final ValueProviderCacheId part = aValueProviderCacheId(fromElementWithName("part").withHashValueFrom("part"));
    ValueProviderCacheId id =
        aValueProviderCacheId(fromElementWithName("ElementName").withHashValueFrom("value1").containing(part));
    ValueProviderCacheId id2 =
        aValueProviderCacheId(fromElementWithName("ElementName").withHashValueFrom("value2").containing(part));
    assertThat(id, is(not(equalTo(id2))));
  }

}
