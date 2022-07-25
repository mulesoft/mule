/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.profiling;

import org.mule.runtime.api.lifecycle.Disposable;

import java.util.Collection;

/**
 * A resource that allows to capture the exported spans. This is used only for testing purposes and is not exposed as a general
 * API.
 *
 * @since 4.5.0
 */
public interface ExportedSpanCapturer extends Disposable {

  Collection<CapturedExportedSpan> getExportedSpans();

}
