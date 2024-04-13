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

package com.tylerkindy.url.common;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class UnicodeVersions {

  private static final Map<Integer, UnicodeVersion> VERSIONS = Map.of(
      17, new UnicodeVersion(13, 0, 0),
      18, new UnicodeVersion(13, 0, 0),
      19, new UnicodeVersion(14, 0, 0),
      20, new UnicodeVersion(15, 0, 0),
      21, new UnicodeVersion(15, 0, 0),
      22, new UnicodeVersion(15, 1, 0)
  );

  private UnicodeVersions() {
    throw new RuntimeException();
  }

  public static UnicodeVersion getCurrentUnicodeVersion() {
    int feature = Runtime.version().feature();

    return Optional.ofNullable(VERSIONS.get(feature))
        .orElseThrow(
            () -> new IllegalStateException(
                "No Unicode version configured for Java " + feature
            )
        );
  }

  public static Set<UnicodeVersion> getAllSupportedUnicodeVersions() {
    return Set.copyOf(VERSIONS.values());
  }
}
