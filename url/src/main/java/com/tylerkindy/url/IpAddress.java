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

import java.util.List;
import java.util.Optional;

public sealed interface IpAddress {
  record Ipv4Address(int address) implements IpAddress {
    @Override
    public String toString() {
      StringBuilder output = new StringBuilder();
      int n = address;

      for (int i = 1; i <= 4; i++) {
        output.insert(0, Integer.remainderUnsigned(n, 256));

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
      StringBuilder output = new StringBuilder();
      Optional<Integer> compress = calcCompress();
      boolean ignore0 = false;

      for (int pieceIndex = 0; pieceIndex < 8; pieceIndex++) {
        if (ignore0 && pieces.get(pieceIndex) == 0) {
          continue;
        } else if (ignore0) {
          ignore0 = false;
        }
        if (compress.filter(isEqual(pieceIndex)).isPresent()) {
          String separator = pieceIndex == 0 ? "::" : ":";
          output.append(separator);
          ignore0 = true;
          continue;
        }

        output.append(Integer.toHexString(pieces.get(pieceIndex)));
        if (pieceIndex != 7) {
          output.append(':');
        }
      }

      return output.toString();
    }

    private Optional<Integer> calcCompress() {
      int current = 0;
      int longest = 0;
      int longestIndex = -1;

      for (int i = 0; i < pieces.size(); i++) {
        char piece = pieces.get(i);

        if (piece == 0) {
          current += 1;
        } else {
          if (current > longest) {
            longest = current;
            longestIndex = i - current;
          }
          current = 0;
        }
      }

      if (current > longest) {
        longest = current;
        longestIndex = pieces.size() - current;
      }

      if (longestIndex == -1 || longest == 1) {
        return Optional.empty();
      }
      return Optional.of(longestIndex);
    }
  }
}
