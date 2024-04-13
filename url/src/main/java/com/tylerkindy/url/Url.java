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

import com.tylerkindy.url.UrlParseResult.Failure;
import com.tylerkindy.url.UrlParseResult.Success;
import com.tylerkindy.url.UrlParseResult.SuccessWithErrors;
import com.tylerkindy.url.UrlPath.NonOpaque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Url {
  private final String scheme;
  private final String username;
  private final String password;
  private final Host host;
  private final Character port;
  private final UrlPath path;
  private final String query;
  private final String fragment;

  public static UrlParseResult parse(String url) {
    return UrlParser.INSTANCE.parse(url, Optional.empty());
  }

  public static Url parseOrThrow(String url) {
    return extractOrThrow(url, parse(url));
  }

  public static UrlParseResult parse(String url, Url base) {
    return UrlParser.INSTANCE.parse(url, Optional.of(base));
  }

  public static Url parseOrThrow(String url, Url base) {
    return extractOrThrow(url, parse(url, base));
  }

  private static Url extractOrThrow(String urlStr, UrlParseResult result) {
    return switch (result) {
      case Success(Url url) -> url;
      case SuccessWithErrors(Url url, var errors) -> url;
      case Failure(List<ValidationError> errors) -> throw new ValidationException(urlStr, errors);
    };
  }

  Url(String scheme, String username, String password, Host host, Character port, UrlPath path, String query, String fragment) {
    this.scheme = scheme;
    this.username = username;
    this.password = password;
    this.host = host;
    this.port = port;
    this.path = path;
    this.query = query;
    this.fragment = fragment;
  }

  public String scheme() {
    return scheme;
  }

  public String username() {
    return username;
  }

  public String password() {
    return password;
  }

  public Optional<Host> host() {
    return Optional.ofNullable(host);
  }

  public Optional<Character> port() {
    return Optional.ofNullable(port);
  }

  public UrlPath path() {
    return path;
  }

  public Optional<String> query() {
    return Optional.ofNullable(query);
  }

  public Optional<String> fragment() {
    return Optional.ofNullable(fragment);
  }

  @Override
  public boolean equals(Object o) {
    return this == o ||
        o instanceof Url url &&
            Objects.equals(scheme, url.scheme) &&
            Objects.equals(username, url.username) &&
            Objects.equals(password, url.password) &&
            Objects.equals(host, url.host) &&
            Objects.equals(port, url.port) &&
            Objects.equals(path, url.path) &&
            Objects.equals(query, url.query) &&
            Objects.equals(fragment, url.fragment);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scheme, username, password, host, port, path, query, fragment);
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder(scheme).append(":");

    host().ifPresent(host -> {
      output.append("//");
      if (!username.isEmpty() || !password.isEmpty()) {
        output.append(username);
        if (!password.isEmpty()) {
          output.append(":").append(password);
        }
        output.append("@");
      }
      output.append(host);
      port().ifPresent(port -> output.append(":").append(Integer.toString(port)));
    });

    if (host().isEmpty() && path instanceof NonOpaque p && p.segments().size() > 1 && p.segments().getFirst().isEmpty()) {
      output.append("/.");
    }

    output.append(path);

    query().ifPresent(query -> output.append('?').append(query));

    // TODO: allow excluding fragment as per spec?
    fragment().ifPresent(fragment -> output.append('#').append(fragment));

    return output.toString();
  }
}
