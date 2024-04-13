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

import com.tylerkindy.url.IpAddress.Ipv4Address;
import com.tylerkindy.url.IpAddress.Ipv6Address;

public sealed interface Host {
  record Domain(String domain) implements Host {
    @Override
    public String toString() {
      return domain;
    }
  }
  record IpAddress(com.tylerkindy.url.IpAddress address) implements Host {
    @Override
    public String toString() {
      if (address instanceof Ipv4Address ipv4) {
        return ipv4.toString();
      }
      if (address instanceof Ipv6Address ipv6) {
        return "[" + ipv6 + "]";
      }
      throw new IllegalStateException("Unknown IpAddress class: " + address);
    }
  }
  record Opaque(String host) implements Host {
    @Override
    public String toString() {
      return host;
    }
  }
  record Empty() implements Host {
    @Override
    public String toString() {
      return "";
    }
  }
}
