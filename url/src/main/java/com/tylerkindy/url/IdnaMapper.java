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

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.io.Resources;
import com.tylerkindy.url.common.CodePoints.Range;
import com.tylerkindy.url.common.CodePoints.Single;
import com.tylerkindy.url.common.MappingRow;
import com.tylerkindy.url.common.UnicodeVersion;
import com.tylerkindy.url.common.UnicodeVersions;
import java.io.IOException;
import java.util.List;

final class IdnaMapper {

  private static final IdnaMapper CURRENT = new IdnaMapper(UnicodeVersions.getCurrentUnicodeVersion());

  private final List<MappingRow> mappingRows;

  public IdnaMapper(UnicodeVersion version) {
    this.mappingRows = readMappingTable(version);
  }

  private static List<MappingRow> readMappingTable(UnicodeVersion version) {
    CsvMapper csvMapper = new CsvMapper();
    CsvSchema schema = csvMapper.schemaFor(MappingRow.class).withHeader();

    try (
        var mapStream = Resources.getResource("com/tylerkindy/url/idnamap/" + version + ".csv")
            .openStream()
    ) {
      return csvMapper.readerFor(MappingRow.class).with(schema).<MappingRow>readValues(mapStream)
          .readAll();
    } catch (IOException e) {
      throw new RuntimeException("Error loading IDNA map for Unicode " + version, e);
    }
  }

  public static IdnaMapper current() {
    return CURRENT;
  }

  public MappingRow getMapping(int codePoint) {
    // TODO: binary search
    return mappingRows.stream()
        .filter(row ->
            switch (row.codePoints()) {
              case Single(var cp) -> cp == codePoint;
              case Range(var low, var high) ->
                  Integer.compareUnsigned(low, codePoint) <= 0 &&
                  Integer.compareUnsigned(codePoint, high) <= 0;
            }
        )
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "No mapping found for code point 0x" + Integer.toHexString(codePoint)
        ));
  }
}
