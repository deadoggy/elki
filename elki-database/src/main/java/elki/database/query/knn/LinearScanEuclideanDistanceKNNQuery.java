/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.database.query.knn;

import java.util.ArrayList;
import java.util.List;

import elki.data.NumberVector;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DBIDUtil;
import elki.database.ids.KNNHeap;
import elki.database.ids.KNNList;
import elki.database.query.LinearScanQuery;
import elki.database.query.distance.PrimitiveDistanceQuery;
import elki.database.relation.Relation;
import elki.distance.distancefunction.minkowski.EuclideanDistance;
import elki.distance.distancefunction.minkowski.SquaredEuclideanDistance;

/**
 * Instance of this query for a particular database.
 *
 * This is a subtle optimization: for primitive queries, it is clearly faster to
 * retrieve the query object from the relation only once!
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @assoc - - - PrimitiveDistanceQuery
 * @assoc - - - EuclideanDistance
 * @assoc - - - SquaredEuclideanDistance
 */
public class LinearScanEuclideanDistanceKNNQuery<O extends NumberVector> extends LinearScanPrimitiveDistanceKNNQuery<O> implements LinearScanQuery {
  /**
   * Constructor.
   *
   * @param distanceQuery Distance function to use
   */
  public LinearScanEuclideanDistanceKNNQuery(PrimitiveDistanceQuery<O> distanceQuery) {
    super(distanceQuery);
    assert (EuclideanDistance.STATIC.equals(distanceQuery.getDistance()));
  }

  @Override
  public KNNList getKNNForDBID(DBIDRef id, int k) {
    final Relation<? extends O> relation = getRelation();
    return linearScan(relation , relation.iterDBIDs(), relation.get(id), DBIDUtil.newHeap(k)).toKNNListSqrt();
  }

  @Override
  public KNNList getKNNForObject(O obj, int k) {
    final Relation<? extends O> relation = getRelation();
    return linearScan(relation, relation.iterDBIDs(), obj, DBIDUtil.newHeap(k)).toKNNListSqrt();
  }

  /**
   * Main loop of the linear scan.
   *
   * @param relation Data relation
   * @param iter ID iterator
   * @param obj Query object
   * @param heap Output heap
   * @return Heap
   */
  private KNNHeap linearScan(Relation<? extends O> relation, DBIDIter iter, final O obj, KNNHeap heap) {
    final SquaredEuclideanDistance squared = SquaredEuclideanDistance.STATIC;
    double max = Double.POSITIVE_INFINITY;
    while(iter.valid()) {
      final double dist = squared.distance(obj, relation.get(iter));
      if(dist <= max) {
        max = heap.insert(dist, iter);
      }
      iter.advance();
    }
    return heap;
  }

  @Override
  public List<KNNList> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    final Relation<? extends O> relation = getRelation();
    final int size = ids.size();
    final List<KNNHeap> heaps = new ArrayList<>(size);
    List<O> objs = new ArrayList<>(size);
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      heaps.add(DBIDUtil.newHeap(k));
      objs.add(relation.get(iter));
    }
    linearScanBatchKNN(objs, heaps);

    List<KNNList> result = new ArrayList<>(heaps.size());
    for(KNNHeap heap : heaps) {
      result.add(heap.toKNNListSqrt());
    }
    return result;
  }

  /**
   * Perform a linear scan batch kNN for primitive distance functions.
   *
   * @param objs Objects list
   * @param heaps Heaps array
   */
  @Override
  protected void linearScanBatchKNN(List<O> objs, List<KNNHeap> heaps) {
    final SquaredEuclideanDistance squared = SquaredEuclideanDistance.STATIC;
    final Relation<? extends O> relation = getRelation();
    final int size = objs.size();
    // Linear scan style KNN.
    for(DBIDIter iter = relation.getDBIDs().iter(); iter.valid(); iter.advance()) {
      O candidate = relation.get(iter);
      for(int index = 0; index < size; index++) {
        final KNNHeap heap = heaps.get(index);
        final double dist = squared.distance(objs.get(index), candidate);
        if(dist <= heap.getKNNDistance()) {
          heap.insert(dist, iter);
        }
      }
    }
  }
}