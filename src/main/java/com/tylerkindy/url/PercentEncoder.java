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

import com.google.common.collect.Range;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

final class PercentEncoder {
  public static final CharacterSet C0_CONTROL = CharacterSet.builder()
      .addRange(Range.closed((int) '\u0000', (int) '\u001F'))
      .addRange(Range.greaterThan((int) '~'))
      .build();
  public static final CharacterSet QUERY = CharacterSet.builder()
      .addAll(C0_CONTROL)
      .addCodePoint(' ')
      .addCodePoint('"')
      .addCodePoint('#')
      .addCodePoint('<')
      .addCodePoint('>')
      .build();
  public static final CharacterSet SPECIAL_QUERY = CharacterSet.builder()
      .addAll(QUERY)
      .addCodePoint('\'')
      .build();
  public static final CharacterSet PATH = CharacterSet.builder()
      .addAll(QUERY)
      .addCodePoint('?')
      .addCodePoint('`')
      .addCodePoint('{')
      .addCodePoint('}')
      .build();
  public static final CharacterSet USERINFO = CharacterSet.builder()
      .addAll(PATH)
      .addCodePoint('/')
      .addCodePoint(':')
      .addCodePoint(';')
      .addCodePoint('=')
      .addCodePoint('@')
      .addRange(Range.closed((int) '[', (int) '^'))
      .addCodePoint('|')
      .build();
  public static final CharacterSet FRAGMENT = CharacterSet.builder()
      .addAll(C0_CONTROL)
      .addCodePoints(' ', '"', '<', '>', '`')
      .build();

  private PercentEncoder() {
    throw new RuntimeException();
  }

  public static String utf8PercentEncode(String input, CharacterSet percentEncodeSet) {
    return percentEncodeAfterEncoding(StandardCharsets.UTF_8, input, percentEncodeSet);
  }

  public static String utf8PercentEncode(int codePoint, CharacterSet percentEncodeSet) {
    return percentEncodeAfterEncoding(
        StandardCharsets.UTF_8,
        Character.toString(codePoint),
        percentEncodeSet
    );
  }

  public static String percentEncodeAfterEncoding(Charset encoding, String input, CharacterSet percentEncodeSet) {
    return percentEncodeAfterEncoding(encoding, input, percentEncodeSet, false);
  }

  public static String percentEncodeAfterEncoding(Charset encoding, String input, CharacterSet percentEncodeSet, boolean spaceAsPlus) {
    ByteBuffer encoded = encoding.encode(input);

    StringBuilder output = new StringBuilder();
    while (encoded.hasRemaining()) {
      byte b = encoded.get();
      if (spaceAsPlus && b == 0x20) {
        output.append('+');
        continue;
      }

      int isomorph = Byte.toUnsignedInt(b);

      if (percentEncodeSet.contains(isomorph)) {
        output
            .append('%')
            .append(Integer.toHexString(isomorph).toUpperCase());
      } else {
        output.appendCodePoint(isomorph);
      }
    }

    return output.toString();
  }

  public static String percentDecode(String input) {
    Charset charset = StandardCharsets.UTF_8;
    return charset.decode(percentDecode(charset.encode(input))).toString();
  }

  private static ByteBuffer percentDecode(ByteBuffer bytes) {
    ByteBuffer output = ByteBuffer.allocate(bytes.capacity());
    int outputBytes = 0;
    while (bytes.hasRemaining()) {
      byte b = bytes.get();
      if (b != 0x25) {
        output.put(b);
        outputBytes++;
      } else {
        bytes.mark();
        byte nextByte1 = bytes.get();
        byte nextByte2 = bytes.get();
        if (!(isUtf8HexDigit(nextByte1) && isUtf8HexDigit(nextByte2))) {
          output.put(b);
          outputBytes++;
          bytes.reset();
        } else {
          byte bytePoint = Byte.parseByte(
              StandardCharsets.UTF_8.decode(
                  ByteBuffer.wrap(new byte[]{nextByte1, nextByte2})
                  )
                  .toString(),
              16
          );
          output.put(bytePoint);
          outputBytes++;
        }
      }
    }
    return output.rewind().limit(outputBytes);
  }

  private static boolean isUtf8HexDigit(byte b) {
    return (b >= 0x30 && b <= 0x39) ||
        (b >= 0x41 && b <= 0x46) ||
        (b >= 0x61 && b <= 0x66);
  }
}
