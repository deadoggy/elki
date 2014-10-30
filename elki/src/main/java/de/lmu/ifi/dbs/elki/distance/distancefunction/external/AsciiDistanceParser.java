package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.datasource.parser.AbstractParser;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Parser for parsing one distance value per line.
 *
 * A line must have the following format: {@code id1 id2 distanceValue}, where
 * id1 and id2 are integers representing the two ids belonging to the distance
 * value. Lines starting with &quot;#&quot; will be ignored.
 * 
 * @author Elke Achtert
 * @author Erich Schubert
 */
@Title("Number Distance Parser")
@Description("Parser for the following line format:\n" //
    + "id1 id2 distanceValue, where id1 and is2 are integers representing the two ids belonging to the distance value.\n" //
    + "The ids and the distance value are separated by whitespace. Empty lines and lines beginning with \"#\" will be ignored.")
public class AsciiDistanceParser extends AbstractParser implements DistanceParser {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(AsciiDistanceParser.class);

  /**
   * Constructor.
   * 
   * @param colSep Column separator pattern
   * @param quoteChars Quote characters
   * @param comment Comment pattern
   */
  public AsciiDistanceParser(Pattern colSep, String quoteChars, Pattern comment) {
    super(colSep, quoteChars, comment);
  }

  @Override
  public void parse(InputStream in, DistanceCacheWriter cache) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    int lineNumber = 1;

    int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
    IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("Parsing distance matrix", LOG) : null;
    try {
      for(String line; (line = reader.readLine()) != null; lineNumber++) {
        LOG.incrementProcessed(prog);
        // Skip empty lines and comments
        if(line.length() <= 0 || isComment(line)) {
          continue;
        }
        tokenizer.initialize(line, 0, lengthWithoutLinefeed(line));

        if(!tokenizer.valid()) {
          throw new IllegalArgumentException("Less than three values in line " + lineNumber);
        }
        int id1, id2;
        try {
          id1 = (int) tokenizer.getLongBase10();
          tokenizer.advance();
        }
        catch(NumberFormatException e) {
          throw new IllegalArgumentException("Error in line " + lineNumber + ": id1 is not an integer!");
        }
        if(!tokenizer.valid()) {
          throw new IllegalArgumentException("Less than three values in line " + lineNumber);
        }

        try {
          id2 = (int) tokenizer.getLongBase10();
          tokenizer.advance();
        }
        catch(NumberFormatException e) {
          throw new IllegalArgumentException("Error in line " + lineNumber + ": id2 is not an integer!");
        }
        if(!tokenizer.valid()) {
          throw new IllegalArgumentException("Less than three values in line " + lineNumber);
        }

        // Track minimum and maximum
        if(id1 < id2) {
          min = (id1 < min) ? id1 : min;
          max = (id2 > min) ? id2 : max;
        }
        else {
          min = (id2 < min) ? id2 : min;
          max = (id1 > min) ? id1 : max;
        }

        try {
          double distance = tokenizer.getDouble();
          cache.put(id1, id2, distance);
        }
        catch(IllegalArgumentException e) {
          throw new IllegalArgumentException("Error in line " + lineNumber + ":" + e.getMessage(), e);
        }
        tokenizer.advance();
        if(tokenizer.valid()) {
          throw new IllegalArgumentException("More than three values in line " + lineNumber);
        }
      }
    }
    catch(IOException e) {
      throw new IllegalArgumentException("Error while parsing line " + lineNumber + ".");
    }

    LOG.setCompleted(prog);

    // check if all distance values are specified
    for(int i1 = min; i1 <= max; i1++) {
      for(int i2 = i1 + 1; i2 <= max; i2++) {
        if(!cache.containsKey(i1, i2)) {
          throw new IllegalArgumentException("Distance value for " + i1 + " to " + i2 + " is missing!");
        }
      }
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParser.Parameterizer {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected AsciiDistanceParser makeInstance() {
      return new AsciiDistanceParser(colSep, quoteChars, comment);
    }
  }
}
