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

import com.tylerkindy.url.common.MappingRow;
import com.tylerkindy.url.common.MappingRow.WithMapping;
import com.tylerkindy.url.common.Status;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.List;

final class Idna {

  private Idna() {
    throw new RuntimeException();
  }

  public static IdnaProcessResult toAscii(String domain, ToAsciiParams params) {
    IdnaProcessResult result = process(
        domain,
        params.useStd3AsciiRules(),
        params.checkHyphens(),
        params.checkBidi(),
        params.checkJoiners(),
        params.transitionalProcessing(),
        params.ignoreInvalidPunycode()
    );
    // TODO
    return result;
  }

  private static IdnaProcessResult process(
      String domain,
      boolean useStd3AsciiRules,
      boolean checkHyphens,
      boolean checkBidi,
      boolean checkJoiners,
      boolean transitionalProcessing,
      boolean ignoreInvalidPunycode
  ) {
    String mapped = map(domain, useStd3AsciiRules, transitionalProcessing);
    String normalized = normalize(mapped);
    return convertAndValidate(
        normalized,
        checkHyphens,
        transitionalProcessing,
        ignoreInvalidPunycode
    );
  }

  private static String map(
      String domain,
      boolean useStd3AsciiRules,
      boolean transitionalProcessing
  ) {
    StringBuilder mappedBuilder = new StringBuilder(domain.length());

    int index = 0;
    while (index < domain.length()) {
      int codePoint = domain.codePointAt(index);
      MappingRow mappingRow = IdnaMapper.current().getMapping(codePoint);
      Status status = mappingRow.status();

      if (
          status == Status.DISALLOWED ||
              (useStd3AsciiRules &&
                  (status == Status.DISALLOWED_STD3_MAPPED ||
                      status == Status.DISALLOWED_STD3_VALID))
      ) {
        mappedBuilder.appendCodePoint(codePoint);
      } else if (
          status == Status.MAPPED ||
              (!useStd3AsciiRules && status == Status.DISALLOWED_STD3_MAPPED)
      ) {
        if (transitionalProcessing && codePoint == 0x1E9E) {
          mappedBuilder.append("ss");
        } else {
          List<Integer> mapping = ((WithMapping) mappingRow).mapping();
          mapping.forEach(mappedBuilder::appendCodePoint);
        }
      } else if (status == Status.DEVIATION) {
        if (transitionalProcessing) {
          List<Integer> mapping = ((WithMapping) mappingRow).mapping();
          mapping.forEach(mappedBuilder::appendCodePoint);
        } else {
          mappedBuilder.appendCodePoint(codePoint);
        }
      } else if (
          status == Status.VALID ||
              (!useStd3AsciiRules && status == Status.DISALLOWED_STD3_VALID)
      ) {
        mappedBuilder.appendCodePoint(codePoint);
      }

      index += Character.charCount(codePoint);
    }

    return mappedBuilder.toString();
  }

  private static String normalize(String mapped) {
    return Normalizer.normalize(mapped, Form.NFC);
  }

  private static IdnaProcessResult convertAndValidate(
      String normalized,
      boolean checkHyphens,
      boolean transitionalProcessing,
      boolean ignoreInvalidPunycode
  ) {
    String[] labels = normalized.split("\\.");

    boolean error = false;
    StringBuilder validatedBuilder = new StringBuilder(normalized.length());

    for (int i = 0; i < labels.length; i++) {
      String label = labels[i];

      if (i > 0) {
        validatedBuilder.append('.');
      }

      if (label.startsWith("xn--")) {
        if (
            label
                .codePoints()
                .anyMatch(codePoint -> codePoint > 0x7F)
        ) {
          error = true;
          validatedBuilder.append(label);
          continue;
        }

        try {
          label = Punycode.decode(label.substring(4));
        } catch (Exception e) {
          if (!ignoreInvalidPunycode) {
            error = true;
            validatedBuilder.append(label);
            continue;
          }
        }

        if (!isValid(label, false, checkHyphens)) {
          error = true;
        }

      } else {
        if (!isValid(label, transitionalProcessing, checkHyphens)) {
          error = true;
        }
      }

      validatedBuilder.append(label);
    }

    return new IdnaProcessResult(validatedBuilder.toString(), error);
  }

  private static boolean isValid(
      String label,
      boolean transitionalProcessing,
      boolean checkHyphens
  ) {
    if (!Normalizer.isNormalized(label, Form.NFC)) {
      return false;
    }
    if (checkHyphens) {
      if (label.codePointCount(0, label.length()) >= 4) {
        int start = label.offsetByCodePoints(0, 3);
        int end = label.offsetByCodePoints(start, 1) + 1;
        if (label.substring(start, end).equals("--")) {
          return false;
        }
      }

      if (label.startsWith("-") || label.endsWith("-")) {
        return false;
      }
    } else if (label.startsWith("xn--")) {
      return false;
    }

    if (label.indexOf('.') != -1) {
      return false;
    }
    if (!label.isEmpty()) {
      int type = Character.getType(label.codePointAt(0));
      if (type == Character.NON_SPACING_MARK ||
          type == Character.COMBINING_SPACING_MARK ||
          type == Character.ENCLOSING_MARK) {
        return false;
      }
    }
    for (int codePoint : new CodePointIterable(label)) {
      Status status = IdnaMapper.current().getMapping(codePoint).status();
      if (transitionalProcessing) {
        if (status != Status.VALID) {
          return false;
        }
      } else {
        if (!(status == Status.VALID || status == Status.DEVIATION)) {
          return false;
        }
      }
    }

    // TODO checkJoiners
    // TODO checkBidi

    return true;
  }

  public static final class ToAsciiParams {

    private final boolean checkHyphens;
    private final boolean checkBidi;
    private final boolean checkJoiners;
    private final boolean useStd3AsciiRules;
    private final boolean transitionalProcessing;
    private final boolean verifyDnsLength;
    private final boolean ignoreInvalidPunycode;

    private ToAsciiParams(Builder builder) {
      this.checkHyphens = builder.checkHyphens;
      this.checkBidi = builder.checkBidi;
      this.checkJoiners = builder.checkJoiners;
      this.useStd3AsciiRules = builder.useStd3AsciiRules;
      this.transitionalProcessing = builder.transitionalProcessing;
      this.verifyDnsLength = builder.verifyDnsLength;
      this.ignoreInvalidPunycode = builder.ignoreInvalidPunycode;
    }

    public static Builder builder() {
      return new Builder();
    }

    public boolean checkHyphens() {
      return checkHyphens;
    }

    public boolean checkBidi() {
      return checkBidi;
    }

    public boolean checkJoiners() {
      return checkJoiners;
    }

    public boolean useStd3AsciiRules() {
      return useStd3AsciiRules;
    }

    public boolean transitionalProcessing() {
      return transitionalProcessing;
    }

    public boolean verifyDnsLength() {
      return verifyDnsLength;
    }

    public boolean ignoreInvalidPunycode() {
      return ignoreInvalidPunycode;
    }

    public static final class Builder {

      private boolean checkHyphens;
      private boolean checkBidi;
      private boolean checkJoiners;
      private boolean useStd3AsciiRules;
      private boolean transitionalProcessing;
      private boolean verifyDnsLength;
      private boolean ignoreInvalidPunycode;

      public Builder setCheckHyphens(boolean checkHyphens) {
        this.checkHyphens = checkHyphens;
        return this;
      }

      public Builder setCheckBidi(boolean checkBidi) {
        this.checkBidi = checkBidi;
        return this;
      }

      public Builder setCheckJoiners(boolean checkJoiners) {
        this.checkJoiners = checkJoiners;
        return this;
      }

      public Builder setUseStd3AsciiRules(boolean useStd3AsciiRules) {
        this.useStd3AsciiRules = useStd3AsciiRules;
        return this;
      }

      public Builder setTransitionalProcessing(boolean transitionalProcessing) {
        this.transitionalProcessing = transitionalProcessing;
        return this;
      }

      public Builder setVerifyDnsLength(boolean verifyDnsLength) {
        this.verifyDnsLength = verifyDnsLength;
        return this;
      }

      public Builder setIgnoreInvalidPunycode(boolean ignoreInvalidPunycode) {
        this.ignoreInvalidPunycode = ignoreInvalidPunycode;
        return this;
      }

      public ToAsciiParams build() {
        return new ToAsciiParams(this);
      }
    }
  }

  record IdnaProcessResult(String domain, boolean error) {

  }
}
