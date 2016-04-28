package com.opengamma.strata.pricer.impl.tree;

public interface SingleBarrierOptionFunction extends OptionFunction {

  public abstract double barrierLevel(double time);

}
