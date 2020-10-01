/*
 * Copyright 2011, 2012 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.odysseus.staxon.json.util;

import de.odysseus.staxon.event.SimpleXMLEventFactory;
import de.odysseus.staxon.json.JsonXMLStreamConstants;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.ProcessingInstruction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static de.odysseus.staxon.util.Constants.MAX_DEPTH;

/**
 * Package-private helper used by {@link XMLMultipleStreamWriter} and {@link XMLMultipleEventWriter}
 * to handle path matching and insert <code>&lt;xml-multiple&gt;</code> processing instruction events.
 */
class XMLMultipleProcessingInstructionHandler {
	private static final ProcessingInstruction MULTIPLE_PI =
			new SimpleXMLEventFactory().createProcessingInstruction(JsonXMLStreamConstants.MULTIPLE_PI_TARGET, null);
	
	/**
	 * Processing instruction writer
	 */
	private static abstract class ProcessingInstructionWriter {
		abstract void add(ProcessingInstruction pi) throws XMLStreamException;
	}
	
	private final StringBuilder path = new StringBuilder();
	private final Set<String> absoluteMultiplePaths = new HashSet<String>();
	private final List<String> relativeMultiplePaths = new ArrayList<String>();
	private final String[] names = new String[MAX_DEPTH];
	private final boolean matchRoot;
	private final boolean matchPrefixes;

	private final Pattern pathPattern;
	private final ProcessingInstructionWriter writer;

	private String previousSiblingName = null;
	private int depth = 0;

	XMLMultipleProcessingInstructionHandler(final XMLStreamWriter writer, boolean matchRoot, boolean matchPrefixes) {
		this(new ProcessingInstructionWriter() {
			@Override
			void add(ProcessingInstruction pi) throws XMLStreamException {
				if (pi.getData() == null) {
					writer.writeProcessingInstruction(pi.getTarget());
				} else {
					writer.writeProcessingInstruction(pi.getTarget(), pi.getData());
				}
			}
		}, matchRoot, matchPrefixes);
	}
	
	XMLMultipleProcessingInstructionHandler(final XMLEventWriter writer, boolean matchRoot, boolean matchPrefixes) {
		this(new ProcessingInstructionWriter() {
			@Override
			void add(ProcessingInstruction pi) throws XMLStreamException {
				writer.add(pi);
			}
		}, matchRoot, matchPrefixes);
	}

	private XMLMultipleProcessingInstructionHandler(ProcessingInstructionWriter writer, boolean matchRoot, boolean matchPrefixes) {
		this.matchRoot = matchRoot;
		this.matchPrefixes = matchPrefixes;
		this.writer = writer;

		/*
		 * determine path pattern
		 */
		String identifier = "\\w(-?\\w)*";
		String name = matchPrefixes ? "(" + identifier + ":)?" + identifier : identifier;
		pathPattern = Pattern.compile("/?" + name + "(/" + name + ")*");
	}
	
	private boolean matches() {
		if (absoluteMultiplePaths.contains(path.toString())) {
			return true;
		}
		if (!relativeMultiplePaths.isEmpty()) {
			for (String suffix : relativeMultiplePaths) {
				if (path.toString().endsWith(suffix) && path.charAt(path.length() - suffix.length() - 1) == '/') {
					return true;
				}
			}
		}
		return false;
	}

	private void push(String name) throws XMLStreamException {
		if (matchRoot || depth > 0) {			
			path.append('/').append(name);
		}

		if (!name.equals(previousSiblingName) && matches()) {
			writer.add(MULTIPLE_PI);
		}

		names[depth] = name;
		previousSiblingName = null;
		depth++;
	}
	
	private void pop() {
		depth--;
		previousSiblingName = names[depth];
		names[depth] = null;

		if (matchRoot || depth > 0) {			
			path.setLength(path.length() - previousSiblingName.length() - 1);
		}
	}

	/**
	 * Add path to trigger <code>&lt;?xml-multiple?></code> PI.
	 * The path may start with <code>'/'</code> and contain element
	 * names, separated by <code>'/'</code>, e.g
	 * <code>"/foo/bar"</code>, <code>"foo/bar"</code> or <code>"bar"</code>.
	 * If <code>matchPrefixes</code> isv set to <code>true</code>, element
	 * names may be prefixed, e.g. <code>"p:foo/bar"</code>.
	 * @param path
	 */
	void addMultiplePath(String path) throws XMLStreamException {
		if (!pathPattern.matcher(path).matches()) {
			throw new XMLStreamException("multiple path does not match " + pathPattern.pattern());
		}
		if (path.charAt(0) == '/') {
			absoluteMultiplePaths.add(path);
		} else {
			relativeMultiplePaths.add(path);
		}
	}
	
	void preStartElement(String prefix, String localPart) throws XMLStreamException {
		if (matchPrefixes) {
			push(XMLConstants.DEFAULT_NS_PREFIX.equals(prefix) ? localPart : prefix + ':' + localPart);
		} else {
			push(localPart);
		}
	}
	
	void postStartElement() throws XMLStreamException {
		// do nothing
	}
	
	void preEndElement() throws XMLStreamException {
		// do nothing
	}
	
	void postEndElement() throws XMLStreamException {
		pop();
	}
	
	void preEmptyElement(String prefix, String localPart) throws XMLStreamException {
		preStartElement(prefix, localPart);
	}
	
	void postEmptyElement() throws XMLStreamException {
		postStartElement();
		preEndElement();
		postEndElement();
	}
}
