package com.takarub.esim.pricing.application.command;

import java.math.BigDecimal;

public record UpsertGlobalMarkupCommand(BigDecimal percentage) {
}
