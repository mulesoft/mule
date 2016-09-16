/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.predicate;

import org.mule.extension.email.api.attributes.POP3EmailAttributes;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;

import java.util.function.Predicate;

/**
 * Builds a {@link Predicate} which verifies that a {@link POP3EmailAttributes} instance is compliant with a number of criteria.
 * <p>
 * The class main purpose is to give the &quot;pop3-matcher&quot; alias to make it unique and distinguishable from the other
 * matcher implementations besides that is more DSL/XML friendly.
 *
 * @since 4.0
 */
@XmlHints(allowTopLevelDefinition = true)
@Alias("pop3-matcher")
public class POP3EmailPredicateBuilder extends BaseEmailPredicateBuilder {

}
