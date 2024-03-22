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

package com.tylerkindy.url.tools;

import com.tylerkindy.url.common.UnicodeVersion;
import com.tylerkindy.url.common.UnicodeVersions;
import com.tylerkindy.url.tools.InputRow.WithMapping;
import com.tylerkindy.url.tools.InputRow.WithoutMapping;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DownloadIdnaMappingTables {

  private final HttpClient httpClient;

  private DownloadIdnaMappingTables(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public static void main(String[] args) {
    new DownloadIdnaMappingTables(
        HttpClient.newHttpClient()
    )
        .run();
  }

  private void run() {
    for (UnicodeVersion version : UnicodeVersions.getAllSupportedUnicodeVersions()) {
      downloadIfNeeded(version);
    }
  }

  private void downloadIfNeeded(UnicodeVersion version) {
    Path resourceFilePath = buildResourcePath(version);
    if (Files.exists(resourceFilePath)) {
      // TODO: add real logging
      // TODO: check if file has changed, i.e. Date is different
      System.out.println("Mapping table for " + version + " already exists, skipping");
      return;
    }

    System.out.println("Downloading mapping table for " + version);

    HttpResponse<InputStream> response;
    try {
      response = httpClient.send(
          HttpRequest.newBuilder()
              .GET()
              .uri(URI.create(
                  "https://www.unicode.org/Public/idna/" + version + "/IdnaMappingTable.txt")
              )
              .build(),
          BodyHandlers.ofInputStream()
      );
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Error fetching mapping table for " + version, e);
    }

    List<InputRow> inputRows;
    try {
      inputRows = readInput(response.body());
    } catch (Exception e) {
      throw new RuntimeException("Error parsing mapping table for " + version, e);
    }

    System.out.println("Writing shrunk table to " + resourceFilePath);

    try {
      Files.createDirectories(resourceFilePath.getParent());
      Files.createFile(resourceFilePath);
    } catch (IOException e) {
      throw new RuntimeException("Error creating " + resourceFilePath, e);
    }

    try (var writer = new BufferedWriter(new FileWriter(resourceFilePath.toFile()))) {
      writer.write("codePoints,status,mapping\n");

      for (InputRow inputRow : inputRows) {
        writer.write(inputRow.codePoints().toString());
        writer.write(',');
        writer.write(inputRow.status().toString());
        writer.write(',');

        if (inputRow instanceof WithMapping mapped) {

          if (mapped.mapping().size() > 1) {
            writer.write('"');
          }

          String mapping = mapped.mapping().stream()
              .map(Integer::toHexString)
              .collect(Collectors.joining(" "));

          writer.write(mapping);

          if (mapped.mapping().size() > 1) {
            writer.write('"');
          }
        }

        writer.write('\n');
      }
    } catch (IOException e) {
      throw new RuntimeException("Error writing table to " + resourceFilePath, e);
    }

    System.out.println("Done with " + version);
  }

  private Path buildResourcePath(UnicodeVersion version) {
    return Path.of("url/src/main/resources/com/tylerkindy/url/idnamap", version + ".csv");
  }

  private List<InputRow> readInput(InputStream inputStream) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

    return reader
        .lines()
        .<InputRow>mapMulti((line, downstream) -> {
          int startOfComment = line.indexOf('#');
          if (startOfComment != -1) {
            line = line.substring(0, startOfComment);
          }

          if (line.isBlank()) {
            // Empty line, or entire line was a comment
            return;
          }

          String[] columns = line.split(";");

          CodePoints codePoints = CodePoints.fromInput(columns[0].trim());
          Status status = Status.fromInput(columns[1].trim());

          if (columns.length > 2 && !columns[2].isBlank()) {
            List<Integer> mapping = Arrays.stream(columns[2].trim().split(" "))
                .map(codePoint -> Integer.parseInt(codePoint, 16))
                .toList();
            downstream.accept(new WithMapping(codePoints, status, mapping));
          } else {
            downstream.accept(new WithoutMapping(codePoints, status));
          }
        })
        .toList();
  }
}
