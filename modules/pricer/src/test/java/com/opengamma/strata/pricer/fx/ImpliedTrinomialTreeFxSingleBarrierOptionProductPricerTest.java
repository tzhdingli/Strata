/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.LongShort.SHORT;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.impl.option.BlackBarrierPriceFormulaRepository;
import com.opengamma.strata.pricer.impl.tree.ConstantContinuousSingleBarrierKnockoutFunction;
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

/**
 * Test {@link ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer}.
 */
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

  @Test
  public void regression() {

    double[][] exp = new double[][] {
      {0.169367632458499, 5.912545053582604E-6, 0.01638727722865993, 0.18574899714210535, 0.19812586719034458,
        0.047434714745728544, 0.039012221435933137, 0.18884692784531476 },
      {
        0.16871352954331315, 7.565766417551369E-6, 0.01704138014384579, 0.18574734392074138, 0.19793289746395692,
        0.04723933856138254, 0.039400416049828024, 0.18883230051195218 },
      {
        0.16817090810503968, 9.715520181607793E-6, 0.017584001582119257, 0.18574519416697732, 0.19779797493438328,
        0.04701189300569924, 0.03972656836542188, 0.1888631122645434 },
      {
        0.16771361625331496, 1.2563069656117235E-5, 0.018041293433843975, 0.18574234661750283, 0.19770848652427053,
        0.04674382441038065, 0.040004273451056505, 0.18895157516437772 },
      {
        0.1636859441062303, 1.6418428121807524E-5, 0.022068965580928634, 0.18573849125903713, 0.19547671882876155,
        0.04642326393521116, 0.042627791740632, 0.18911467205846583 },
      {
        0.16184807329199677, 2.177713247153649E-5, 0.023906836395162168, 0.1857331325546874, 0.1944746956802298,
        0.046033265285850784, 0.04389676368833151, 0.18937671392800465 },
      {
        0.1604178578353176, 2.9466811783270368E-5, 0.025337051851841347, 0.18572544287537568, 0.1937225357379606,
        0.045548799375784065, 0.04489862974285297, 0.1897737172011254 },
      {
        0.1592740564809123, 4.09445595667748E-5, 0.02648085320624663, 0.18571396512759217, 0.19315045041611711,
        0.04493136480004293, 0.04570865442426478, 0.19036128778133096 },
      {
        0.15833891227347605, 1.1441434892472865E-4, 0.02741599741368289, 0.1856404953382342, 0.1927120894569823,
        0.04283060892647906, 0.046376577836298144, 0.1930143920234173 },
      {
        0.1575603520441323, 1.2585312171406194E-4, 0.02819455764302664, 0.18562905656544487, 0.19237558779775593,
        0.04264460512994919, 0.04693647180995114, 0.19302756341710037 },
      {
        0.1569022327228241, 1.3954846360411893E-4, 0.028852676964334828, 0.18561536122355482, 0.1921183736162998,
        0.042431865464184225, 0.04741240483731815, 0.19308112457191792 },
      {
        0.15137909687262088, 1.5618501241275234E-4, 0.034375812814538054, 0.1855987246747462, 0.18845507636662115,
        0.042186214542150574, 0.051553077806271747, 0.19318452597018668 },
      {
        0.1487274534672741, 1.7673735959685235E-4, 0.03702745621988485, 0.18557817232756207, 0.18672037298531854,
        0.04189943080570688, 0.05360945935475442, 0.19335039455218192 },
      {
        0.1466666315101969, 2.0263313168262403E-4, 0.03908827817696203, 0.18555227655547632, 0.18540437054243458,
        0.0415603303958505, 0.05522207580353358, 0.193595978850279 },
      {
        0.14502020425541215, 2.3603441905296316E-4, 0.04073470543174679, 0.18551887526810598, 0.18438670986366512,
        0.04115331615092171, 0.0565192730945249, 0.1939454532872825 },
      {
        0.14367525226008743, 2.8034646026384104E-4, 0.04207965742707151, 0.1854745632268951, 0.18358872202829307,
        0.040655993556021196, 0.05758465605331839, 0.19443373470187342 },
      {
        0.1425562804821941, 3.411883019149657E-4, 0.04319862920496484, 0.18541372138524398, 0.18295696121538013,
        0.04003508291810453, 0.0584748790690017, 0.1951130902619553 },
      {
        0.14161096190683986, 4.283674838705196E-4, 0.044143947780319076, 0.18532654220328842, 0.1824539131842228,
        0.03923905037249994, 0.05922962973791443, 0.19606519307426945 },
      {
        0.1353048360120745, 6.627460167003706E-4, 0.050450073675084445, 0.18509216367045855, 0.17784801686623536,
        0.03805756574031216, 0.06435253999519724, 0.19764155601139077 },
      {
        0.13136860786889062, 7.036604985590385E-4, 0.05438630181826831, 0.1850512491885999, 0.17496819583171305,
        0.03789234845966398, 0.06762308782907842, 0.1976811131748726 },
      {
        0.128339253116515, 7.519188428676202E-4, 0.05741565657064393, 0.1850029908442913, 0.17278804572734804,
        0.03770402322731098, 0.07015658099125432, 0.19776010779667313 },
      {
        0.12593782538687123, 8.096063446723807E-4, 0.059817084300287704, 0.18494530334248654, 0.1710979495074909,
        0.03748742870964878, 0.07217489674605296, 0.1978876322862771 },
      {
        0.12398849328480048, 8.796486275946958E-4, 0.06176641640235846, 0.18487526105956423, 0.1697637879181255,
        0.037235763162210125, 0.07381961117847799, 0.19807578045977262 },
      {
        0.12237515791565369, 9.662659711322755E-4, 0.06337975177150525, 0.18478864371602666, 0.16869598455813412,
        0.03693988750324173, 0.07518508546479585, 0.19834098306889766 },
      {
        0.1210181787380287, 0.0010757465766465118, 0.06473673094913024, 0.1846791631105124, 0.16783253552745453,
        0.03658724798892655, 0.07633652995295392, 0.1987061158439714 },
      {
        0.11986115496138154, 0.0012178237216511722, 0.0658937547257774, 0.18453708596550777, 0.16712920909355536,
        0.03616015933045661, 0.07732040354169087, 0.19920394750554937 },
      {
        0.10995795664682095, 0.0014082618247153959, 0.07579695304033798, 0.18434664786244354, 0.15926704153048232,
        0.035632974221969364, 0.08585270619718413, 0.19988301628756122 },
      {
        0.10539905632424991, 0.002032469273302029, 0.08035585336290903, 0.1837224404138569, 0.1557177142718709,
        0.034585985348249484, 0.08982909461684818, 0.20156590082187165 },
      {
        0.1018433926207121, 0.002111636962036281, 0.08391151706644684, 0.18364327272512265, 0.15299204449957576,
        0.03449340194266889, 0.09294310417652446, 0.2015553252957476 },
      {
        0.09899463652329471, 0.0022030326782051177, 0.08676027316386423, 0.1835518770089538, 0.15085097626097718,
        0.03438988582007837, 0.09544591343397935, 0.2015696333296253 },
      {
        0.09666209445798633, 0.0023096550225456215, 0.0890928152291726, 0.1834452546646133, 0.14913923370858514,
        0.03427343751294505, 0.09750036528173686, 0.20161386264677766 },
      {
        0.09471770239698372, 0.0024355458235319964, 0.09103720729017521, 0.18331936386362693, 0.14775177432614683,
        0.03414156068851682, 0.09921646002426876, 0.20169449246287804 },
      {
        0.09307236410964273, 0.002586275460855882, 0.0926825455775162, 0.18316863422630306, 0.146615088489976,
        0.03399110953618276, 0.10067108688544346, 0.20181999082817553 },
      {
        0.09166222641159252, 0.0027697196289313895, 0.09409268327556641, 0.18298519005822755, 0.145676239688176,
        0.03381808500936995, 0.10191956342657882, 0.2020016269207027 },
      {
        0.07878698451313858, 0.002997348453288641, 0.10696792517402036, 0.1827575612338703, 0.13495263447875155,
        0.03361736776082979, 0.11342165751446659, 0.2022547071388795 },
      {
        0.07350968959671958, 0.0032864572059023746, 0.11224522009043936, 0.18246845248125657, 0.13067020847834157,
        0.033382386057435515, 0.11816876322942085, 0.20260050896455062 },
      {
        0.06937629288746154, 0.0036642183782276488, 0.1163786167996974, 0.1820906913089313, 0.12736414149805955,
        0.03310476194231099, 0.12189775795782787, 0.2030693999850166 },
      {
        0.0660533980593332, 0.004782531813188737, 0.11970151162782573, 0.1809723778739702, 0.12475350995254614,
        0.03275159632671795, 0.1249024101391919, 0.2040816943354904 },
      {
        0.06332504631514416, 0.004917590786779237, 0.12242986337201478, 0.1808373189003797, 0.1226551328244023,
        0.03273435482858358, 0.127373994936478, 0.20402221530425457 },
      {
        0.061045420043318165, 0.005071931190253526, 0.12470948964384077, 0.1806829784969054, 0.12094467044229049,
        0.03271678727422938, 0.12944219559784048, 0.20397622347918207 },
      {
        0.059112611254829465, 0.00524992441013189, 0.12664229843232946, 0.18050498527702705, 0.11953485230240744,
        0.032699239325618745, 0.1311979479372835, 0.20394609593197133 },
      {
        0.05745330370307346, 0.005457346376882777, 0.12830160598408547, 0.18029756331027616, 0.1183626701995357,
        0.032682285557673774, 0.1327068559146049, 0.20393478186452121 },
      {
        0.04257038516909604, 0.005701979665847029, 0.1431845245180629, 0.1800529300213119, 0.10558001375770919,
        0.032666872081900285, 0.1463302453694198, 0.20394597534668038 },
      {
        0.03726146173112964, 0.00599454765166809, 0.1484934479560293, 0.17976036203549084, 0.10118372354499912,
        0.03265455957506297, 0.15120235593566728, 0.2039843477783165 },
      {
        0.03312072189331142, 0.006350210834894966, 0.15263418779384752, 0.17940469885226398, 0.09780954295702594,
        0.03264794972865551, 0.155011355380067, 0.20405585946004534 },
      {
        0.02980307852121251, 0.006791052749277588, 0.15595183116594644, 0.17896385693788136, 0.09515861513528324,
        0.0326514596188553, 0.15806885025549472, 0.20416816934221008 },
      {
        0.02708648821475916, 0.007350387211614607, 0.1586684214723998, 0.17840452247554434, 0.09303762030926208,
        0.03267278522521869, 0.16057615079762055, 0.20433114536765942 },
      {
        0.024821844297479165, 0.00923740251568898, 0.16093306538967977, 0.17651750717146997, 0.09131618835045621,
        0.033084928207597175, 0.16266887826508775, 0.2046992902252813 },
      {
        0.022905414871052632, 0.00942733022707724, 0.1628494948161063, 0.1763275794600817, 0.08990330763586901,
        0.03314716578254975, 0.16444163233413925, 0.20457833438235973 },
      {
        0.021262858812403043, 0.00964197116828423, 0.16449205087475588, 0.17611293851887472, 0.08873356551473373,
        0.033218779895174404, 0.16596235835943196, 0.20445938067658964 }
    };

    RecombiningTrinomialTreeData data = PRICER.calibrateTrinomialTree(CALL_DKO, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT);
    for (int i = 0; i < 50; ++i) {
      double barrierLow = 1.1 + 0.006 * i;
      SimpleConstantContinuousBarrier dki =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, barrierLow);
      SimpleConstantContinuousBarrier dko =
          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, barrierLow);
      ResolvedFxSingleBarrierOption optionDko =
          ResolvedFxSingleBarrierOption.of(CALL, dko);
      ResolvedFxSingleBarrierOption optionDki =
          ResolvedFxSingleBarrierOption.of(CALL, dki);
      ResolvedFxSingleBarrierOption optionDkoR =
          ResolvedFxSingleBarrierOption.of(CALL, dko, REBATE_BASE);
      ResolvedFxSingleBarrierOption optionDkiR =
          ResolvedFxSingleBarrierOption.of(CALL, dki, REBATE);
      double barrierHigh = 1.4 + 0.006 * (i + 1);
      SimpleConstantContinuousBarrier uki =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, barrierHigh);
      SimpleConstantContinuousBarrier uko =
          SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, barrierHigh);
      ResolvedFxSingleBarrierOption optionUko =
          ResolvedFxSingleBarrierOption.of(CALL, uko);
      ResolvedFxSingleBarrierOption optionUki =
          ResolvedFxSingleBarrierOption.of(CALL, uki);
      ResolvedFxSingleBarrierOption optionUkoR =
          ResolvedFxSingleBarrierOption.of(CALL, uko, REBATE);
      ResolvedFxSingleBarrierOption optionUkiR =
          ResolvedFxSingleBarrierOption.of(CALL, uki, REBATE_BASE);

      double priceDko = PRICER.price(optionDko, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      double priceUko = PRICER.price(optionUko, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      double priceDki = PRICER.price(optionDki, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      double priceUki = PRICER.price(optionUki, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      double priceDkoR = PRICER.price(optionDkoR, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      double priceUkoR = PRICER.price(optionUkoR, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      double priceDkiR = PRICER.price(optionDkiR, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      double priceUkiR = PRICER.price(optionUkiR, RATE_PROVIDER_FLAT, VOL_PROVIDER_FLAT, data);
      //      System.out.println(priceDko + ", " + priceUko + ", " + priceDki + ", " + priceUki + ", " +
      //          priceDkoR + ", " + priceUkoR + ", " + priceDkiR + ", " + priceUkiR);
      assertEquals(priceDko, exp[i][0], 1.0e-14);
      assertEquals(priceUko, exp[i][1], 1.0e-14);
      assertEquals(priceDki, exp[i][2], 1.0e-14);
      assertEquals(priceUki, exp[i][3], 1.0e-14);
      assertEquals(priceDkoR, exp[i][4], 1.0e-14);
      assertEquals(priceUkoR, exp[i][5], 1.0e-14);
      assertEquals(priceDkiR, exp[i][6], 1.0e-14);
      assertEquals(priceUkiR, exp[i][7], 1.0e-14);
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
    CoxRossRubinsteinLatticeSpecification lattice = new CoxRossRubinsteinLatticeSpecification();

    for (int i = 0; i < 50; ++i) {
      //      double barrier = 1.1 + 0.006 * i;
      //      SimpleConstantContinuousBarrier dko =
      //          SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, barrier);
      double barrier = 1.4 + 0.006 * (i + 1);

      ConstantContinuousSingleBarrierKnockoutFunction func = ConstantContinuousSingleBarrierKnockoutFunction.of(strike,
          time, PutCall.CALL,
          nSteps,
          BarrierType.UP, barrier, DoubleArray.ofUnsafe(new double[nSteps + 1]));
      double price = tree.optionPrice(lattice, func, spot, vol, rate, div);
      double blackPrice = (new BlackBarrierPriceFormulaRepository()).price(spot, strike, time, rate - div, rate, vol,
          true, SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, barrier));
      System.out.println(barrier + "\t" + price + "\t" + blackPrice);
    }
  }
}
