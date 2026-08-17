package com.mcm.privatecircle.customer.service;

import com.mcm.privatecircle.customer.dto.CustomerActivitySummary;

public interface CustomerActivitySummaryReader {

    CustomerActivitySummary read(Long customerId);
}
