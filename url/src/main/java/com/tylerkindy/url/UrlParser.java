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

import static com.tylerkindy.url.CharacterUtils.isAsciiAlpha;
import static com.tylerkindy.url.CharacterUtils.isAsciiAlphanumeric;
import static com.tylerkindy.url.CharacterUtils.isAsciiDigit;
import static com.tylerkindy.url.CharacterUtils.isAsciiTabOrNewline;
import static com.tylerkindy.url.CharacterUtils.isC0ControlOrSpace;
import static com.tylerkindy.url.CharacterUtils.isNormalizedWindowsDriveLetter;
import static com.tylerkindy.url.CharacterUtils.isUrlCodePoint;
import static com.tylerkindy.url.CharacterUtils.isWindowsDriveLetter;
import static java.util.function.Predicate.isEqual;

import com.google.common.collect.ImmutableMap;
import com.tylerkindy.url.Host.Domain;
import com.tylerkindy.url.Host.Empty;
import com.tylerkindy.url.Pointer.PointedAt;
import com.tylerkindy.url.Pointer.PointedAt.CodePoint;
import com.tylerkindy.url.Pointer.PointedAt.Eof;
import com.tylerkindy.url.UrlParseResult.Failure;
import com.tylerkindy.url.UrlParseResult.Success;
import com.tylerkindy.url.UrlPath.NonOpaque;
import com.tylerkindy.url.UrlPath.Opaque;
import com.tylerkindy.url.ValidationError.FileInvalidWindowsDriveLetter;
import com.tylerkindy.url.ValidationError.FileInvalidWindowsDriveLetterHost;
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
    StringBuilder query = null;
    StringBuilder fragment = null;

    while (true) {
      switch (state) {
        case SCHEME_START -> {
          if (pointer.pointedAt() instanceof CodePoint cp && isAsciiAlpha(cp.codePoint())) {
            buffer.appendCodePoint(Character.toLowerCase(cp.codePoint()));
            state = State.SCHEME;
          } else {
            state = State.NO_SCHEME;
            pointer.decrease();
          }
        }
        case SCHEME -> {
          PointedAt pointedAt = pointer.pointedAt();
          if (pointedAt instanceof CodePoint cp && (
              isAsciiAlphanumeric(cp.codePoint()) ||
                  cp.codePoint() == '+' ||
                  cp.codePoint() == '-' ||
                  cp.codePoint() == '.'
          )) {
            buffer.appendCodePoint(Character.toLowerCase(cp.codePoint()));
          } else if (pointedAt instanceof CodePoint cp && cp.codePoint() == ':') {
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
              || (base.get().path() instanceof Opaque && !(pointer.pointedAt() instanceof CodePoint cp && cp.codePoint() == '#'))) {
            errors.add(new MissingSchemeNonRelativeUrl());
            return new Failure(errors);
          } else if (base.get().path() instanceof Opaque && pointer.pointedAt() instanceof CodePoint cp && cp.codePoint() == '#') {
            scheme = base.get().scheme();
            path = base.get().path();
            query = base.get().query().map(StringBuilder::new).orElse(null);
            fragment = new StringBuilder();
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
          if (pointer.pointedAt() instanceof CodePoint cp && cp.codePoint() == '/' && pointer.doesRemainingStartWith("/")) {
            state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
            pointer.increase();
          } else {
            errors.add(new SpecialSchemeMissingFollowingSolidus());
            state = State.RELATIVE;
            pointer.decrease();
          }
        }
        case PATH_OR_AUTHORITY -> {
          if (pointer.pointedAt() instanceof CodePoint cp && cp.codePoint() == '/') {
            state = State.AUTHORITY;
          } else {
            state = State.PATH;
            pointer.decrease();
          }
        }
        case RELATIVE -> {
          scheme = base.get().scheme();
          if (pointer.pointedAt() instanceof CodePoint cp && cp.codePoint() == '/') {
            state = State.RELATIVE_SLASH;
          } else if (SPECIAL_SCHEMES.contains(scheme) && pointer.pointedAt() instanceof CodePoint cp && cp.codePoint() == '\\') {
            errors.add(new InvalidReverseSolidus());
            state = State.RELATIVE_SLASH;
          } else {
            username = new StringBuilder(base.get().username());
            password = new StringBuilder(base.get().password());
            host = base.get().host().orElse(null);
            port = base.get().port().orElse(null);
            path = base.get().path().copy();
            query = base.get().query().map(StringBuilder::new).orElse(null);

            PointedAt pointedAt = pointer.pointedAt();
            if (pointedAt instanceof CodePoint cp && cp.codePoint() == '?') {
              query = new StringBuilder();
              state = State.QUERY;
            } else if (pointedAt instanceof CodePoint cp && cp.codePoint() == '#') {
              fragment = new StringBuilder();
              state = State.FRAGMENT;
            } else if (pointedAt instanceof CodePoint cp) {
              var c = cp.codePoint();
              query = null;

              if (path instanceof NonOpaque no && !no.segments().isEmpty()) {
                path = new NonOpaque(no.segments().subList(0, no.segments().size() - 1));
              }

              state = State.PATH;
              pointer.decrease();
            }
          }
        }
        case RELATIVE_SLASH -> {
          if (SPECIAL_SCHEMES.contains(scheme) && (pointer.pointedAt() instanceof CodePoint cp && (cp.codePoint() == '/' || cp.codePoint() == '\\'))) {
            if (cp.codePoint() == '\\') {
              errors.add(new InvalidReverseSolidus());
            }
            state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
          } else if (pointer.pointedAt() instanceof CodePoint cp && cp.codePoint() == '/') {
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
          if (pointer.pointedAt() instanceof CodePoint cp && cp.codePoint() == '/' && pointer.doesRemainingStartWith("/")) {
            state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
            pointer.increase();
          } else {
            errors.add(new SpecialSchemeMissingFollowingSolidus());
            state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
            pointer.decrease();
          }
        }
        case SPECIAL_AUTHORITY_IGNORE_SLASHES -> {
          if (!(pointer.pointedAt() instanceof CodePoint cp && (cp.codePoint() == '/' || cp.codePoint() == '\\'))) {
            state = State.AUTHORITY;
            pointer.decrease();
          } else {
            errors.add(new SpecialSchemeMissingFollowingSolidus());
          }
        }
        case AUTHORITY -> {
          PointedAt pointedAt = pointer.pointedAt();

          if (pointedAt instanceof CodePoint cp && cp.codePoint() == '@') {
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

              String encodedCodePoints = PercentEncoder.utf8PercentEncode(codePoint, PercentEncoder.USERINFO);
              if (passwordTokenSeen) {
                password.append(encodedCodePoints);
              } else {
                username.append(encodedCodePoints);
              }
            }

            buffer.delete(0, buffer.length());
          } else if (
              (pointedAt instanceof Eof || (pointedAt instanceof CodePoint cp && (cp.codePoint() == '/' || cp.codePoint() == '?' || cp.codePoint() == '#'))) ||
                  (SPECIAL_SCHEMES.contains(scheme) && pointedAt instanceof CodePoint cp && cp.codePoint() == '\\')
          ) {
            if (atSignSeen && buffer.isEmpty()) {
              errors.add(new HostMissing());
              return new Failure(errors);
            }
            pointer.decrease(buffer.codePointCount(0, buffer.length()) + 1);
            buffer.delete(0, buffer.length());
            state = State.HOST;
          } else {
            if (pointedAt instanceof CodePoint cp) {
              buffer.appendCodePoint(cp.codePoint());
            } else {
              throw new IllegalStateException("Expected code point");
            }
          }
        }
        case HOST, HOSTNAME -> {
          PointedAt pointedAt = pointer.pointedAt();

          if (pointedAt instanceof CodePoint cp && cp.codePoint() == ':' && !insideBrackets) {
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
              (pointedAt instanceof Eof || (pointedAt instanceof CodePoint cp && (cp.codePoint() == '/' || cp.codePoint() == '?' || cp.codePoint() == '#'))) ||
                  (SPECIAL_SCHEMES.contains(scheme) && pointedAt instanceof CodePoint cp && cp.codePoint() == '\\')
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
            if (pointedAt instanceof CodePoint cp) {
              var c = cp.codePoint();
              if (c == '[') {
                insideBrackets = true;
              }
              if (c == ']') {
                insideBrackets = false;
              }
              buffer.appendCodePoint(c);
            }
          }
        }
        case PORT -> {
          PointedAt pointedAt = pointer.pointedAt();

          if (pointedAt instanceof CodePoint cp && isAsciiDigit(cp.codePoint())) {
            buffer.appendCodePoint(cp.codePoint());
          } else if (
              (pointedAt instanceof Eof || (pointedAt instanceof CodePoint cp && (cp.codePoint() == '/' || cp.codePoint() == '?' || cp.codePoint() == '#'))) ||
                  (SPECIAL_SCHEMES.contains(scheme) && pointedAt instanceof CodePoint cp && cp.codePoint() == '\\')
          ) {
            if (!buffer.isEmpty()) {
              int portInt;
              try {
                portInt = Integer.parseInt(buffer.toString());
              } catch (NumberFormatException e) {
                errors.add(new PortOutOfRange());
                return new Failure(errors);
              }

              if (portInt > Character.MAX_VALUE) {
                errors.add(new PortOutOfRange());
                return new Failure(errors);
              }

              char portChar = (char) portInt;

              if (
                  Optional.ofNullable(DEFAULT_PORTS.get(scheme))
                      .filter(isEqual(portChar))
                      .isPresent()
              ) {
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
        case FILE -> {
          scheme = "file";
          host = new Empty();

          if (pointer.pointedAt() instanceof CodePoint cp && (cp.codePoint() == '/' || cp.codePoint() == '\\')) {
            if (cp.codePoint() == '\\') {
              errors.add(new InvalidReverseSolidus());
            }
            state = State.FILE_SLASH;
          } else if (base.map(Url::scheme).filter(isEqual("file")).isPresent()) {
            Url b = base.get();

            host = b.host().orElse(null);
            path = b.path().copy();
            query = b.query().map(StringBuilder::new).orElse(null);

            PointedAt pointedAt = pointer.pointedAt();
            if (pointedAt instanceof CodePoint cp && cp.codePoint() == '?') {
              query = new StringBuilder();
              state = State.QUERY;
            } else if (pointedAt instanceof CodePoint cp && cp.codePoint() == '#') {
              fragment = new StringBuilder();
              state = State.FRAGMENT;
            } else if (pointedAt instanceof CodePoint cp) {
              var c = cp.codePoint();
              query = null;

              if (!pointer.doesRemainingStartWithWindowsDriveLetter()) {
                path = path.shorten(scheme);
              } else {
                errors.add(new FileInvalidWindowsDriveLetter());
                path = new NonOpaque(List.of());
              }

              state = State.PATH;
              pointer.decrease();
            }
          } else {
            state = State.PATH;
            pointer.decrease();
          }
        }
        case FILE_SLASH -> {
          if (pointer.pointedAt() instanceof CodePoint cp && (cp.codePoint() == '/' || cp.codePoint() == '\\')) {
            if (cp.codePoint() == '\\') {
              errors.add(new InvalidReverseSolidus());
            }
            state = State.FILE_HOST;
          } else {
            if (base.map(Url::scheme).filter(isEqual("file")).isPresent()) {
              Url b = base.get();
              host = b.host().orElse(null);

              if (
                  !pointer.doesRemainingStartWithWindowsDriveLetter()
              ) {
                String baseFirstPathSegment = ((NonOpaque) b.path()).segments().get(0);

                if (isNormalizedWindowsDriveLetter(baseFirstPathSegment)) {
                  path = path.append(baseFirstPathSegment);
                }
              }
            }
            state = State.PATH;
            pointer.decrease();
          }
        }
        case FILE_HOST -> {
          PointedAt pointedAt = pointer.pointedAt();

          if (
              pointedAt instanceof Eof ||
                  (pointedAt instanceof CodePoint cp &&
                      (cp.codePoint() == '/' || cp.codePoint() == '\\' || cp.codePoint() == '?' || cp.codePoint() == '#'))
          ) {
            pointer.decrease();

            if (isWindowsDriveLetter(buffer.toString())) {
              errors.add(new FileInvalidWindowsDriveLetterHost());
              state = State.PATH;
            } else if (buffer.isEmpty()) {
              host = new Empty();
              state = State.PATH_START;
            } else {
              Optional<Host> maybeHost = HostParser.parseHost(buffer.toString(), !SPECIAL_SCHEMES.contains(scheme), errors);
              if (maybeHost.isEmpty()) {
                return new Failure(errors);
              }

              Host host1 = maybeHost.get();

              if (host1 instanceof Domain d && d.domain().equals("localhost")) {
                host1 = new Empty();
              }

              host = host1;
              buffer.delete(0, buffer.length());
              state = State.PATH_START;
            }
          } else {
            buffer.appendCodePoint(((CodePoint) pointedAt).codePoint());
          }
        }
        case PATH_START -> {
          if (SPECIAL_SCHEMES.contains(scheme)) {
            if (pointer.pointedAt() instanceof CodePoint cp && cp.codePoint() == '\\') {
              errors.add(new InvalidReverseSolidus());
            }
            state = State.PATH;

            if (!(pointer.pointedAt() instanceof CodePoint cp && (cp.codePoint() == '/' || cp.codePoint() == '\\'))) {
              pointer.decrease();
            }
          } else {
            PointedAt pointedAt = pointer.pointedAt();
            if (pointedAt instanceof CodePoint cp && cp.codePoint() == '?') {
              query = new StringBuilder();
              state = State.QUERY;
            } else if (pointedAt instanceof CodePoint cp && cp.codePoint() == '#') {
              fragment = new StringBuilder();
              state = State.FRAGMENT;
            } else if (pointedAt instanceof CodePoint cp) {
              state = State.PATH;
              if (cp.codePoint() != '/') {
                pointer.decrease();
              }
            }
          }
        }
        case PATH -> {
          PointedAt pointedAt = pointer.pointedAt();
          if (
              (pointedAt instanceof Eof || (pointedAt instanceof CodePoint cp && cp.codePoint() == '/')) ||
                  (SPECIAL_SCHEMES.contains(scheme) && pointedAt instanceof CodePoint cp && cp.codePoint() == '\\') ||
                  (pointedAt instanceof CodePoint cp1 && (cp1.codePoint() == '?' || cp1.codePoint() == '#'))
          ) {
            if (SPECIAL_SCHEMES.contains(scheme) && pointedAt instanceof CodePoint cp && cp.codePoint() == '\\') {
              errors.add(new InvalidReverseSolidus());
            }

            String curBuffer = buffer.toString();
            if (curBuffer.equals("..") ||
                curBuffer.equalsIgnoreCase(".%2e") ||
                curBuffer.equalsIgnoreCase("%2e.") ||
                curBuffer.equalsIgnoreCase("%2e%2e")) {
              path = path.shorten(scheme);

              if (
                  !(pointedAt instanceof CodePoint cp && cp.codePoint() == '/') &&
                      !(SPECIAL_SCHEMES.contains(scheme) && pointedAt instanceof CodePoint cp1 && cp1.codePoint() == '\\')
              ) {
                path = path.append("");
              }
            } else if (
                (curBuffer.equals(".") || curBuffer.equalsIgnoreCase("%2e")) &&
                    !(pointedAt instanceof CodePoint cp && cp.codePoint() == '/') &&
                    !(SPECIAL_SCHEMES.contains(scheme) && pointedAt instanceof CodePoint cp1 && cp1.codePoint() == '\\')

            ) {
              path = path.append("");
            } else if (!(curBuffer.equals(".") || curBuffer.equalsIgnoreCase("%2e"))) {
              if (
                  scheme.equals("file") &&
                      path instanceof NonOpaque no &&
                      no.segments().isEmpty() &&
                      isWindowsDriveLetter(curBuffer)
              ) {
                buffer.setCharAt(1, ':');
                curBuffer = buffer.toString();
              }
              path = path.append(curBuffer);
            }

            buffer.delete(0, buffer.length());

            if (pointedAt instanceof CodePoint cp && cp.codePoint() == '?') {
              query = new StringBuilder();
              state = State.QUERY;
            } else if (pointedAt instanceof CodePoint cp && cp.codePoint() == '#') {
              fragment = new StringBuilder();
              state = State.FRAGMENT;
            }
          } else {
            int c;
            if (pointedAt instanceof CodePoint cp) {
              c = cp.codePoint();
            } else {
              throw new IllegalStateException("Must be code point here!");
            }
            if (!isUrlCodePoint(c) && c != '%') {
              errors.add(new InvalidUrlUnit(Character.toString(c)));
            }
            if (c == '%' && !pointer.doesRemainingStartWith("%d%d")) {
              errors.add(new InvalidUrlUnit("Unexpected %"));
            }

            buffer.append(PercentEncoder.utf8PercentEncode(c, PercentEncoder.PATH));
          }
        }
        case OPAQUE_PATH -> {
          PointedAt pointedAt = pointer.pointedAt();
          if (pointedAt instanceof CodePoint cp && cp.codePoint() == '?') {
            query = new StringBuilder();
            state = State.QUERY;
          } else if (pointedAt instanceof CodePoint cp && cp.codePoint() == '#') {
            fragment = new StringBuilder();
            state = State.FRAGMENT;
          } else {
            if (pointedAt instanceof CodePoint cp && !isUrlCodePoint(cp.codePoint()) && cp.codePoint() != '%') {
              errors.add(new InvalidUrlUnit(Character.toString(cp.codePoint())));
            }
            if (pointedAt instanceof CodePoint cp && cp.codePoint() == '%' && !pointer.doesRemainingStartWith("%d%d")) {
              errors.add(new InvalidUrlUnit("Unexpected %"));
            }
            if (pointedAt instanceof CodePoint cp) {
              path = path.append(PercentEncoder.utf8PercentEncode(cp.codePoint(), PercentEncoder.C0_CONTROL));
            }
          }
        }
        case QUERY -> {
          PointedAt pointedAt = pointer.pointedAt();

          if (pointedAt instanceof Eof || (pointedAt instanceof CodePoint cp && cp.codePoint() == '#')) {
            CharacterSet queryPercentEncodeSet =
                SPECIAL_SCHEMES.contains(scheme) ?
                    PercentEncoder.SPECIAL_QUERY :
                    PercentEncoder.QUERY;
            query.append(PercentEncoder.utf8PercentEncode(buffer.toString(), queryPercentEncodeSet));
            buffer.delete(0, buffer.length());

            if (pointedAt instanceof CodePoint) {
              fragment = new StringBuilder();
              state = State.FRAGMENT;
            }
          } else {
            int c;
            if (pointedAt instanceof CodePoint cp) {
              c = cp.codePoint();
            } else {
              throw new IllegalStateException("Must be code point here!");
            }

            if (!isUrlCodePoint(c) && c != '%') {
              errors.add(new InvalidUrlUnit(Character.toString(c)));
            }
            if (c == '%' && !pointer.doesRemainingStartWith("%d%d")) {
              errors.add(new InvalidUrlUnit("Unexpected %"));
            }
            buffer.appendCodePoint(c);
          }
        }
        case FRAGMENT -> {
          if (pointer.pointedAt() instanceof CodePoint cp) {
            var c = cp.codePoint();
            if (!isUrlCodePoint(c) && c != '%') {
              errors.add(new InvalidUrlUnit(Character.toString(c)));
            }
            if (c == '%' && !pointer.doesRemainingStartWith("%d%d")) {
              errors.add(new InvalidUrlUnit("Unexpected %"));
            }

            fragment.append(PercentEncoder.utf8PercentEncode(c, PercentEncoder.FRAGMENT));
          }
        }
      }

      if (pointer.pointedAt() instanceof Eof) {
        break;
      } else {
        pointer.increase();
      }
    }

    return new Success(new Url(scheme, username.toString(), password.toString(), host, port, path, query == null ? null : query.toString(), fragment == null ? null : fragment.toString()));
  }

  private String removeControlAndWhitespaceCharacters(String urlStr, List<ValidationError> errors) {
    if (urlStr.isEmpty()) {
      return urlStr;
    }

    int prefixEndIndex = 0;
    while (prefixEndIndex < urlStr.length()) {
      int codePoint = urlStr.codePointAt(prefixEndIndex);
      if (!isC0ControlOrSpace(codePoint)) {
        break;
      }

      prefixEndIndex += Character.charCount(codePoint);
    }

    int suffixStartIndex = urlStr.length();
    while (suffixStartIndex > 0) {
      int codePoint = urlStr.codePointBefore(suffixStartIndex);
      if (!isC0ControlOrSpace(codePoint)) {
        break;
      }

      suffixStartIndex -= Character.charCount(codePoint);
    }

    if (prefixEndIndex > 0 || suffixStartIndex < urlStr.length() - 1) {
      errors.add(new InvalidUrlUnit("leading or trailing C0 control or space"));
    }

    if (prefixEndIndex < suffixStartIndex) {
      urlStr = urlStr.substring(prefixEndIndex, suffixStartIndex);
    } else {
      urlStr = "";
    }

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
