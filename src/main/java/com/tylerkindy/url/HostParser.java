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

import static com.tylerkindy.url.CharacterUtils.isAsciiDigit;
import static com.tylerkindy.url.CharacterUtils.isAsciiHexDigit;

import com.google.common.collect.ImmutableList;
import com.tylerkindy.url.Host.Domain;
import com.tylerkindy.url.IpAddress.Ipv6Address;
import com.tylerkindy.url.Pointer.PointedAt.CodePoint;
import com.tylerkindy.url.Pointer.PointedAt.Eof;
import com.tylerkindy.url.ValidationError.Ipv4InIpv6InvalidCodePoint;
import com.tylerkindy.url.ValidationError.Ipv4InIpv6OutOfRangePart;
import com.tylerkindy.url.ValidationError.Ipv4InIpv6TooFewParts;
import com.tylerkindy.url.ValidationError.Ipv4InIpv6TooManyPieces;
import com.tylerkindy.url.ValidationError.Ipv6InvalidCodePoint;
import com.tylerkindy.url.ValidationError.Ipv6InvalidCompression;
import com.tylerkindy.url.ValidationError.Ipv6MultipleCompression;
import com.tylerkindy.url.ValidationError.Ipv6TooFewPieces;
import com.tylerkindy.url.ValidationError.Ipv6TooManyPieces;
import com.tylerkindy.url.ValidationError.Ipv6Unclosed;
import java.util.ArrayList;
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
    String domain = PercentEncoder.percentDecode(input);
    // TODO: domain to ASCII
    return Optional.of(new Domain(domain));
  }

  private static Optional<Ipv6Address> parseIpv6(String input, List<ValidationError> errors) {
    List<Character> pieces = repeat((char) 0, 8);
    int pieceIndex = 0;
    Integer compress = null;
    Pointer pointer = new Pointer(input);

    if (pointer.pointedAt() instanceof CodePoint(var c) && c == ':') {
      if (!pointer.doesRemainingStartWith(":")) {
        errors.add(new Ipv6InvalidCompression());
        return Optional.empty();
      }

      pointer.increase(2);
      pieceIndex += 1;
      compress = pieceIndex;
    }

    while (pointer.pointedAt() instanceof CodePoint(var c)) {
      if (pieceIndex == 8) {
        errors.add(new Ipv6TooManyPieces());
        return Optional.empty();
      }
      if (c == ':') {
        if (compress != null) {
          errors.add(new Ipv6MultipleCompression());
          return Optional.empty();
        }
        pointer.increase();
        pieceIndex += 1;
        compress = pieceIndex;
        continue;
      }

      int value = 0;
      int length = 0;

      while (
          length < 4 &&
              pointer.pointedAt() instanceof CodePoint(var c1) &&
              isAsciiHexDigit(c1)
      ) {
        value = value * 0x10 + Character.digit(c1, 16);
        pointer.increase();
        length += 1;
      }

      if (pointer.pointedAt() instanceof CodePoint(var c1) && c1 == '.') {
        if (length == 0) {
          errors.add(new Ipv4InIpv6InvalidCodePoint());
          return Optional.empty();
        }
        pointer.decrease(length);

        if (pieceIndex > 6) {
          errors.add(new Ipv4InIpv6TooManyPieces());
          return Optional.empty();
        }

        int numbersSeen = 0;

        while (pointer.pointedAt() instanceof CodePoint(var c2)) {
          Integer ipv4Piece = null;
          if (numbersSeen > 0) {
            if (c2 == '.' && numbersSeen < 4) {
              pointer.increase();
            } else {
              errors.add(new Ipv4InIpv6InvalidCodePoint());
              return Optional.empty();
            }
          }

          if (!(pointer.pointedAt() instanceof CodePoint(var c3) && isAsciiDigit(c3))) {
            errors.add(new Ipv4InIpv6InvalidCodePoint());
            return Optional.empty();
          }

          while (pointer.pointedAt() instanceof CodePoint(var c4) && isAsciiDigit(c4)) {
            int number = Character.digit(c4, 10);
            if (ipv4Piece == null) {
              ipv4Piece = number;
            } else if (ipv4Piece == 0) {
              errors.add(new Ipv4InIpv6InvalidCodePoint());
              return Optional.empty();
            } else {
              ipv4Piece = ipv4Piece * 10 + number;
            }

            if (ipv4Piece > 255) {
              errors.add(new Ipv4InIpv6OutOfRangePart());
              return Optional.empty();
            }
            pointer.increase();
          }

          pieces.set(
              pieceIndex,
              (char) (pieces.get(pieceIndex) * 0x100 + ipv4Piece)
          );

          numbersSeen += 1;
          if (numbersSeen == 2 || numbersSeen == 4) {
            pieceIndex += 1;
          }
        }

        if (numbersSeen != 4) {
          errors.add(new Ipv4InIpv6TooFewParts());
          return Optional.empty();
        }
        break;
      } else if (pointer.pointedAt() instanceof CodePoint(var c1) && c1 == ':') {
        pointer.increase();

        if (pointer.pointedAt() instanceof Eof) {
          errors.add(new Ipv6InvalidCodePoint());
          return Optional.empty();
        }
      } else if (!(pointer.pointedAt() instanceof Eof)) {
        errors.add(new Ipv6InvalidCodePoint());
        return Optional.empty();
      }

      pieces.set(pieceIndex, (char) value);
      pieceIndex += 1;
    }

    if (compress != null) {
      int swaps = pieceIndex - compress;
      pieceIndex = 7;

      while (pieceIndex != 0 && swaps > 0) {
        char temp = pieces.get(pieceIndex);
        pieces.set(pieceIndex, pieces.get(compress + swaps - 1));
        pieces.set(compress + swaps - 1, temp);

        pieceIndex -= 1;
        swaps -= 1;
      }
    } else if (pieceIndex != 8) {
      errors.add(new Ipv6TooFewPieces());
      return Optional.empty();
    }

    return Optional.of(new Ipv6Address(ImmutableList.copyOf(pieces)));
  }

  private static Optional<String> parseOpaque(String input, List<ValidationError> errors) {
    return Optional.empty(); // TODO
  }
  
  private static <T> List<T> repeat(T t, int count) {
    List<T> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      result.add(t);
    }
    return result;
  }
}
