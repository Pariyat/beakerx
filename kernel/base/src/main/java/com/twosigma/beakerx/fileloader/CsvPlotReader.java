/*
 *  Copyright 2017 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.twosigma.beakerx.fileloader;

import com.opencsv.CSVReader;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CsvPlotReader {

  public static String TIME_COLUMN = "time";

  public List<Map<String, Object>> read(String fileName) throws IOException {
    CSVReader reader = new CSVReader(new FileReader(fileName));

    List<Map<String, Object>> result = new ArrayList<>();
    String[] header = getHeader(reader);
    String[] row;
    while ((row = reader.readNext()) != null) {
      Map<String, Object> entry = new HashMap<>();
      int index = 0;
      for (String hc : header) {
        if (hc.equals(TIME_COLUMN)) {
          entry.put(hc, convertDate(row[index++]));
        } else {
          entry.put(hc, convertToNumber(row[index++]));
        }
      }
      result.add(entry);
    }
    return result;
  }

  private Object convertToNumber(Object value) {
    if (value instanceof String && NumberUtils.isCreatable((String) value)) {
      try {
        return Integer.parseInt((String) value);
      } catch (Exception ignored) {
      }
      try {
        return new BigInteger((String) value);
      } catch (Exception ignored) {
      }
      return Float.parseFloat((String) value);
    }
    return value;
  }

  private Object convertDate(Object x) {
    if (x instanceof Number) {
      return x;
    } else if (x instanceof Date) {
      Date date = (Date) x;
      return date;
    } else if (x instanceof String) {
      Date inputDate = null;
      try {
        inputDate = new SimpleDateFormat("yyyy-MM-dd").parse((String) x);
      } catch (ParseException e) {
        throw new IllegalArgumentException("time column accepts String date in a following format yyyy-MM-dd");
      }
      return inputDate;
    } else {
      throw new IllegalArgumentException("time column accepts numbers or java.util.Date objects or String date in a following format yyyy-MM-dd");
    }
  }

  private String[] getHeader(CSVReader reader) throws IOException {
    return reader.readNext();
  }

}
