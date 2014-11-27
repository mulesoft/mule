package org.mule.config.spring;

import org.w3c.dom.Element;

/**
 * Allows validating an {@link Element} prior to parsing it.
 * Implementations are to be reusable and thread-safe
 *
 * @since 3.6.0
 */
public interface ElementValidator
{

    /**
     * Validates the element and throws exception if validation failed
     *
     * @param element the {@link Element} to be validated
     * @throws Exception if the element was not valid
     */
    void validate(Element element);

}
