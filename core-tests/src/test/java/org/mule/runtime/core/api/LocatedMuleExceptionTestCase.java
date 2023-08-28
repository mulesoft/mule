/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.exception.MuleException.INFO_LOCATION_KEY;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.exception.LocatedMuleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class LocatedMuleExceptionTestCase extends AbstractMuleTestCase {

  @Test
  public void component() {
    Component named = mock(Component.class);
    when(named.getRepresentation()).thenReturn("mockComponent");
    LocatedMuleException lme = new LocatedMuleException(named);
    assertThat(lme.getInfo().get(INFO_LOCATION_KEY).toString(), is("mockComponent"));
  }

}
