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

package com.tylerkindy.url.testdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.tylerkindy.url.testdata.TestCase.Failure;
import com.tylerkindy.url.testdata.TestCase.Success;
import java.util.Optional;

@JsonTypeInfo(use = Id.DEDUCTION)
@JsonSubTypes({@Type(Success.class), @Type(Failure.class)})
public sealed interface TestCase {
  String input();
  Optional<String> base();

  default String name() {
    StringBuilder sb = new StringBuilder()
        .append('\'')
        .append(StringEscaper.escape(input()))
        .append("'");

    base().ifPresent(base -> {
      sb.append(" with base '")
          .append(base)
          .append('\'');
    });

    if (this instanceof Success) {
      sb.append(" successfully parses");
    } else if (this instanceof Failure) {
      sb.append(" fails to parse");
    } else {
      throw new IllegalStateException("Unknown TestCase class: " + this);
    }

    return sb.toString();
  }

  record Success(
      String input,
      Optional<String> base,
      String href,
      Optional<String> origin,
      String protocol,
      String username,
      String password,
      String host,
      String hostname,
      String port,
      String pathname,
      String search,
      String hash)
      implements TestCase {}

  record Failure(
      String input, Optional<String> base, boolean failure, Optional<FailsRelativeTo> relativeTo)
      implements TestCase {
    public Failure {
      if (!failure) {
        throw new IllegalArgumentException("failure must be true!");
      }
    }

    enum FailsRelativeTo {
      @JsonProperty("non-opaque-path-base")
      NON_OPAQUE_PATH_BASE,

      @JsonProperty("any-base")
      ANY_BASE
    }
  }
}
