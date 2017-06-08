/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.AbstractAnnotatedObject.LOCATION_KEY;
import static org.mule.runtime.config.spring.MuleArtifactContext.INNER_BEAN_PREFIX;
import static org.mule.runtime.config.spring.MuleArtifactContext.postProcessBeanDefinition;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.FLOW_REF_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_DOMAIN_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_DOMAIN_ROOT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_EE_DOMAIN_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_ROOT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.POLICY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.POLICY_ROOT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROPERTIES_ELEMENT;
import static org.mule.runtime.config.spring.dsl.spring.CommonBeanDefinitionCreator.adaptFilterBeanDefinitions;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.config.spring.util.SpringXMLUtils.isLocalName;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.DOMAIN_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.DOMAIN_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EE_DOMAIN_NAMESPACE;
import static org.mule.runtime.internal.dsl.DslConstants.EE_DOMAIN_PREFIX;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.spring.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.spring.BeanDefinitionFactory;
import org.mule.runtime.config.spring.dsl.spring.ComponentModelHelper;
import org.mule.runtime.config.spring.parsers.generic.AutoIdUtils;
import org.mule.runtime.config.spring.util.SpringXMLUtils;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.internal.util.xmlsecurity.XMLSecureFactories;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This parser enables Mule to parse heirarchical bean structures using spring Namespace handling There are 4 base
 * DefinitionParsers supplied in Mule that most Parsers will extend from, these are
 * {@link org.mule.runtime.config.spring.parsers.AbstractChildDefinitionParser}
 * {@link org.mule.runtime.config.spring.parsers.AbstractMuleBeanDefinitionParser}
 * {@link org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser}
 * {@link org.mule.runtime.config.spring.parsers.generic.MuleOrphanDefinitionParser}
 */
public class MuleHierarchicalBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate {

  public static final String BEANS = "beans"; // cannot find this in Spring api!
  public static final String MULE_REPEAT_PARSE =
      "org.mule.runtime.config.spring.MuleHierarchicalBeanDefinitionParserDelegate.MULE_REPEAT_PARSE";
  public static final String MULE_NO_RECURSE =
      "org.mule.runtime.config.spring.MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE";
  public static final String MULE_FORCE_RECURSE =
      "org.mule.runtime.config.spring.MuleHierarchicalBeanDefinitionParserDelegate.MULE_FORCE_RECURSE";
  public static final String MULE_NO_REGISTRATION =
      "org.mule.runtime.config.spring.MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_REGISTRATION";
  public static final String MULE_POST_CHILDREN =
      "org.mule.runtime.config.spring.MuleHierarchicalBeanDefinitionParserDelegate.MULE_POST_CHILDREN";
  private final Supplier<ApplicationModel> applicationModelSupplier;
  private final List<ElementValidator> elementValidators;
  private final DefaultBeanDefinitionDocumentReader beanDefinitionDocumentReader;

  private BeanDefinitionFactory beanDefinitionFactory;

  protected static final Logger logger = LoggerFactory.getLogger(MuleHierarchicalBeanDefinitionParserDelegate.class);

  public MuleHierarchicalBeanDefinitionParserDelegate(XmlReaderContext readerContext,
                                                      DefaultBeanDefinitionDocumentReader beanDefinitionDocumentReader,
                                                      Supplier<ApplicationModel> applicationModelSupplier,
                                                      BeanDefinitionFactory beanDefinitionFactory,
                                                      ElementValidator... elementValidators) {
    super(readerContext);
    this.beanDefinitionDocumentReader = beanDefinitionDocumentReader;
    this.applicationModelSupplier = applicationModelSupplier;
    this.beanDefinitionFactory = beanDefinitionFactory;
    this.elementValidators =
        ArrayUtils.isEmpty(elementValidators) ? ImmutableList.<ElementValidator>of() : ImmutableList.copyOf(elementValidators);
  }

  private Either<BeanDefinition, BeanReference> parseDefinitionOrReference(Element element, BeanDefinition parent) {
    if (isLocalName(element, REF_ELEMENT)) {
      RuntimeBeanNameReference beanNameReference = (RuntimeBeanNameReference) parseIdRefElement(element);
      RuntimeBeanReference beanReference = new RuntimeBeanReference(beanNameReference.getBeanName());
      return right(beanReference);
    } else {
      return left(parseCustomElement(element, parent));
    }
  }

  @Override
  public BeanDefinition parseCustomElement(Element element, BeanDefinition parent) {
    if (logger.isDebugEnabled()) {
      logger.debug("parsing: " + SpringXMLUtils.elementToString(element));
    }

    validate(element);

    if (SpringXMLUtils.isBeansNamespace(element) && !springElementHasCustomParser(element)) {
      return handleSpringElements(element, parent);
    } else {
      String namespaceUri = element.getNamespaceURI();
      NamespaceHandler handler = getReaderContext().getNamespaceHandlerResolver().resolve(namespaceUri);

      boolean noRecurse = false;
      boolean forceRecurse = false;
      BeanDefinition finalChild = null;
      BeanDefinition currentDefinition = null;

      do {
        ComponentModel componentModel = applicationModelSupplier.get().findComponentDefinitionModel(element);

        if (shouldUseNewMechanism(element)) {
          ComponentModel parentComponentModel =
              applicationModelSupplier.get().findComponentDefinitionModel(element.getParentNode());
          beanDefinitionFactory.resolveComponentRecursively(parentComponentModel, componentModel,
                                                            getReaderContext().getRegistry(), (resolvedComponent, registry) -> {
                                                              if (resolvedComponent.isRoot()) {
                                                                String name = resolvedComponent.getNameAttribute();
                                                                if (name == null) {
                                                                  if (resolvedComponent.getIdentifier()
                                                                      .equals(CONFIGURATION_IDENTIFIER)) {
                                                                    name = OBJECT_MULE_CONFIGURATION;
                                                                  } else if (resolvedComponent.getIdentifier()
                                                                      .equals(FLOW_REF_IDENTIFIER)) {
                                                                    name = AutoIdUtils.uniqueValue("flow-ref."
                                                                        + resolvedComponent.getNameAttribute());
                                                                  } else {
                                                                    name = AutoIdUtils.uniqueValue(resolvedComponent
                                                                        .getIdentifier().toString());
                                                                  }
                                                                }
                                                                BeanDefinitionFactory.checkElementNameUnique(registry, element);
                                                                registry.registerBeanDefinition(name, resolvedComponent
                                                                    .getBeanDefinition());
                                                                postProcessBeanDefinition(resolvedComponent, registry, name);
                                                              }
                                                            }, (mpElement, beanDefinition) -> {
                                                              // We don't want the bean definition to be automatically injected in
                                                              // the parent bean in this cases since the parent is using the new
                                                              // parsing mechanism.
                                                              // Here it will always be a nested element. We use a fake bean
                                                              // definition so it does not try to validate the ID if it thinks is
                                                              // a global element
                                                              return parseDefinitionOrReference(mpElement, BeanDefinitionBuilder
                                                                  .genericBeanDefinition().getBeanDefinition());
                                                            });
          // Do not iterate since this iteration is done iside the resolve component going through childrens
          return null;
        } else {
          if (!element.getLocalName().equals(MULE_ROOT_ELEMENT) && !element.getLocalName().equals(MULE_DOMAIN_ROOT_ELEMENT)
              && !element.getLocalName().equals(POLICY_ROOT_ELEMENT)) {
            if (handler == null) {
              throw new NullPointerException(format("No namespace handler found for '%s' for Mule 3 parsing mechanism; OR "
                  + "No componentModel found for element '%s' for Mule 4 parsing mechanism",
                                                    element.getNamespaceURI(), element.getNodeName()));
            }

            ParserContext parserContext = new ParserContext(getReaderContext(), this, parent);
            finalChild = handler.parse(element, parserContext);
            currentDefinition = finalChild;
            ComponentModel parentComponentModel =
                applicationModelSupplier.get().findComponentDefinitionModel(element.getParentNode());
            if (parentComponentModel != null) {
              finalChild =
                  adaptFilterBeanDefinitions(parentComponentModel,
                                             (org.springframework.beans.factory.support.AbstractBeanDefinition) finalChild);
            }
            Optional<Class> beanDefinitionType = getBeanDefinitionType(currentDefinition);
            if (componentModel != null && beanDefinitionType.isPresent()) {
              // This is required until flow-ref is migrated to the new parsing mechanism.
              componentModel.setType(beanDefinitionType.get());
              componentModel.setBeanDefinition(currentDefinition);
              ComponentModelHelper.addAnnotation(LOCATION_KEY, componentModel.getComponentLocation(),
                                                 componentModel);
            }
            registerBean(element, currentDefinition);
            setComponentModelTypeFromBeanDefinition(finalChild, componentModel);
          }
        }
        noRecurse = noRecurse || testFlag(finalChild, MULE_NO_RECURSE);
        forceRecurse = forceRecurse || testFlag(finalChild, MULE_FORCE_RECURSE);
      } while (null != finalChild && testFlag(finalChild, MULE_REPEAT_PARSE));

      // Only iterate and parse child mule name-spaced elements. Spring does not do
      // hierarchical parsing by default so we need to maintain this behavior
      // for non-mule elements to ensure that we don't break the parsing of any
      // other custom name-spaces e.g spring-jee.

      // We also avoid parsing inside elements that have constructed a factory bean
      // because that means we're dealing with (something like) ChildMapDefinitionParser,
      // which handles iteration internally (this is a hack needed because Spring doesn't
      // expose the DP for "<spring:entry>" elements directly).

      boolean isRecurse;
      if (noRecurse) {
        // no recursion takes precedence, as recursion is set by default
        isRecurse = false;
      } else {
        if (forceRecurse) {
          isRecurse = true;
        } else {
          // default behaviour if no control specified
          isRecurse = SpringXMLUtils.isMuleNamespace(element);
        }
      }

      if (isRecurse) {
        NodeList list = element.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
          if (list.item(i) instanceof Element) {
            parseCustomElement((Element) list.item(i), currentDefinition);
          }
        }
      }

      // If a parser requests post-processing we call again after children called

      if (testFlag(finalChild, MULE_POST_CHILDREN)) {
        ParserContext parserContext = new ParserContext(getReaderContext(), this, parent);
        finalChild = handler.parse(element, parserContext);
      }

      return finalChild;
    }
  }

  private Optional<Class> getBeanDefinitionType(BeanDefinition currentDefinition) {
    try {
      if (currentDefinition == null) {
        return empty();
      }
      if (currentDefinition instanceof AbstractBeanDefinition) {
        return of(((AbstractBeanDefinition) currentDefinition).getBeanClass());
      }
      return of(org.apache.commons.lang.ClassUtils.getClass(currentDefinition.getBeanClassName()));
    } catch (Exception e) {
      return empty();
    }
  }

  private boolean springElementHasCustomParser(Element element) {
    if (element.getLocalName().equals(PROPERTIES_ELEMENT)) {
      return true;
    }
    if (element.getLocalName().equals(ENTRY_ELEMENT)) {
      if (element.getParentNode().getLocalName().equals(PROPERTIES_ELEMENT)) {
        return true;
      }
    }
    return false;
  }

  // TODO MULE-9638 Remove this ugly code since it's not going to be needed anymore.
  private void setComponentModelTypeFromBeanDefinition(BeanDefinition finalChild, ComponentModel componentModel) {
    if (componentModel != null) // This condition is needed when we are parsing something unrelated to mule. See ReferenceTestCase
    {
      if (finalChild != null) {
        try {
          Class<?> type = org.apache.commons.lang.ClassUtils.getClass(finalChild.getBeanClassName());
          if (FactoryBean.class.isAssignableFrom(type)) {
            try {
              // When the FactoryBean implementation implements the FactoryBean directly.
              type = (Class<?>) ((ParameterizedType) org.apache.commons.lang.ClassUtils.getClass(finalChild.getBeanClassName())
                  .getGenericInterfaces()[0])
                      .getActualTypeArguments()[0];
            } catch (Exception e2) {
              try {
                // When the FactoryBean implementation extends a Class that implements FactoryBean.
                type = (Class<?>) ((ParameterizedType) org.apache.commons.lang.ClassUtils.getClass(finalChild.getBeanClassName())
                    .getGenericSuperclass())
                        .getActualTypeArguments()[0];
              } catch (Exception e3) {
                try {
                  // We get the type directly from a FactoryBean instance if it has a default constructor.
                  type = ((FactoryBean) type.newInstance()).getObjectType();
                } catch (InstantiationException e) {
                  type = Object.class;
                }
              }
            }
          }
          componentModel.setType(type);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  /**
   * Determines if the {@code element} must be parsed using the new mechanism or the old one.
   *
   * It will use the new mechanism if it's not a root element or if the parent has not been parsed with the old mechanism or if
   * there's not a {@code org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition} defined for the {@code element}.
   *
   * @param element xml element from the XML configuration file.
   * @return true if the parsing should be done with the new mechanism, false otherwise.
   */
  private boolean shouldUseNewMechanism(Element element) {
    if (element.getLocalName().equals(MULE_ROOT_ELEMENT) || element.getLocalName().equals(MULE_DOMAIN_ROOT_ELEMENT)
        || element.getLocalName().equals(POLICY_ROOT_ELEMENT)) {
      return false;
    }
    Node parentNode = element;
    while ((parentNode = parentNode.getParentNode()) != null) {
      ComponentIdentifier parentComponentIdentifier = getComponentIdentifier(parentNode);
      if (isMuleRootElement(parentComponentIdentifier)) {
        break;
      }
      if (!beanDefinitionFactory.hasDefinition(parentComponentIdentifier, getParentComponentIdentifier(parentNode))) {
        return false;
      }
    }
    ComponentIdentifier elementComponentIdentifier = getComponentIdentifier(element);
    return beanDefinitionFactory.hasDefinition(elementComponentIdentifier, getParentComponentIdentifier(element));
  }

  private Optional<ComponentIdentifier> getParentComponentIdentifier(Node element) {
    if (element.getParentNode() == null || element.getParentNode().getLocalName() == null) {
      return empty();
    }
    return of(getComponentIdentifier(element.getParentNode()));
  }

  private ComponentIdentifier getComponentIdentifier(Node element) {
    String parentNodeNamespace = getNamespace(element);
    String parentNodeName = element.getLocalName();
    return builder().withNamespace(parentNodeNamespace).withName(parentNodeName).build();
  }

  private boolean isMuleRootElement(ComponentIdentifier componentIdentifier) {
    return MULE_IDENTIFIER.equals(componentIdentifier) || MULE_DOMAIN_IDENTIFIER.equals(componentIdentifier) ||
        MULE_EE_DOMAIN_IDENTIFIER.equals(componentIdentifier) || POLICY_IDENTIFIER.equals(componentIdentifier);
  }

  private String getNamespace(Node parentNode) {
    if (parentNode.getPrefix() == null) {
      if (parentNode.getNamespaceURI().equals(CORE_NAMESPACE)) {
        return CORE_PREFIX;
      } else if (parentNode.getNamespaceURI().equals(DOMAIN_NAMESPACE)) {
        return DOMAIN_PREFIX;
      } else if (parentNode.getNamespaceURI().equals(EE_DOMAIN_NAMESPACE)) {
        return EE_DOMAIN_PREFIX;
      } else {
        return POLICY_ROOT_ELEMENT;
      }
    } else {
      final ComponentModel componentDefinitionModel = applicationModelSupplier.get().findComponentDefinitionModel(parentNode);
      if (componentDefinitionModel != null) {
        return componentDefinitionModel.getIdentifier().getNamespace();
      } else {
        return parentNode.getPrefix();
      }
    }
  }

  private void validate(Element element) {
    for (ElementValidator validator : elementValidators) {
      validator.validate(element);
    }
  }

  protected BeanDefinition handleSpringElements(Element element, BeanDefinition parent) {

    // these are only called if they are at a "top level" - if they are nested inside
    // other spring elements then spring will handle them itself

    if (isLocalName(element, BEANS)) {
      // the delegate doesn't support the full spring schema, but it seems that
      // we can invoke the DefaultBeanDefinitionDocumentReader via registerBeanDefinitions
      // but we need to create a new DOM document from the element first
      try {
        Document doc = XMLSecureFactories.createDefault().getDocumentBuilderFactory().newDocumentBuilder().newDocument();
        doc.appendChild(doc.importNode(element, true));
        beanDefinitionDocumentReader.registerBeanDefinitions(doc, getReaderContext());
      } catch (ParserConfigurationException e) {
        throw new RuntimeException(e);
      }
      return parent;
    }

    else if (isLocalName(element, PROPERTY_ELEMENT)) {
      parsePropertyElement(element, parent);
      return parent;
    }

    // i am trying to keep these to a minimum - using anything but "bean" is a recipe
    // for disaster - we already have problems with "property", for example.

    // else if (isLocalName(element, MAP_ELEMENT))
    // {
    // // currently unused?
    // parseMapElement(element, bd);
    // }
    // else if (isLocalName(element, LIST_ELEMENT))
    // {
    // // currently unused?
    // parseListElement(element, bd);
    // }
    // else if (isLocalName(element, SET_ELEMENT))
    // {
    // // currently unused?
    // parseSetElement(element, bd);
    // }

    else if (isLocalName(element, BEAN_ELEMENT)) {
      BeanDefinitionHolder holder = parseBeanDefinitionElement(element, parent);
      registerBeanDefinitionHolder(holder);
      return holder.getBeanDefinition();
    } else {
      throw new IllegalStateException("Unexpected Spring element: " + SpringXMLUtils.elementToString(element));
    }
  }

  protected void registerBean(Element ele, BeanDefinition bd) {
    if (bd == null) {
      return;
    }

    // Check to see if the Bean Definition represents a compound element - one represents a subset of
    // configuration for the parent bean. Compound bean definitions should not be registered since the properties
    // set on them are really set on the parent bean.
    if (!testFlag(bd, MULE_NO_REGISTRATION)) {
      String name = generateChildBeanName(ele);
      logger.debug("register " + name + ": " + bd.getBeanClassName());
      BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(bd, name);
      registerBeanDefinitionHolder(beanDefinitionHolder);
    }
  }

  protected void registerBeanDefinitionHolder(BeanDefinitionHolder bdHolder) {
    // bdHolder = decorateBeanDefinitionIfRequired(ele, bdHolder);
    // Register the final decorated instance.
    BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
    // Send registration event.
    getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
  }

  protected String generateChildBeanName(Element e) {
    String id = SpringXMLUtils.getNameOrId(e);
    if (StringUtils.isBlank(id)) {
      String parentId = SpringXMLUtils.getNameOrId((Element) e.getParentNode());
      return INNER_BEAN_PREFIX + "." + parentId + ":" + e.getLocalName();
    } else {
      return id;
    }
  }

  public static void setFlag(BeanDefinition bean, String flag) {
    bean.setAttribute(flag, Boolean.TRUE);
  }

  public static boolean testFlag(BeanDefinition bean, String flag) {
    return null != bean && bean.hasAttribute(flag) && bean.getAttribute(flag) instanceof Boolean
        && ((Boolean) bean.getAttribute(flag)).booleanValue();
  }


  /**
   * Parse a map element.
   */
  public Map parseMapElement(Element mapEle, String mapElementTagName, String mapElementKeyAttributeName,
                             String mapElementValueAttributeName) {
    List<Element> entryEles = DomUtils.getChildElementsByTagName(mapEle, mapElementTagName);
    ManagedMap<Object, Object> map = new ManagedMap<Object, Object>(entryEles.size());
    map.setSource(extractSource(mapEle));
    map.setMergeEnabled(parseMergeAttribute(mapEle));

    for (Element entryEle : entryEles) {
      // Extract key from attribute or sub-element.
      Object key = buildTypedStringValueForMap(entryEle.getAttribute(mapElementKeyAttributeName), null, entryEle);
      // Extract value from attribute or sub-element.
      Object value = buildTypedStringValueForMap(entryEle.getAttribute(mapElementValueAttributeName), null, entryEle);
      // Add final key and value to the Map.
      map.put(key, value);
    }
    return map;
  }

}
