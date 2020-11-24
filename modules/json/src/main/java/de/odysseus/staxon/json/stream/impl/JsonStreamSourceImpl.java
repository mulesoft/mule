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
package de.odysseus.staxon.json.stream.impl;

import de.odysseus.staxon.json.stream.JsonStreamSource;
import de.odysseus.staxon.json.stream.JsonStreamToken;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static de.odysseus.staxon.util.Constants.MAX_DEPTH;

/**
 * Default <code>JsonStreamSource</code> implementation.
 */
class JsonStreamSourceImpl implements JsonStreamSource {
	/**
	 * Scanner interface
	 */
	interface Scanner extends Closeable {
		enum Symbol {
		    START_OBJECT,
		    END_OBJECT,
		    START_ARRAY,
		    END_ARRAY,
		    COLON,
		    COMMA,
		    STRING,
		    NUMBER,
		    TRUE,
		    FALSE,
		    NULL,
		    EOF;
		}
		Symbol nextSymbol() throws IOException;
		String getText();

		int getCharOffset();
		int getLineNumber();
		int getColumnNumber();
	}
	
	private final Scanner scanner;
	private final boolean[] arrays = new boolean[MAX_DEPTH];
	private final boolean closeScanner;

	private JsonStreamToken token = null;
	private Scanner.Symbol symbol = null;
	private int depth = 0;
	private boolean peeked = false;

	private int lineNumber;
	private int columnNumber;
	private int charOffset;
	
	
	JsonStreamSourceImpl(Scanner scanner, boolean closeScanner) {
		this.scanner = scanner;
		this.closeScanner = closeScanner;
		this.lineNumber = scanner.getLineNumber();
		this.columnNumber = scanner.getColumnNumber();
		this.charOffset = scanner.getCharOffset();
	}

	private JsonStreamToken startJsonValue() throws IOException {
		switch (symbol) {
		case FALSE:
		case NULL:
		case NUMBER:
		case TRUE:
		case STRING:
			return JsonStreamToken.VALUE;
		case START_ARRAY:
			if (arrays[depth]) {
				throw new IOException("Already in an array");
			}
			arrays[depth] = true;
			return JsonStreamToken.START_ARRAY;
		case START_OBJECT:
			depth++;
			return JsonStreamToken.START_OBJECT;
		default:
			throw new IOException("Unexpected symbol: " + symbol);
		}
	}
	
	private void require(Scanner.Symbol expected) throws IOException {
		if (symbol != expected) {
			throw new IOException("Unexpected symbol:" + symbol);
		}		
	}

	private JsonStreamToken next() throws IOException {
		symbol = scanner.nextSymbol();
		if (symbol == Scanner.Symbol.EOF) {
			if (depth != 0 || arrays[depth]) {
				throw new IOException("Premature EOF");
			}
			return JsonStreamToken.NONE;
		}
		if (token == null) {
			return startJsonValue();
		}
		switch (token) {
		case NAME:
			require(Scanner.Symbol.COLON);
			symbol = scanner.nextSymbol();
			return startJsonValue();
		case END_OBJECT:
		case END_ARRAY:
		case VALUE:
			switch (symbol) {
			case COMMA:
				symbol = scanner.nextSymbol();
				if (arrays[depth]) {
					return startJsonValue();
				} else {
					require(Scanner.Symbol.STRING);
					return JsonStreamToken.NAME;
				}
			case END_ARRAY:
				if (!arrays[depth]) {
					throw new IOException("Not in an array");
				}
				arrays[depth] = false;
				return JsonStreamToken.END_ARRAY;
			case END_OBJECT:
				if (arrays[depth]) {
					throw new IOException("Unclosed array");
				}
				if (depth == 0) {
					throw new IOException("Not in an object");
				}
				depth--;
				return JsonStreamToken.END_OBJECT;
			default:
				throw new IOException("Unexpected symbol: " + symbol);
			}
		case START_OBJECT:
			switch (symbol) {
			case END_OBJECT:
				depth--;
				return JsonStreamToken.END_OBJECT;
			case STRING:
				return JsonStreamToken.NAME;
			default:
				throw new IOException("Unexpected symbol: " + symbol);
			}
		case START_ARRAY:
			switch (symbol) {
			case END_ARRAY:
				arrays[depth] = false;
				return JsonStreamToken.END_ARRAY;
			default:
				return startJsonValue();
			}
		default:
			throw new IOException("Unexpected token: " + token);
		}
	}
	
	@Override
	public void close() throws IOException {
		if (closeScanner) {
			scanner.close();
		}
	}

	/**
	 * Make the next token the current token.
	 * Save location info from scanner to prevent changing location by peek()
	 * @param token expected token
	 * @throws IOException
	 */
	private void poll(JsonStreamToken token) throws IOException {
		if (token != peek()) {
			throw new IOException("Unexpected token: " + peek());
		}
		lineNumber = scanner.getLineNumber();
		columnNumber = scanner.getColumnNumber();
		charOffset = scanner.getCharOffset();
		peeked = false;
	}
	
	@Override
	public String name() throws IOException {
		poll(JsonStreamToken.NAME);
		return scanner.getText();
	}

	@Override
	public Value value() throws IOException {
		poll(JsonStreamToken.VALUE);
		switch (symbol) {
		case NULL:
			return NULL;
		case STRING:
			return new Value(scanner.getText());
		case TRUE:
			return TRUE;
		case FALSE:
			return FALSE;
		case NUMBER:
			if (scanner.getText().indexOf('.') < 0 && scanner.getText().toLowerCase().indexOf('e') < 0) {
				return new Value(scanner.getText(), new BigInteger(scanner.getText()));
			} else {
				return new Value(scanner.getText(), new BigDecimal(scanner.getText()));
			}
		default:
			throw new IOException("Not a value token: " + symbol);
		}
	}

	@Override
	public void startObject() throws IOException {
		poll(JsonStreamToken.START_OBJECT);
	}

	@Override
	public void endObject() throws IOException {
		poll(JsonStreamToken.END_OBJECT);
	}

	@Override
	public void startArray() throws IOException {
		poll(JsonStreamToken.START_ARRAY);
	}

	@Override
	public void endArray() throws IOException {
		poll(JsonStreamToken.END_ARRAY);
	}
	
	@Override
	public JsonStreamToken peek() throws IOException {
		if (!peeked) {
			token = next();
			peeked = true;
		}
		return token;
	}
	
	@Override
	public int getLineNumber() {
		return lineNumber + 1;
	}
	
	@Override
	public int getColumnNumber() {
		return columnNumber + 1;
	}
	
	@Override
	public int getCharacterOffset() {
		return charOffset;
	}
	
	@Override
	public String getPublicId() {
		return null;
	}

	@Override
	public String getSystemId() {
		return null;
	}
}
