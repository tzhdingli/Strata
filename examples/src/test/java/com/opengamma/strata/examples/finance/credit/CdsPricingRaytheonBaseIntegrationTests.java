/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance.credit;

import com.opengamma.strata.basics.BuySell;
import org.testng.annotations.Test;

import java.time.LocalDate;

import static com.opengamma.strata.examples.finance.credit.TestHarness.TradeFactory.withTrade;

@Test
public class CdsPricingRaytheonBaseIntegrationTests {

  final LocalDate valuationDate = LocalDate.of(2014, 10, 16);
  final LocalDate cashSettleDate = LocalDate.of(2014, 10, 21);
  final double feeAmount = 3_694_117.73D;
  final BuySell buySell = BuySell.BUY;

  private TestHarness.TradeFactory onTrade() {
    return withTrade(buySell, feeAmount, cashSettleDate);
  }

  @Test
  public void test_pvs_on_raytheon_base() {
    onTrade().pvShouldBe(0.004943638574331999).on(valuationDate);
  }

  @Test
  public void test_ir01_parallel_par_on_raytheon_base_case() {
    onTrade().ir01ParallelParShouldBe(963.5778398220427).on(valuationDate);
  }

  @Test
  public void test_ir01_bucketed_par_on_raytheon_base_case() {
    onTrade()
        .ir01BucketedParShouldBe(
            -1.0080670211464167,
            3.1341894390061498,
            1.5775036015547812,
            12.721042910125107,
            52.718546617776155,
            138.10654797079042,
            206.87827731855214,
            275.05216884659603,
            257.4313263935037,
            17.14714361494407,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0)
        .on(valuationDate);
  }

  @Test
  public void test_cs01_parallel_par_on_raytheon_base_case() {
    onTrade().cs01ParallelParShouldBe(51873.837977122515).on(valuationDate);
  }

  @Test
  public void test_cs01_bucketed_par_on_raytheon_base_case() {
    onTrade()
        .cs01BucketedParShouldBe(
            46.60949367238209,
            103.86381249455735,
            252.1060386202298,
            364.7911099912599,
            484.21517529617995,
            50640.11123423558,
            0.0,
            0.0)
        .on(valuationDate);
  }


}
