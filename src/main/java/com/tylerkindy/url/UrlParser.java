/*
 * Copyright 2024 Tyler Kindy.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tylerkindy.url;

import com.tylerkindy.url.UrlParseResult.Success;
import com.tylerkindy.url.ValidationError.InvalidUrlUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class UrlParser {
  public static final UrlParser INSTANCE = new UrlParser();

  private UrlParser() {
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public UrlParseResult parse(String urlStr, Optional<Url> base) {
    List<ValidationError> errors = new ArrayList<>();
    urlStr = removeControlAndWhitespaceCharacters(urlStr, errors);

    State state = State.SCHEME_START;
    StringBuilder buffer = new StringBuilder();
    boolean atSignSeen = false;
    boolean insideBrackets = false;
    boolean passwordTokenSeen = false;
    Pointer pointer = new Pointer(urlStr);

    String scheme = "";

    boolean shouldAdvance;
    stateLoop: while (!pointer.isEof()) {
      shouldAdvance = true;

      switch (state) {
        case SCHEME_START -> {
          int c = pointer.getCurrentCodePoint();
          if (isAsciiAlpha(c)) {
            buffer.appendCodePoint(Character.toLowerCase(c));
            state = State.SCHEME;
          } else {
            state = State.NO_SCHEME;
            shouldAdvance = false;
          }
        }
        case SCHEME -> {
          int c = pointer.getCurrentCodePoint();
          if (isAsciiAlphanumeric(c) || c == '+' || c == '-' || c == '.') {
            buffer.appendCodePoint(Character.toLowerCase(c));
          } else if (c == ':') {
            scheme = buffer.toString();
            buffer = new StringBuilder();
            break stateLoop; // TODO: implement the rest
          }
        }
      }

      if (shouldAdvance) {
        pointer.advance();
      }
    }

    return new Success(new Url(scheme));
  }

  private String removeControlAndWhitespaceCharacters(String urlStr, List<ValidationError> errors) {
    if (urlStr.isEmpty()) {
      return urlStr;
    }

    int prefixEndIndex = 0;
    while (isC0ControlOrSpace(Character.codePointAt(urlStr, prefixEndIndex))) {
      prefixEndIndex += 1;
    }

    int suffixStartIndex = urlStr.length() - 1;
    while (isC0ControlOrSpace(Character.codePointAt(urlStr, suffixStartIndex))) {
      suffixStartIndex -= 1;
    }

    if (prefixEndIndex > 0 || suffixStartIndex < urlStr.length() - 1) {
      errors.add(new InvalidUrlUnit("leading or trailing C0 control or space"));
    }

    urlStr = urlStr.substring(prefixEndIndex, suffixStartIndex + 1);

    StringBuilder sb = new StringBuilder(urlStr.length());
    boolean hasTabOrNewline = false;
    for (int i = 0; i < urlStr.length(); i++) {
      char c = urlStr.charAt(i);
      if (!isAsciiTabOrNewline(c)) {
        sb.append(c);
      } else {
        hasTabOrNewline = true;
      }
    }

    if (hasTabOrNewline) {
      errors.add(new InvalidUrlUnit("tab or newline"));
    }
    return sb.toString();
  }

  private boolean isC0ControlOrSpace(int codePoint) {
    return isC0Control(codePoint) || codePoint == ' ';
  }

  private boolean isC0Control(int codePoint) {
    return codePoint >= 0x0 && codePoint <= 0x1F;
  }

  private boolean isAsciiTabOrNewline(int c) {
    return c == '\t' || c == '\f' || c == '\r';
  }

  private boolean isAsciiAlpha(int codePoint) {
    return isAsciiLowerAlpha(codePoint) || isAsciiUpperAlpha(codePoint);
  }

  private boolean isAsciiLowerAlpha(int codePoint) {
    return codePoint >= 'a' && codePoint <= 'z';
  }

  private boolean isAsciiUpperAlpha(int codePoint) {
    return codePoint >= 'A' && codePoint <= 'Z';
  }

  private boolean isAsciiAlphanumeric(int codePoint) {
    return isAsciiAlpha(codePoint) || isAsciiDigit(codePoint);
  }

  private boolean isAsciiDigit(int codePoint) {
    return codePoint >= '0' && codePoint <= '9';
  }

  private enum State {
    SCHEME_START,
    SCHEME,
    NO_SCHEME,
  }
}
