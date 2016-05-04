package com.opengamma.strata.pricer.fx;

import java.util.Arrays;
import java.util.function.Function;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.view.DiscountFactors;
import com.opengamma.strata.pricer.impl.tree.ConstantKnockoutOptionFunction;
import com.opengamma.strata.pricer.impl.tree.EuropeanVanillaOptionFunction;
import com.opengamma.strata.pricer.impl.tree.RecombiningTrinomialTreeData;
import com.opengamma.strata.pricer.impl.tree.TrinomialTree;
import com.opengamma.strata.pricer.impl.volatility.local.ImpliedTrinomialTreeLocalVolatilityCalculator;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fx.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fx.ResolvedFxVanillaOption;
import com.opengamma.strata.product.fx.SimpleConstantContinuousBarrier;

public class ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer {

  private static final TrinomialTree TREE = new TrinomialTree();

  private static final int NUM_STEPS_DEFAULT = 101;

  private final int nSteps;

  public ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer() {
    this(NUM_STEPS_DEFAULT);
  }

  public ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer(int nSteps) {
    ArgChecker.isTrue(nSteps > 2, "the number of steps should be greater than 1"); // TODO check
    this.nSteps = nSteps;
  }

  //-------------------------------------------------------------------------
  public double price(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    RecombiningTrinomialTreeData data = calibrateTrinomialTree(option, ratesProvider, volatilityProvider);
    return price(option, ratesProvider, volatilityProvider, data);
  }

  public double price(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider,
      RecombiningTrinomialTreeData data) {

    validate(option, ratesProvider, volatilityProvider);
    // TODO check maxTime consistency between data and option

    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    double timeToExpiry = volatilityProvider.relativeTime(underlyingOption.getExpiry());
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
    DiscountFactors counterDiscountFactors = ratesProvider.discountFactors(ccyCounter);

    double rebate = 0d;
    double[] rebateArray = new double[nSteps + 1];
    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
    if (option.getRebate().isPresent()) {
      CurrencyAmount rebateCurrencyAmount = option.getRebate().get();
      rebate = rebateCurrencyAmount.getCurrency().equals(ccyCounter) ? rebateCurrencyAmount.getAmount() :
          rebateCurrencyAmount.getAmount() * barrier.getBarrierLevel();
    }
    double vanillaPrice = 0d;

    if (barrier.getKnockType().isKnockIn() && option.getRebate().isPresent()) {
      double dt = timeToExpiry / nSteps;
      double dfAtExpiry = counterDiscountFactors.discountFactor(timeToExpiry);
      for (int i = 0; i < nSteps + 1; ++i) {
        rebateArray[i] = -rebate * dfAtExpiry / counterDiscountFactors.discountFactor(dt * i);
      }
      EuropeanVanillaOptionFunction vanillaFunction = EuropeanVanillaOptionFunction.of(
          underlyingOption.getStrike(), timeToExpiry, underlyingOption.getPutCall());
      vanillaPrice = TREE.optionPrice(vanillaFunction, data);
    } else {
      Arrays.fill(rebateArray, rebate);
    }

    ConstantKnockoutOptionFunction barrierFunction = ConstantKnockoutOptionFunction.of(
        underlyingOption.getStrike(),
        timeToExpiry,
        underlyingOption.getPutCall(),
        barrier.getBarrierType(),
        barrier.getBarrierLevel(),
        DoubleArray.ofUnsafe(rebateArray));
    double barrierPrice = TREE.optionPrice(barrierFunction, data);
    return barrier.getKnockType().isKnockIn() ? vanillaPrice - barrierPrice : barrierPrice;
  }

  //-------------------------------------------------------------------------
  public RecombiningTrinomialTreeData calibrateTrinomialTree(
      ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    validate(option, ratesProvider, volatilityProvider);
    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
    double timeToExpiry = volatilityProvider.relativeTime(underlyingOption.getExpiry());
    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
    CurrencyPair currencyPair = underlyingFx.getCurrencyPair();
    Currency ccyBase = underlyingFx.getBaseCurrencyPayment().getCurrency();
    Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
    double todayFx = ratesProvider.fxRate(currencyPair);
    DiscountFactors baseDiscountFactors = ratesProvider.discountFactors(ccyBase);
    DiscountFactors counterDiscountFactors = ratesProvider.discountFactors(ccyCounter);

    Function<Double, Double> interestRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double t) {
        return counterDiscountFactors.zeroRate(t);
      }
    };
    Function<Double, Double> dividendRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double t) {
        return baseDiscountFactors.zeroRate(t);
      }
    };
    Function<DoublesPair, Double> impliedVolSurface = new Function<DoublesPair, Double>() {
      @Override
      public Double apply(DoublesPair tk) {
        double dfBase = baseDiscountFactors.discountFactor(tk.getFirst());
        double dfCounter = counterDiscountFactors.discountFactor(tk.getFirst());
        double forward = todayFx * dfBase / dfCounter;
        return volatilityProvider.getVolatility(currencyPair, tk.getFirst(), tk.getSecond(), forward);
      }
    };
    ImpliedTrinomialTreeLocalVolatilityCalculator localVol =
        new ImpliedTrinomialTreeLocalVolatilityCalculator(nSteps, timeToExpiry);
    return localVol.calibrateImpliedVolatility(impliedVolSurface, todayFx, interestRate, dividendRate);
  }

  //-------------------------------------------------------------------------
  private void validate(ResolvedFxSingleBarrierOption option,
      RatesProvider ratesProvider,
      BlackVolatilityFxProvider volatilityProvider) {

    ArgChecker.isTrue(option.getBarrier() instanceof SimpleConstantContinuousBarrier,
        "barrier should be SimpleConstantContinuousBarrier");
    ArgChecker.isTrue(
        ratesProvider.getValuationDate().isEqual(volatilityProvider.getValuationDateTime().toLocalDate()),
        "Volatility and rate data must be for the same date");
  }
}
