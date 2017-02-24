/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.rss.transformers;


import static org.jdom2.Namespace.NO_NAMESPACE;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.feed.rss.Content;
import com.rometools.rome.feed.rss.Description;
import com.rometools.rome.feed.rss.Guid;
import com.rometools.rome.feed.rss.Item;
import com.rometools.rome.feed.rss.Source;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedParser;

import static com.rometools.rome.io.impl.DateParser.parseDate;

import com.rometools.rome.io.impl.RSS20Parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

/**
 * Parses RSS item elements and its children.
 * Considers the use of namespaces that were added in RSS 1.0
 */
public class RssParser extends RSS20Parser implements WireFeedParser
{

    private List<Namespace> namespaces;

    @Override
    public String getType()
    {
        return "rss_2.0";
    }

    @Override
    public boolean isMyType(Document document)
    {
        return true;
    }

    @Override
    protected Namespace getRSSNamespace()
    {
        return NO_NAMESPACE;
    }

    @Override
    public Item parseItem(final Element rssRoot, final Element itemElement, final Locale locale)
    {
        final Item item = super.parseItem(rssRoot, itemElement, locale);
        parseTitle(item, itemElement);
        parseLink(item, itemElement);
        parseDescription(item, itemElement);
        parseAuthor(item, itemElement);
        parseComments(item, itemElement);
        parseSource(item, itemElement);
        parseGuid(item, itemElement);
        parsePubDate(item, itemElement, locale);
        parseExpirationDate(item, itemElement, locale);
        parseContent(item, itemElement);
        parseCategories(item, itemElement);

        return item;
    }

    private Element parseChildWithNamespaces(Element parentElement, String childName)
    {
        Element element = null;
        Iterator namespacesIterator = namespaces.iterator();

        while (namespacesIterator.hasNext() && element == null)
        {
            Namespace namespace = (Namespace) namespacesIterator.next();
            element = parentElement.getChild(childName, namespace);
        }

        return element;
    }

    private List<Element> parseChildrenWithNamespaces(Element parentElement, String childName)
    {
        List<Element> elements = new ArrayList<>();
        Iterator namespacesIterator = namespaces.iterator();

        while (namespacesIterator.hasNext() && elements.size() == 0)
        {
            Namespace namespace = (Namespace) namespacesIterator.next();
            elements = parentElement.getChildren(childName, namespace);
        }

        return elements;
    }

    @Override
    public WireFeed parse(final Document document, final boolean validate, final Locale locale) throws IllegalArgumentException, FeedException
    {
        final Element rssRoot = document.getRootElement();
        namespaces = new ArrayList<>(rssRoot.getAdditionalNamespaces());
        namespaces.add(NO_NAMESPACE);
        return parseChannel(rssRoot, locale);
    }

    private void parseTitle(Item item, final Element itemElement)
    {
        if (item.getTitle() == null)
        {
            final Element title = parseChildWithNamespaces(itemElement, "title");
            if (title != null)
            {
                item.setTitle(title.getText());
            }
        }
    }

    private void parseLink(Item item, final Element itemElement)
    {
        if (item.getLink() == null)
        {
            final Element link = parseChildWithNamespaces(itemElement, "link");
            if (link != null)
            {
                item.setLink(link.getText());
                item.setUri(link.getText());
            }
        }
    }

    private void parseDescription(Item item, final Element itemElement)
    {
        if (item.getDescription() == null)
        {
            final Element description = parseChildWithNamespaces(itemElement, "description");
            if (description != null)
            {
                Description descriptionBean = new Description();
                descriptionBean.setValue(description.getText());
                item.setDescription(descriptionBean);
                final String type = description.getAttributeValue("type");
                if (type != null)
                {
                    item.getDescription().setType(type);
                }
            }
        }
    }

    private void parseAuthor(Item item, final Element itemElement)
    {
        if (item.getAuthor() == null)
        {
            final Element author = parseChildWithNamespaces(itemElement, "author");

            if (author != null)
            {
                item.setAuthor(author.getText());
            }

        }
    }

    private void parseComments(Item item, final Element itemElement)
    {
        if (item.getComments() == null)
        {
            final Element comments = parseChildWithNamespaces(itemElement, "comments");

            if (comments != null)
            {
                item.setComments(comments.getText());
            }
        }
    }

    private void parseSource(Item item, final Element itemElement)
    {
        if (item.getSource() == null)
        {
            final Element source = parseChildWithNamespaces(itemElement, "source");
            if (source != null)
            {
                Source sourceBean = new Source();
                sourceBean.setValue(source.getText());
                final String url = source.getAttributeValue("url");
                sourceBean.setUrl(url);
                item.setSource(sourceBean);
            }
        }
    }

    private void parseGuid(Item item, final Element itemElement)
    {
        if (item.getGuid() == null)
        {
            final Element guid = parseChildWithNamespaces(itemElement, "guid");
            if (guid != null)
            {
                Guid guidBean = new Guid();
                final String isPermaLink = guid.getAttributeValue("isPermaLink");

                if (isPermaLink != null)
                {
                    guidBean.setPermaLink(isPermaLink.equalsIgnoreCase("true"));
                }
                guidBean.setValue(guid.getText());
                item.setGuid(guidBean);
            }
        }
    }

    private void parsePubDate(Item item, final Element itemElement, Locale locale)
    {
        if (item.getPubDate() == null)
        {
            final Element pubDate = parseChildWithNamespaces(itemElement, "pubDate");

            if (pubDate != null)
            {
                item.setPubDate(parseDate(pubDate.getText(), locale));
            }
        }
    }

    private void parseExpirationDate(Item item, final Element itemElement, Locale locale)
    {
        if (item.getExpirationDate() == null)
        {
            final Element expirationDate = parseChildWithNamespaces(itemElement, "expirationDate");
            if (expirationDate != null)
            {
                item.setExpirationDate(parseDate(expirationDate.getText(), locale));
            }
        }
    }

    private void parseContent(Item item, final Element itemElement)
    {
        if (item.getContent() == null)
        {
            final Element encoded = parseChildWithNamespaces(itemElement, "encoded");
            if (encoded != null)
            {
                final Content content = new Content();
                content.setType(Content.HTML);
                content.setValue(encoded.getText());
                item.setContent(content);
            }
        }
    }

    private void parseCategories(Item item, final Element itemElement)
    {
        if (item.getCategories() != null && item.getCategories().size() == 0)
        {
            final List<Element> categories = parseChildrenWithNamespaces(itemElement, "category");
            item.setCategories(parseCategories(categories));
        }
    }

}