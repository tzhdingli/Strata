/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.dsf;

import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.product.dsf.ResolvedDsf;

/**
 * Base pricer for Deliverable Swap Futures (DSFs).
 * <p>
 * This function provides common code used when pricing an {@link ResolvedDsf}.
 */
public abstract class AbstractDsfProductPricer {

  /**
   * Creates an instance.
   */
  protected AbstractDsfProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the number related to deliverable swap futures product on which the daily margin is computed.
   * <p>
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param future  the future
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  protected double marginIndex(ResolvedDsf future, double price) {
    return price * future.getNotional();
  }

  /**
   * Calculates the margin index sensitivity of the deliverable swap futures product.
   * <p>
   * The margin index sensitivity is the sensitivity of the margin index to the underlying curves.
   * For two consecutive closing prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param future  the future
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  protected PointSensitivities marginIndexSensitivity(
      ResolvedDsf future,
      PointSensitivities priceSensitivity) {

    return priceSensitivity.multipliedBy(future.getNotional());
  }

}
