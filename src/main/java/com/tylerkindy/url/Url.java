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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Url {
  private final String scheme;

  public static Url parseOrThrow(String url) {
    return extractOrThrow(url, UrlParser.parse(url, Optional.empty()));
  }

  public static Url parseOrThrow(String url, Url base) {
    return extractOrThrow(url, UrlParser.parse(url, Optional.of(base)));
  }

  private static Url extractOrThrow(String urlStr, UrlParseResult result) {
    return switch (result) {
      case Success(Url url) -> url;
      case SuccessWithErrors(Url url, var errors) -> url;
      case Failure(List<ValidationError> errors) -> throw new ValidationException(urlStr, errors);
    };
  }

  Url(String scheme) {
    this.scheme = scheme;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o instanceof Url u && Objects.equals(scheme, u.scheme);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(scheme);
  }

  @Override
  public String toString() {
    return scheme + ":";
  }
}
