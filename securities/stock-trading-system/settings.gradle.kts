rootProject.name = "stock-trading-system"
include(
    "libs:common-domain",
    "libs:common-event",
    "services:order-service",
    "services:order-router",
    "services:mock-krx",
    "services:execution-service",
    "services:balance-service",
    "services:account-service",
    "services:settlement-service"
)
