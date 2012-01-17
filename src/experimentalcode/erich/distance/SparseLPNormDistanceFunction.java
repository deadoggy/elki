package experimentalcode.erich.distance;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2012
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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides a LP-Norm for FeatureVectors.
 * 
 * @author Erich Schubert
 * 
 * @param <V> the type of FeatureVector to compute the distances in between
 * @param <N> number type
 */
// TODO: implement SpatialDistanceFunction
public class SparseLPNormDistanceFunction<V extends SparseNumberVector<V, N>, N extends Number> extends AbstractPrimitiveDistanceFunction<V, DoubleDistance> {
  /**
   * OptionID for {@link #P_PARAM}
   */
  public static final OptionID P_ID = OptionID.getOrCreateOptionID("lpnorm.p", "the degree of the L-P-Norm (positive number)");

  /**
   * P parameter
   */
  private final DoubleParameter P_PARAM = new DoubleParameter(P_ID, new GreaterConstraint(0));

  /**
   * Keeps the currently set p.
   */
  private double p;

  /**
   * Provides a LP-Norm for FeatureVectors.
   */
  public SparseLPNormDistanceFunction(Parameterization config) {
    super();
    config = config.descend(this);
    if(config.grab(P_PARAM)) {
      p = P_PARAM.getValue();
    }
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  /**
   * Returns the distance between the specified FeatureVectors as a LP-Norm for
   * the currently set p.
   * 
   * @param v1 first FeatureVector
   * @param v2 second FeatureVector
   * @return the distance between the specified FeatureVectors as a LP-Norm for
   *         the currently set p
   */
  @Override
  public DoubleDistance distance(V v1, V v2) {
    if(v1.getDimensionality() != v2.getDimensionality()) {
      throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
    }

    double sqrDist = 0;
    // Get the bit masks
    BitSet b1 = v1.getNotNullMask();
    BitSet b2 = v2.getNotNullMask();
    // Recombine
    BitSet both = (BitSet) b1.clone();
    both.and(b2);
    b1.andNot(both);
    b2.andNot(both);
    // Set in first only
    for(int i = b1.nextSetBit(0); i >= 0; i = b1.nextSetBit(i + 1)) {
      double manhattanI = Math.abs(v1.doubleValue(i));
      sqrDist += Math.pow(manhattanI, p);
    }
    // Set in second only
    for(int i = b2.nextSetBit(0); i >= 0; i = b2.nextSetBit(i + 1)) {
      double manhattanI = Math.abs(v2.doubleValue(i));
      sqrDist += Math.pow(manhattanI, p);
    }
    // Set in both
    for(int i = both.nextSetBit(0); i >= 0; i = both.nextSetBit(i + 1)) {
      double manhattanI = Math.abs(v1.doubleValue(i) - v2.doubleValue(i));
      sqrDist += Math.pow(manhattanI, p);
    }
    return new DoubleDistance(Math.pow(sqrDist, 1.0 / p));
  }

  // TODO: relax this to VectorTypeInformation!
  @Override
  public VectorFieldTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }
}