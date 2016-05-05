package com.opengamma.strata.pricer.impl.tree;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.product.fx.BarrierType;

abstract class KnockoutOptionFunction implements OptionFunction {

  public abstract double getBarrierLevel(int step);

  public abstract double getSign();

  public abstract double getStrike();

  public abstract double getTimeToExpiry();

  public abstract BarrierType getBarrierType();

  public abstract double getRebate(int step);

  @Override
  public DoubleArray getPayoffAtExpiryTrinomial(DoubleArray stateValue, int numberOfSteps) {
    int nNodes = stateValue.size();
    double[] values = new double[nNodes];
    double rebate = getRebate(numberOfSteps);
    double barrierLevel = getBarrierLevel(numberOfSteps);
    Arrays.fill(values, rebate);
    int index = getLowerBoundIndex(stateValue, barrierLevel);
    ArgChecker.isTrue(index > -1 && index < nNodes - 1, "barrier is covered by tree");
    int iMin = getBarrierType().equals(BarrierType.DOWN) ? index + 1 : 0;
    int iMmax = getBarrierType().equals(BarrierType.UP) ? index + 1 : nNodes;
    for (int i = iMin; i < iMmax; ++i) {
      values[i] = Math.max(getSign() * (stateValue.get(i) - getStrike()), 0d);
    }
    //    if (getBarrierType().equals(BarrierType.UP) && barrierLevel == stateValue.get(index)) {
    //      values[index] = rebate;
    //    }
    double bd = barrierLevel - stateValue.get(index);
    double ub = stateValue.get(index + 1) - barrierLevel;
    double ud = stateValue.get(index + 1) - stateValue.get(index);
    if (getBarrierType().equals(BarrierType.UP)) { // TODO combine two cases
      values[index] = barrierLevel == stateValue.get(index) ? rebate: 
          0.5 * values[index] + 0.5 * (ub / ud * rebate + bd / ud * values[index]);
    }
    if (getBarrierType().equals(BarrierType.DOWN)) { // TODO use isDown
      values[index + 1] = 0.5 * values[index + 1] + 0.5 *
          (bd / ud * rebate + ub / ud * values[index + 1]);
    }
    return DoubleArray.ofUnsafe(values);
  }

  @Override
  public DoubleArray getNextOptionValues(
      double discountFactor,
      DoubleMatrix transitionProbability,
      DoubleArray stateValue,
      DoubleArray values,
      int i) {

    int nNodes = 2 * i + 1;
    double[] res = new double[nNodes];
    double barrierLevel = getBarrierLevel(i);
    double rebate = getRebate(i);
    for (int j = 0; j < nNodes; ++j) {
      if ((getBarrierType().equals(BarrierType.DOWN) && stateValue.get(j) <= barrierLevel) ||
          (getBarrierType().equals(BarrierType.UP) && stateValue.get(j) >= barrierLevel)) {
        res[j] = rebate;
      } else {
        double upProb = transitionProbability.get(j, 2);
        double middleProb = transitionProbability.get(j, 1);
        double downProb = transitionProbability.get(j, 0);
        res[j] = discountFactor *
            (upProb * values.get(j + 2) + middleProb * values.get(j + 1) + downProb * values.get(j));
      }
    }
    int index = getLowerBoundIndex(stateValue, barrierLevel);
    if (index > -1 && index < nNodes - 1) {
      double bd = barrierLevel - stateValue.get(index);
      double ub = stateValue.get(index + 1) - barrierLevel;
      double ud = stateValue.get(index + 1) - stateValue.get(index);
      if (getBarrierType().equals(BarrierType.UP)) { // TODO combine two cases
        res[index] = 0.5 * res[index] + 0.5 * (ub / ud * rebate + bd / ud * res[index]);
      }
      if (getBarrierType().equals(BarrierType.DOWN)) { // TODO use isDown
        res[index + 1] = 0.5 * res[index + 1] + 0.5 *
            (bd / ud * rebate + ub / ud * res[index + 1]);
      }
    }
    return DoubleArray.ofUnsafe(res);
  }

  private int getLowerBoundIndex(DoubleArray set, double value) {
    int n = set.size();
    if (value < set.get(0)) {
      return -1;
    }
    if (value > set.get(n - 1)) {
      return n - 1;
    }
    int index = Arrays.binarySearch(set.toArrayUnsafe(), value);
    if (index >= 0) {
      // Fast break out if it's an exact match.
      return index;
    }
    if (index < 0) {
      index = -(index + 1);
      index--;
    }
    if (value == -0. && index < n - 1 && set.get(index + 1) == 0.) {
      ++index;
    }
    return index;
  }
}
