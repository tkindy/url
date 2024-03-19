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

import java.util.List;

public sealed interface IpAddress {
  record Ipv4Address(int address) implements IpAddress {
    @Override
    public String toString() {
      StringBuilder output = new StringBuilder();
      int n = address;

      for (int i = 1; i <= 4; i++) {
        output.insert(0, n % 256);

        if (i != 4) {
          output.insert(0, '.');
        }
        n = Math.floorDiv(n, 256);
      }

      return output.toString();
    }
  }
  record Ipv6Address(List<Character> pieces) implements IpAddress {
    @Override
    public String toString() {
      return "::1"; // TODO
    }
  }
}
