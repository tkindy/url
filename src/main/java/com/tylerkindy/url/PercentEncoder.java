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
  public static final PercentEncodeSet C0_CONTROL = PercentEncodeSet.builder()
      .addRange(Range.closed((int) '\u0000', (int) '\u001F'))
      .addRange(Range.greaterThan((int) '~'))
      .build();
  public static final PercentEncodeSet QUERY = PercentEncodeSet.builder()
      .addAll(C0_CONTROL)
      .addCodePoint(' ')
      .addCodePoint('"')
      .addCodePoint('#')
      .addCodePoint('<')
      .addCodePoint('>')
      .build();
  public static final PercentEncodeSet PATH = PercentEncodeSet.builder()
      .addAll(QUERY)
      .addCodePoint('?')
      .addCodePoint('`')
      .addCodePoint('{')
      .addCodePoint('}')
      .build();
  public static final PercentEncodeSet USERINFO = PercentEncodeSet.builder()
      .addAll(PATH)
      .addCodePoint('/')
      .addCodePoint(':')
      .addCodePoint(';')
      .addCodePoint('=')
      .addCodePoint('@')
      .addRange(Range.closed((int) '[', (int) '^'))
      .addCodePoint('|')
      .build();

  private PercentEncoder() {
    throw new RuntimeException();
  }

  public static String utf8PecentEncode(int codePoint, PercentEncodeSet percentEncodeSet) {
    return percentEncodeAfterEncoding(
        StandardCharsets.UTF_8,
        Character.toString(codePoint),
        percentEncodeSet
    );
  }

  public static String percentEncodeAfterEncoding(Charset encoding, String input, PercentEncodeSet percentEncodeSet) {
    return percentEncodeAfterEncoding(encoding, input, percentEncodeSet, false);
  }

  public static String percentEncodeAfterEncoding(Charset encoding, String input, PercentEncodeSet percentEncodeSet, boolean spaceAsPlus) {
    ByteBuffer encoded = encoding.encode(input);

    StringBuilder output = new StringBuilder();
    while (encoded.hasRemaining()) {
      byte b = encoded.get();
      if (spaceAsPlus && b == ' ') {
        output.append('+');
        continue;
      }

      if (percentEncodeSet.contains(b)) {
        output
            .append('%')
            .append(Integer.toHexString(b).toUpperCase());
      } else {
        output.appendCodePoint(b);
      }
    }

    return output.toString();
  }
}
