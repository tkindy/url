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

import static java.util.function.Predicate.isEqual;

import com.google.common.collect.ImmutableMap;
import com.tylerkindy.url.UrlParseResult.Failure;
import com.tylerkindy.url.UrlParseResult.Success;
import com.tylerkindy.url.UrlPath.NonOpaque;
import com.tylerkindy.url.UrlPath.Opaque;
import com.tylerkindy.url.ValidationError.HostMissing;
import com.tylerkindy.url.ValidationError.InvalidCredentials;
import com.tylerkindy.url.ValidationError.InvalidReverseSolidus;
import com.tylerkindy.url.ValidationError.InvalidUrlUnit;
import com.tylerkindy.url.ValidationError.MissingSchemeNonRelativeUrl;
import com.tylerkindy.url.ValidationError.PortInvalid;
import com.tylerkindy.url.ValidationError.PortOutOfRange;
import com.tylerkindy.url.ValidationError.SpecialSchemeMissingFollowingSolidus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class UrlParser {
  public static final UrlParser INSTANCE = new UrlParser();

  private static final Set<String> SPECIAL_SCHEMES =
      Set.of("ftp", "file", "http", "https", "ws", "wss");
  private static final Map<String, Character> DEFAULT_PORTS = ImmutableMap.<String, Character>builder()
      .put("ftp", (char) 21)
      .put("http", (char) 80)
      .put("https", (char) 443)
      .put("ws", (char) 80)
      .put("wss", (char) 443)
      .build();

  private UrlParser() {}

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public UrlParseResult parse(String urlStr, Optional<Url> base) {
    List<ValidationError> errors = new ArrayList<>();
    urlStr = removeControlAndWhitespaceCharacters(urlStr, errors);

    State state = State.SCHEME_START;
    final StringBuilder buffer = new StringBuilder();
    boolean atSignSeen = false;
    boolean insideBrackets = false;
    boolean passwordTokenSeen = false;
    Pointer pointer = new Pointer(urlStr);

    String scheme = "";
    StringBuilder username = new StringBuilder();
    StringBuilder password = new StringBuilder();
    Host host = null;
    Character port = null;
    UrlPath path = new NonOpaque(List.of());
    String query = null;
    String fragment = null;

    stateLoop:
    while (true) {
      switch (state) {
        case SCHEME_START -> {
          int c = pointer.getCurrentCodePoint();
          if (isAsciiAlpha(c)) {
            buffer.appendCodePoint(Character.toLowerCase(c));
            state = State.SCHEME;
          } else {
            state = State.NO_SCHEME;
            pointer.decrease();
          }
        }
        case SCHEME -> {
          int c = pointer.getCurrentCodePoint();
          if (isAsciiAlphanumeric(c) || c == '+' || c == '-' || c == '.') {
            buffer.appendCodePoint(Character.toLowerCase(c));
          } else if (c == ':') {
            scheme = buffer.toString();
            buffer.delete(0, buffer.length());

            if (scheme.equals("file")) {
              if (!pointer.doesRemainingStartWith("//")) {
                errors.add(new SpecialSchemeMissingFollowingSolidus());
              }
              state = State.FILE;
            } else if (SPECIAL_SCHEMES.contains(scheme)
                && base.map(Url::scheme).filter(isEqual(scheme)).isPresent()) {
              state = State.SPECIAL_RELATIVE_OR_AUTHORITY;
            } else if (SPECIAL_SCHEMES.contains(scheme)) {
              state = State.SPECIAL_AUTHORITY_SLASHES;
            } else if (pointer.doesRemainingStartWith("/")) {
              state = State.PATH_OR_AUTHORITY;
              pointer.increase();
            } else {
              path = new Opaque("");
              state = State.OPAQUE_PATH;
            }
          } else {
            buffer.delete(0, buffer.length());
            state = State.NO_SCHEME;
            pointer.reset();
            pointer.decrease();
          }
        }
        case NO_SCHEME -> {
          if (base.isEmpty()
              || (base.get().path() instanceof Opaque && pointer.getCurrentCodePoint() != '#')) {
            errors.add(new MissingSchemeNonRelativeUrl());
            return new Failure(errors);
          } else if (base.get().path() instanceof Opaque) {
            scheme = base.get().scheme();
            path = base.get().path();
            query = base.get().query().orElse(null);
            fragment = "";
            state = State.FRAGMENT;
          } else if (!base.get().scheme().equals("file")) {
            state = State.RELATIVE;
            pointer.decrease();
          } else {
            state = State.FILE;
            pointer.decrease();
          }
        }
        case SPECIAL_RELATIVE_OR_AUTHORITY -> {
          if (pointer.getCurrentCodePoint() == '/' && pointer.doesRemainingStartWith("/")) {
            state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
            pointer.increase();
          } else {
            errors.add(new SpecialSchemeMissingFollowingSolidus());
            state = State.RELATIVE;
            pointer.decrease();
          }
        }
        case PATH_OR_AUTHORITY -> {
          if (pointer.getCurrentCodePoint() == '/') {
            state = State.AUTHORITY;
          } else {
            state = State.PATH;
            pointer.decrease();
          }
        }
        case RELATIVE -> {
          scheme = base.get().scheme();
          if (pointer.getCurrentCodePoint() == '/') {
            state = State.RELATIVE_SLASH;
          } else if (SPECIAL_SCHEMES.contains(scheme) && pointer.getCurrentCodePoint() == '\\') {
            errors.add(new InvalidReverseSolidus());
            state = State.RELATIVE_SLASH;
          } else {
            username = new StringBuilder(base.get().username());
            password = new StringBuilder(base.get().password());
            host = base.get().host().orElse(null);
            port = base.get().port().orElse(null);
            path = base.get().path().copy();
            query = base.get().query().orElse(null);

            if (pointer.getCurrentCodePoint() == '?') {
              query = "";
              state = State.QUERY;
            } else if (pointer.getCurrentCodePoint() == '#') {
              fragment = "";
              state = State.FRAGMENT;
            } else if (!pointer.isEof()) {
              query = null;

              if (path instanceof NonOpaque(var segments) && !segments.isEmpty()) {
                path = new NonOpaque(segments.subList(0, segments.size() - 1));
              }

              state = State.PATH;
              pointer.decrease();
            }
          }
        }
        case RELATIVE_SLASH -> {
          if (SPECIAL_SCHEMES.contains(scheme) && (pointer.getCurrentCodePoint() == '/' || pointer.getCurrentCodePoint() == '\\')) {
            if (pointer.getCurrentCodePoint() == '\\') {
              errors.add(new InvalidReverseSolidus());
            }
            state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
          } else if (pointer.getCurrentCodePoint() == '/') {
            state = State.AUTHORITY;
          } else {
            username = new StringBuilder(base.get().username());
            password = new StringBuilder(base.get().password());
            host = base.get().host().orElse(null);
            port = base.get().port().orElse(null);
            state = State.PATH;
            pointer.decrease();
          }
        }
        case SPECIAL_AUTHORITY_SLASHES -> {
          if (pointer.getCurrentCodePoint() == '/' && pointer.doesRemainingStartWith("/")) {
            state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
            pointer.increase();
          } else {
            errors.add(new SpecialSchemeMissingFollowingSolidus());
            state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
            pointer.decrease();
          }
        }
        case SPECIAL_AUTHORITY_IGNORE_SLASHES -> {
          if (pointer.getCurrentCodePoint() != '/' && pointer.getCurrentCodePoint() != '\\') {
            state = State.AUTHORITY;
            pointer.decrease();
          } else {
            errors.add(new SpecialSchemeMissingFollowingSolidus());
          }
        }
        case AUTHORITY -> {
          if (!pointer.isEof() && pointer.getCurrentCodePoint() == '@') {
            errors.add(new InvalidCredentials());
            if (atSignSeen) {
              buffer.insert(0, "%40");
            }
            atSignSeen = true;
            Iterable<Integer> bufferCodePoints = () -> buffer.codePoints().iterator();
            for (int codePoint : bufferCodePoints) {
              if (codePoint == ':' && !passwordTokenSeen) {
                passwordTokenSeen = true;
                continue;
              }

              String encodedCodePoints = PercentEncoder.utf8PecentEncode(codePoint, PercentEncoder.USERINFO);
              if (passwordTokenSeen) {
                password.append(encodedCodePoints);
              } else {
                username.append(encodedCodePoints);
              }
            }

            buffer.delete(0, buffer.length());
          } else if (
              (pointer.isEof() || pointer.getCurrentCodePoint() == '/' || pointer.getCurrentCodePoint() == '?' || pointer.getCurrentCodePoint() == '#') ||
                  (SPECIAL_SCHEMES.contains(scheme) && pointer.getCurrentCodePoint() == '\\')
          ) {
            if (atSignSeen && buffer.isEmpty()) {
              errors.add(new HostMissing());
              return new Failure(errors);
            }
            pointer.decrease(buffer.codePointCount(0, buffer.length()) + 1);
            buffer.delete(0, buffer.length());
            state = State.HOST;
          } else {
            buffer.appendCodePoint(pointer.getCurrentCodePoint());
          }
        }
        case HOST, HOSTNAME -> {
          if (!pointer.isEof() && pointer.getCurrentCodePoint() == ':' && !insideBrackets) {
            if (buffer.isEmpty()) {
              errors.add(new HostMissing());
              return new Failure(errors);
            }
            Optional<Host> maybeHost = HostParser.parseHost(buffer.toString(), !SPECIAL_SCHEMES.contains(scheme), errors);
            if (maybeHost.isEmpty()) {
              return new Failure(errors);
            }
            host = maybeHost.get();
            buffer.delete(0, buffer.length());
            state = State.PORT;
          } else if (
              (pointer.isEof() || pointer.getCurrentCodePoint() == '/' || pointer.getCurrentCodePoint() == '?' || pointer.getCurrentCodePoint() == '#') ||
                  (SPECIAL_SCHEMES.contains(scheme) && pointer.getCurrentCodePoint() == '\\')
          ) {
            pointer.decrease();
            if (SPECIAL_SCHEMES.contains(scheme) && buffer.isEmpty()) {
              errors.add(new HostMissing());
              return new Failure(errors);
            }
            Optional<Host> maybeHost = HostParser.parseHost(buffer.toString(), !SPECIAL_SCHEMES.contains(scheme), errors);
            if (maybeHost.isEmpty()) {
              return new Failure(errors);
            }
            host = maybeHost.get();
            buffer.delete(0, buffer.length());
            state = State.PATH_START;
          } else {
            if (pointer.getCurrentCodePoint() == '[') {
              insideBrackets = true;
            }
            if (pointer.getCurrentCodePoint() == ']') {
              insideBrackets = false;
            }
            buffer.appendCodePoint(pointer.getCurrentCodePoint());
          }
        }
        case PORT -> {
          if (!pointer.isEof() && pointer.getCurrentCodePoint() >= '0'
              && pointer.getCurrentCodePoint() <= '9') {
            buffer.appendCodePoint(pointer.getCurrentCodePoint());
          } else if (
              (
                  pointer.isEof() ||
                      pointer.getCurrentCodePoint() == '/' ||
                      pointer.getCurrentCodePoint() == '?' ||
                      pointer.getCurrentCodePoint() == '#'
              ) ||
                  (SPECIAL_SCHEMES.contains(scheme) && pointer.getCurrentCodePoint() == '\\')
          ) {
            if (!buffer.isEmpty()) {
              int portInt = Integer.parseInt(buffer.toString());
              if (portInt > Character.MAX_VALUE) {
                errors.add(new PortOutOfRange());
                return new Failure(errors);
              }

              char portChar = (char) portInt;

              if (portChar == DEFAULT_PORTS.get(scheme)) {
                port = null;
              } else {
                port = portChar;
              }

              buffer.delete(0, buffer.length());
            }

            state = State.PATH_START;
            pointer.decrease();
          } else {
            errors.add(new PortInvalid());
            return new Failure(errors);
          }
        }
        default -> {
          break stateLoop; // TODO: remove
        }
      }

      if (pointer.isEof()) {
        break;
      } else {
        pointer.increase();
      }
    }

    return new Success(new Url(scheme, username.toString(), password.toString(), host, port, path, query, fragment));
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
    return c == '\t' || c == '\f' || c == '\r' || c == '\n';
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
    SPECIAL_RELATIVE_OR_AUTHORITY,
    PATH_OR_AUTHORITY,
    RELATIVE,
    RELATIVE_SLASH,
    SPECIAL_AUTHORITY_SLASHES,
    SPECIAL_AUTHORITY_IGNORE_SLASHES,
    AUTHORITY,
    HOST,
    HOSTNAME,
    PORT,
    FILE,
    FILE_SLASH,
    FILE_HOST,
    PATH_START,
    PATH,
    OPAQUE_PATH,
    QUERY,
    FRAGMENT,
  }
}
