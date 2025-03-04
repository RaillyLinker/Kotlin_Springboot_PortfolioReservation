plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "portfolio_reservation"

// 인증/인가 서버 (11000)
include("module-auth")

// 결제 서버 (11002)
include("module-payment")
include("module-service-rental-reservation")
