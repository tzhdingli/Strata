package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.LongShort.SHORT;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.impl.option.BlackBarrierPriceFormulaRepository;
import com.opengamma.strata.pricer.impl.tree.ConstantKnockoutOptionFunction;
import com.opengamma.strata.pricer.impl.tree.CoxRossRubinsteinLatticeSpecification;
import com.opengamma.strata.pricer.impl.tree.RecombiningTrinomialTreeData;
import com.opengamma.strata.pricer.impl.tree.TrinomialTree;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.fx.BarrierType;
import com.opengamma.strata.product.fx.KnockType;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fx.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fx.ResolvedFxVanillaOption;
import com.opengamma.strata.product.fx.SimpleConstantContinuousBarrier;

@Test
public class ImpliedTrinomialTreeFxSingleBarrierOptionProductPricerTest {

  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final LocalDate VAL_DATE = LocalDate.of(2011, 6, 13);
  private static final ZonedDateTime VAL_DATETIME = VAL_DATE.atStartOfDay(ZONE);
  private static final LocalDate PAY_DATE = LocalDate.of(2014, 9, 15);
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 9, 15);
  private static final ZonedDateTime EXPIRY_DATETIME = EXPIRY_DATE.atStartOfDay(ZONE);
  // providers
  private static final ImmutableRatesProvider RATE_PROVIDER_FLAT =
      RatesProviderFxDataSets.createProviderEurUsdFlat(VAL_DATE);
  private static final BlackVolatilitySmileFxProvider VOL_PROVIDER_FLAT =
      FxVolatilitySmileDataSet.createVolatilitySmileProvider5FlatFlat(VAL_DATETIME);

  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final double SPOT = RATE_PROVIDER_FLAT.fxRate(CURRENCY_PAIR);
  private static final double NOTIONAL = 100_000_000d;
  private static final double LEVEL_LOW = 1.25;
  private static final double LEVEL_HIGH = 1.6;
  private static final SimpleConstantContinuousBarrier BARRIER_DKI =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, LEVEL_LOW);
  private static final SimpleConstantContinuousBarrier BARRIER_DKO =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, LEVEL_LOW);
  private static final SimpleConstantContinuousBarrier BARRIER_UKI =
      SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, LEVEL_HIGH);
  private static final SimpleConstantContinuousBarrier BARRIER_UKO =
      SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, LEVEL_HIGH);
  private static final double REBATE_AMOUNT = 5_000_000d; // large rebate for testing
  private static final CurrencyAmount REBATE = CurrencyAmount.of(USD, REBATE_AMOUNT);
  private static final CurrencyAmount REBATE_BASE = CurrencyAmount.of(EUR, REBATE_AMOUNT);
  private static final double STRIKE_RATE_HIGH = 1.45;
  private static final double STRIKE_RATE_LOW = 1.35;
  // call
  private static final CurrencyAmount EUR_AMOUNT_REC = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_PAY = CurrencyAmount.of(USD, -NOTIONAL * STRIKE_RATE_LOW);
  private static final ResolvedFxSingle FX_PRODUCT = ResolvedFxSingle.of(EUR_AMOUNT_REC, USD_AMOUNT_PAY, PAY_DATE);
  private static final ResolvedFxVanillaOption CALL = ResolvedFxVanillaOption.builder()
      .longShort(LONG)
      .expiry(EXPIRY_DATETIME)
      .underlying(FX_PRODUCT)
      .build();
  private static final ResolvedFxSingleBarrierOption CALL_DKO =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKO);
  private static final ResolvedFxSingleBarrierOption CALL_UKO =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_UKO);

  private static final ResolvedFxSingleBarrierOption CALL_DKI =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKI);
  private static final ResolvedFxSingleBarrierOption CALL_UKI =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_UKI);

  private static final ResolvedFxSingleBarrierOption CALL_DKO_C =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKO, REBATE);
  private static final ResolvedFxSingleBarrierOption CALL_UKO_B =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_UKO, REBATE_BASE);

  private static final ResolvedFxSingleBarrierOption CALL_DKI_B =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_DKI, REBATE_BASE);
  private static final ResolvedFxSingleBarrierOption CALL_UKI_C =
      ResolvedFxSingleBarrierOption.of(CALL, BARRIER_UKI, REBATE);

  private static final CurrencyAmount EUR_AMOUNT_PAY = CurrencyAmount.of(EUR, -NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT_REC = CurrencyAmount.of(USD, NOTIONAL * STRIKE_RATE_HIGH);
  private static final ResolvedFxSingle FX_PRODUCT_INV = ResolvedFxSingle.of(EUR_AMOUNT_PAY, USD_AMOUNT_REC, PAY_DATE);
  private static final ResolvedFxVanillaOption PUT = ResolvedFxVanillaOption.builder()
      .longShort(SHORT)
      .expiry(EXPIRY_DATETIME)
      .underlying(FX_PRODUCT_INV)
      .build();
  private static final ResolvedFxSingleBarrierOption PUT_DKO =
      ResolvedFxSingleBarrierOption.of(PUT, BARRIER_DKO);
  private static final ResolvedFxSingleBarrierOption PUT_UKO =
      ResolvedFxSingleBarrierOption.of(PUT, BARRIER_UKO);

  private static final ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer PRICER = new ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(
      151);
  private static final BlackFxSingleBarrierOptionProductPricer BLACK_PRICER = BlackFxSingleBarrierOptionProductPricer.DEFAULT;

  @Test(enabled = false)
  public void test_change_numNodes() {
    double priceBlack = BLACK_PRICER.price(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    for (int i = 4; i < 150; i += 5) {
      ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer pricer =
          new ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(i);
      double price = pricer.price(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
      System.out.println(i + "\t" + price + "\t" + priceBlack);
    }
  }

  @Test(enabled = false)
  public void test_change_numNodes_in() {
    double priceBlack = BLACK_PRICER.price(CALL_UKI, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    for (int i = 4; i < 150; i += 5) {
      ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer pricer =
          new ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(i);
      double price = pricer.price(CALL_UKI, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
      System.out.println(i + "\t" + price + "\t" + priceBlack);
    }
  }

  @Test(enabled = false)
  public void test_change_numNodes_rebate() {
    double priceBlack = BLACK_PRICER.price(CALL_UKO_B, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    for (int i = 4; i < 150; i += 5) {
      ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer pricer =
          new ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(i);
      double price = pricer.price(CALL_UKO_B, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
      System.out.println(i + "\t" + price + "\t" + priceBlack);
    }
  }

  @Test(enabled = false)
  public void test_change_numNodes_in_rebate() {
    double priceBlack = BLACK_PRICER.price(CALL_DKI_B, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    for (int i = 4; i < 150; i += 5) {
      ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer pricer =
          new ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(i);
      double price = pricer.price(CALL_DKI_B, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
      System.out.println(i + "\t" + price + "\t" + priceBlack);
    }
  }

  @Test(enabled = false)
  public void test11() {
    double priceBlack = BLACK_PRICER.price(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    for (int i = 4; i < 100; i += 1) {
      ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer pricer =
          new ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(i);
      double price = pricer.price(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
      System.out.println(i + "\t" + price + "\t" + priceBlack);
    }
  }

  @Test(enabled = false)
  public void test1() {
    double priceBlack = BLACK_PRICER.price(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    RecombiningTrinomialTreeData data = PRICER.calibrateTrinomialTree(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    double price = PRICER.price(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
    System.out.println(price + "\t" + priceBlack);
  }

  @Test(enabled = false)
  public void test_change_barrier() {
    RecombiningTrinomialTreeData data = PRICER.calibrateTrinomialTree(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    for (int i = 0; i < 50; ++i) {
      //      double barrier = 1.1 + 0.006 * i;
      //      SimpleConstantContinuousBarrier dko =
      //          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, barrier);
      double barrier = 1.4 + 0.006 * (i + 1);
      SimpleConstantContinuousBarrier uko =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, barrier);
      ResolvedFxSingleBarrierOption option =
          ResolvedFxSingleBarrierOption.of(CALL, uko);
      double priceBlack = BLACK_PRICER.price(option, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
      double price = PRICER.price(option, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      System.out.println(barrier + "\t" + price + "\t" + priceBlack);
    }
  }

  @Test(enabled = false)
  public void test_change_barrier_in() {
    RecombiningTrinomialTreeData data = PRICER.calibrateTrinomialTree(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    for (int i = 0; i < 50; ++i) {
      double barrier = 1.1 + 0.006 * i;
      SimpleConstantContinuousBarrier dki =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, barrier);
      ResolvedFxSingleBarrierOption option =
          ResolvedFxSingleBarrierOption.of(CALL, dki);
      //      double barrier = 1.4 + 0.006 * (i + 1);
      //      SimpleConstantContinuousBarrier uki =
      //          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, barrier);
      //      ResolvedFxSingleBarrierOption option =
      //          ResolvedFxSingleBarrierOption.of(CALL, uki);
      double priceBlack = BLACK_PRICER.price(option, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
      double price = PRICER.price(option, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      System.out.println(barrier + "\t" + price + "\t" + priceBlack);
    }
  }

  @Test(enabled = false)
  public void test_change_barrier_rebate() {
    RecombiningTrinomialTreeData data = PRICER.calibrateTrinomialTree(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    for (int i = 0; i < 50; ++i) {
      //      double barrier = 1.1 + 0.006 * i;
      //      SimpleConstantContinuousBarrier dko =
      //          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, barrier);
      //      ResolvedFxSingleBarrierOption option =
      //          ResolvedFxSingleBarrierOption.of(CALL, dko, REBATE);
      double barrier = 1.4 + 0.006 * (i + 1);
      SimpleConstantContinuousBarrier uko =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, barrier);
      ResolvedFxSingleBarrierOption option =
          ResolvedFxSingleBarrierOption.of(CALL, uko, REBATE_BASE);
      double priceBlack = BLACK_PRICER.price(option, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
      double price = PRICER.price(option, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      System.out.println(barrier + "\t" + price + "\t" + priceBlack);
    }
  }

  @Test(enabled = false)
  public void test_change_barrier_in_rebate() {
    RecombiningTrinomialTreeData data = PRICER.calibrateTrinomialTree(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    for (int i = 0; i < 50; ++i) {
      //      double barrier = 1.1 + 0.006 * i;
      //      SimpleConstantContinuousBarrier dki =
      //          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, barrier);
      //      ResolvedFxSingleBarrierOption option =
      //          ResolvedFxSingleBarrierOption.of(CALL, dki, REBATE);
      double barrier = 1.4 + 0.006 * (i + 1);
      SimpleConstantContinuousBarrier uki =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, barrier);
      ResolvedFxSingleBarrierOption option =
          ResolvedFxSingleBarrierOption.of(CALL, uki, REBATE_BASE);
      double priceBlack = BLACK_PRICER.price(option, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
      double price = PRICER.price(option, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      System.out.println(barrier + "\t" + price + "\t" + priceBlack);
    }
  }

  @Test(enabled = false)
  public void test_change_barrier_uniform() {
    double vol = 0.18;
    double rate = 0.011;
    double div = 0.015;
    double time = 1d;
    double spot = 1.4;
    double strike = 1.35;
    int nSteps = 151;
    TrinomialTree tree = new TrinomialTree();
    CoxRossRubinsteinLatticeSpecification lattice = CoxRossRubinsteinLatticeSpecification.of(nSteps);

    for (int i = 0; i < 50; ++i) {
      //      double barrier = 1.1 + 0.006 * i;
      //      SimpleConstantContinuousBarrier dko =
      //          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, barrier);
      double barrier = 1.4 + 0.006 * (i + 1);

      ConstantKnockoutOptionFunction func = ConstantKnockoutOptionFunction.of(strike, time, PutCall.CALL,
          BarrierType.UP, barrier, DoubleArray.ofUnsafe(new double[nSteps + 1]));
      double price = tree.optionPrice(lattice, func, spot, vol, rate, div);
      double blackPrice = (new BlackBarrierPriceFormulaRepository()).price(spot, strike, time, rate - div, rate, vol,
          true, SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, barrier));
      System.out.println(barrier + "\t" + price + "\t" + blackPrice);
    }
  }
}
