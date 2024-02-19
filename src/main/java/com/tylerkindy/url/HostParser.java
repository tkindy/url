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

import com.tylerkindy.url.IpAddress.Ipv6Address;
import com.tylerkindy.url.ValidationError.Ipv6Unclosed;
import java.util.List;
import java.util.Optional;

final class HostParser {
  private HostParser() {
    throw new RuntimeException();
  }

  public static Optional<Host> parseHost(String input, List<ValidationError> errors) {
    return parseHost(input, false, errors);
  }

  public static Optional<Host> parseHost(String input, boolean isOpaque, List<ValidationError> errors) {
    if (!input.isEmpty() && input.charAt(0) == '[') {
      if (input.charAt(input.length() - 1) != ']') {
        errors.add(new Ipv6Unclosed());
        return Optional.empty();
      }
      return parseIpv6(input.substring(1, input.length() - 1), errors)
          .map(Host.IpAddress::new);
    }
    if (isOpaque) {
      return parseOpaque(input, errors)
          .map(Host.Opaque::new);
    }
    return Optional.empty(); // TODO
  }

  private static Optional<Ipv6Address> parseIpv6(String input, List<ValidationError> errors) {
    return Optional.empty(); // TODO
  }

  private static Optional<String> parseOpaque(String input, List<ValidationError> errors) {
    return Optional.empty(); // TODO
  }
}
