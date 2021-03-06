/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Parameter metadata based on a date and year-month.
 */
@BeanDefinition(builderScope = "private")
public final class YearMonthDateParameterMetadata
    implements DatedParameterMetadata, ImmutableBean, Serializable {

  /**
   * Formatter for Jan15.
   */
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMMuu", Locale.ENGLISH);

  /**
   * The date associated with the parameter.
   * <p>
   * This is the date that is most closely associated with the parameter.
   * The actual parameter is typically a year fraction based on a day count.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate date;
  /**
   * The year-month associated with the parameter.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth yearMonth;
  /**
   * The label that describes the parameter, defaulted to the year-month.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String label;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance using the year-month.
   * 
   * @param date  the date associated with the parameter
   * @param yearMonth  the year-month of the curve node
   * @return the parameter metadata based on the year-month
   */
  public static YearMonthDateParameterMetadata of(LocalDate date, YearMonth yearMonth) {
    ArgChecker.notNull(date, "date");
    ArgChecker.notNull(yearMonth, "yearMonth");
    return new YearMonthDateParameterMetadata(date, yearMonth, yearMonth.format(FORMATTER));
  }

  /**
   * Obtains an instance using the year-month, specifying the label.
   * 
   * @param date  the date associated with the parameter
   * @param yearMonth  the year-month of the curve node
   * @param label  the label to use
   * @return the parameter metadata based on the year-month
   */
  public static YearMonthDateParameterMetadata of(LocalDate date, YearMonth yearMonth, String label) {
    return new YearMonthDateParameterMetadata(date, yearMonth, label);
  }

  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.label == null && builder.yearMonth != null) {
      builder.label = builder.yearMonth.format(FORMATTER);
    }
  }

  /**
   * Gets the identifier, which is the year-month.
   *
   * @return the year-month
   */
  @Override
  public YearMonth getIdentifier() {
    return yearMonth;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code YearMonthDateParameterMetadata}.
   * @return the meta-bean, not null
   */
  public static YearMonthDateParameterMetadata.Meta meta() {
    return YearMonthDateParameterMetadata.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(YearMonthDateParameterMetadata.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private YearMonthDateParameterMetadata(
      LocalDate date,
      YearMonth yearMonth,
      String label) {
    JodaBeanUtils.notNull(date, "date");
    JodaBeanUtils.notNull(yearMonth, "yearMonth");
    JodaBeanUtils.notEmpty(label, "label");
    this.date = date;
    this.yearMonth = yearMonth;
    this.label = label;
  }

  @Override
  public YearMonthDateParameterMetadata.Meta metaBean() {
    return YearMonthDateParameterMetadata.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date associated with the parameter.
   * <p>
   * This is the date that is most closely associated with the parameter.
   * The actual parameter is typically a year fraction based on a day count.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getDate() {
    return date;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year-month associated with the parameter.
   * @return the value of the property, not null
   */
  public YearMonth getYearMonth() {
    return yearMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the label that describes the parameter, defaulted to the year-month.
   * @return the value of the property, not empty
   */
  @Override
  public String getLabel() {
    return label;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      YearMonthDateParameterMetadata other = (YearMonthDateParameterMetadata) obj;
      return JodaBeanUtils.equal(date, other.date) &&
          JodaBeanUtils.equal(yearMonth, other.yearMonth) &&
          JodaBeanUtils.equal(label, other.label);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(date);
    hash = hash * 31 + JodaBeanUtils.hashCode(yearMonth);
    hash = hash * 31 + JodaBeanUtils.hashCode(label);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("YearMonthDateParameterMetadata{");
    buf.append("date").append('=').append(date).append(',').append(' ');
    buf.append("yearMonth").append('=').append(yearMonth).append(',').append(' ');
    buf.append("label").append('=').append(JodaBeanUtils.toString(label));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code YearMonthDateParameterMetadata}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<LocalDate> date = DirectMetaProperty.ofImmutable(
        this, "date", YearMonthDateParameterMetadata.class, LocalDate.class);
    /**
     * The meta-property for the {@code yearMonth} property.
     */
    private final MetaProperty<YearMonth> yearMonth = DirectMetaProperty.ofImmutable(
        this, "yearMonth", YearMonthDateParameterMetadata.class, YearMonth.class);
    /**
     * The meta-property for the {@code label} property.
     */
    private final MetaProperty<String> label = DirectMetaProperty.ofImmutable(
        this, "label", YearMonthDateParameterMetadata.class, String.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "date",
        "yearMonth",
        "label");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3076014:  // date
          return date;
        case -496678845:  // yearMonth
          return yearMonth;
        case 102727412:  // label
          return label;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends YearMonthDateParameterMetadata> builder() {
      return new YearMonthDateParameterMetadata.Builder();
    }

    @Override
    public Class<? extends YearMonthDateParameterMetadata> beanType() {
      return YearMonthDateParameterMetadata.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code date} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> date() {
      return date;
    }

    /**
     * The meta-property for the {@code yearMonth} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YearMonth> yearMonth() {
      return yearMonth;
    }

    /**
     * The meta-property for the {@code label} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> label() {
      return label;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3076014:  // date
          return ((YearMonthDateParameterMetadata) bean).getDate();
        case -496678845:  // yearMonth
          return ((YearMonthDateParameterMetadata) bean).getYearMonth();
        case 102727412:  // label
          return ((YearMonthDateParameterMetadata) bean).getLabel();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code YearMonthDateParameterMetadata}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<YearMonthDateParameterMetadata> {

    private LocalDate date;
    private YearMonth yearMonth;
    private String label;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3076014:  // date
          return date;
        case -496678845:  // yearMonth
          return yearMonth;
        case 102727412:  // label
          return label;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3076014:  // date
          this.date = (LocalDate) newValue;
          break;
        case -496678845:  // yearMonth
          this.yearMonth = (YearMonth) newValue;
          break;
        case 102727412:  // label
          this.label = (String) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public YearMonthDateParameterMetadata build() {
      preBuild(this);
      return new YearMonthDateParameterMetadata(
          date,
          yearMonth,
          label);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("YearMonthDateParameterMetadata.Builder{");
      buf.append("date").append('=').append(JodaBeanUtils.toString(date)).append(',').append(' ');
      buf.append("yearMonth").append('=').append(JodaBeanUtils.toString(yearMonth)).append(',').append(' ');
      buf.append("label").append('=').append(JodaBeanUtils.toString(label));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
