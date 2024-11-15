package com.nationale.currency.nbp;

import java.math.BigDecimal;
import java.util.List;

public record Exchange(String code, List<ExchangeRate> rates) {

    public BigDecimal getFirst() {
        return rates.getFirst().mid();
    }
}
