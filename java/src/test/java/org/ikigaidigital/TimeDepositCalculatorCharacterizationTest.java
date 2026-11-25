package org.ikigaidigital;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Characterization tests: pin the CURRENT behaviour of {@link TimeDepositCalculator#updateBalance(List)}.
 *
 * <p>README: "The existing TimeDepositCalculator.updateBalance method is functioning correctly. Ensure its
 * behavior remains unchanged after refactoring." Every expected value below was captured by running the original
 * implementation, then checked against the README interest rules. These tests are FROZEN: a refactor that
 * breaks one is wrong. Never change an expected value to make a refactor pass.
 *
 * <p>Pinned behaviour (matches the README rules):
 * <ul>
 *   <li>No interest while {@code days <= 30}, for every plan.</li>
 *   <li>basic: balance * 0.01 / 12 from day 31.</li>
 *   <li>student: balance * 0.03 / 12 on days 31..365, none from day 366 ("no interest after 1 year").</li>
 *   <li>premium: balance * 0.05 / 12 from day 46 ("interest starts after 45 days").</li>
 *   <li>Any other plan type, including different letter case, gets no interest.</li>
 *   <li>The monthly interest is rounded to 2 decimals (HALF_UP, on the exact binary value of the double, i.e.
 *       {@code new BigDecimal(double)}, not {@code BigDecimal.valueOf(double)}) and then
 *       added to the {@code double} balance, so some results carry double artifacts (e.g. 1000.9300000000001).
 *       They're pinned exactly on purpose.</li>
 * </ul>
 *
 * <p>Out of scope: null plan types, negative balances and negative days. README: "Handling invalid input or
 * exceptions is not required", so that behaviour is deliberately left unpinned.
 */
@DisplayName("TimeDepositCalculator.updateBalance (characterization)")
class TimeDepositCalculatorCharacterizationTest {

    private static final double BALANCE = 1000.00;

    @Nested
    @DisplayName("grace period: no interest for the first 30 days")
    class GracePeriod {

        @ParameterizedTest(name = "{0} plan at {1} days keeps balance {2}")
        @CsvSource({
                "basic,   0,  1000.0",
                "basic,   1,  1000.0",
                "basic,   30, 1000.0",
                "student, 0,  1000.0",
                "student, 1,  1000.0",
                "student, 30, 1000.0",
                "premium, 0,  1000.0",
                "premium, 1,  1000.0",
                "premium, 30, 1000.0",
        })
        void appliesNoInterestUpToAndIncludingDay30(String planType, int days, double expectedBalance) {
            assertThat(balanceAfterOneUpdate(planType, BALANCE, days)).isEqualTo(expectedBalance);
        }
    }

    @Nested
    @DisplayName("basic plan: 1% a year, applied monthly")
    class BasicPlan {

        @ParameterizedTest(name = "basic at {0} days -> {1}")
        @CsvSource({
                "31,         1000.83",
                "45,         1000.83",
                "46,         1000.83",
                "365,        1000.83",
                "366,        1000.83",
                "10000,      1000.83",
                "2147483647, 1000.83",
        })
        void appliesMonthlyInterestFromDay31WithNoUpperLimit(int days, double expectedBalance) {
            assertThat(balanceAfterOneUpdate("basic", BALANCE, days)).isEqualTo(expectedBalance);
        }
    }

    @Nested
    @DisplayName("student plan: 3% a year, applied monthly, no interest after 1 year")
    class StudentPlan {

        @ParameterizedTest(name = "student at {0} days -> {1}")
        @CsvSource({
                "31,  1002.5",
                "45,  1002.5",
                "46,  1002.5",
                "365, 1002.5",
        })
        void appliesMonthlyInterestFromDay31UpToAndIncludingDay365(int days, double expectedBalance) {
            assertThat(balanceAfterOneUpdate("student", BALANCE, days)).isEqualTo(expectedBalance);
        }

        @ParameterizedTest(name = "student at {0} days keeps balance")
        @CsvSource({"366", "10000", "2147483647"})
        void appliesNoInterestFromDay366(int days) {
            assertThat(balanceAfterOneUpdate("student", BALANCE, days)).isEqualTo(BALANCE);
        }
    }

    @Nested
    @DisplayName("premium plan: 5% a year, applied monthly, interest starts after 45 days")
    class PremiumPlan {

        @ParameterizedTest(name = "premium at {0} days keeps balance")
        @CsvSource({"31", "45"})
        void appliesNoInterestFromDay31UpToAndIncludingDay45(int days) {
            assertThat(balanceAfterOneUpdate("premium", BALANCE, days)).isEqualTo(BALANCE);
        }

        @ParameterizedTest(name = "premium at {0} days -> {1}")
        @CsvSource({
                "46,         1004.17",
                "365,        1004.17",
                "366,        1004.17",
                "10000,      1004.17",
                "2147483647, 1004.17",
        })
        void appliesMonthlyInterestFromDay46WithNoUpperLimit(int days, double expectedBalance) {
            assertThat(balanceAfterOneUpdate("premium", BALANCE, days)).isEqualTo(expectedBalance);
        }
    }

    @Nested
    @DisplayName("unknown plan types: no interest")
    class UnknownPlanType {

        // README defines only basic, student and premium. Plan matching is exact and case-sensitive.
        @ParameterizedTest(name = "plan ''{0}'' at {1} days keeps balance")
        @CsvSource({
                "gold,    31",
                "gold,    366",
                "Basic,   31",
                "STUDENT, 100",
                "PREMIUM, 46",
                "'',      100",
        })
        void appliesNoInterestToUnrecognisedPlanTypes(String planType, int days) {
            assertThat(balanceAfterOneUpdate(planType, BALANCE, days)).isEqualTo(BALANCE);
        }
    }

    @Nested
    @DisplayName("rounding: monthly interest rounded to 2 decimals, HALF_UP")
    class Rounding {

        @ParameterizedTest(name = "{0} plan, balance {1} -> {2}")
        @CsvSource({
                // interest below half a cent rounds down to zero
                "basic,   0.01,       0.01",
                "basic,   0.1,        0.1",
                "basic,   1.0,        1.0",
                "basic,   3.0,        3.0",          // raw 0.0025 -> 0.00
                "student, 1.0,        1.0",          // raw 0.0025 -> 0.00
                "premium, 1.0,        1.0",          // raw 0.0041666... -> 0.00
                // binary value just BELOW a half cent rounds down, even though it prints as x.xx5.
                // The legacy code rounds new BigDecimal(double), the exact binary value, not BigDecimal.valueOf(double).
                "student, 6.0,        6.01",         // raw 0.01499999999999999944 -> 0.01 (valueOf would give 0.02)
                "student, 10.0,       10.02",        // raw 0.02499999999999999792 -> 0.02
                // exactly or just above half a cent rounds up
                "basic,   6.0,        6.01",         // raw 0.00500000000000000010 -> 0.01
                "student, 2.0,        2.01",         // raw 0.00500000000000000010 -> 0.01
                "premium, 2.0,        2.01",         // raw 0.0083333... -> 0.01
                // exact binary halves round up
                "basic,   150.0,      150.13",       // raw 0.125 -> 0.13
                "student, 150.0,      150.38",       // raw 0.375 -> 0.38
                "premium, 150.0,      150.63",       // raw 0.625 -> 0.63
                "premium, 3.0,        3.01",         // raw 0.0125000000000000024 -> 0.01
                "premium, 6.0,        6.03",         // raw 0.0250000000000000049 -> 0.03
                // no rounding needed
                "basic,   300.0,      300.25",
                "premium, 300.0,      301.25",
                // double artifacts of adding the rounded interest to a double balance, pinned exactly
                "basic,   1000.1,     1000.9300000000001",
                "student, 1234.56,    1237.6499999999999",
                "student, 1000.1,     1002.6",
                "premium, 1234.56,    1239.7",
                // large balances
                "basic,   1234567.0,  1235595.81",
                "student, 1234567.0,  1237653.42",
                "premium, 1234567.0,  1239711.03",
                "basic,   9999999.99, 10008333.32",
                "student, 9999999.99, 10024999.99",
                "premium, 9999999.99, 10041666.66",
        })
        void roundsMonthlyInterestToTwoDecimalsBeforeAddingIt(String planType, double balance, double expectedBalance) {
            assertThat(balanceAfterOneUpdate(planType, balance, 60)).isEqualTo(expectedBalance);
        }

        @ParameterizedTest(name = "{0} plan keeps a zero balance at zero")
        @CsvSource({"basic", "student", "premium"})
        void keepsZeroBalanceAtZero(String planType) {
            assertThat(balanceAfterOneUpdate(planType, 0.0, 60)).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("batch and side effects")
    class BatchAndSideEffects {

        @Test
        void updatesEveryDepositInTheListIndependently() {
            List<TimeDeposit> deposits = List.of(
                    new TimeDeposit(1, "basic", BALANCE, 31),
                    new TimeDeposit(2, "student", BALANCE, 366),
                    new TimeDeposit(3, "premium", BALANCE, 46),
                    new TimeDeposit(4, "premium", BALANCE, 45),
                    new TimeDeposit(5, "gold", BALANCE, 100));

            new TimeDepositCalculator().updateBalance(deposits);

            assertThat(deposits)
                    .extracting(TimeDeposit::getId, TimeDeposit::getBalance)
                    .containsExactly(
                            tuple(1, 1000.83),
                            tuple(2, 1000.0),
                            tuple(3, 1004.17),
                            tuple(4, 1000.0),
                            tuple(5, 1000.0));
        }

        @Test
        void changesOnlyTheBalanceAndKeepsIdPlanTypeAndDays() {
            TimeDeposit deposit = new TimeDeposit(7, "premium", BALANCE, 46);

            new TimeDepositCalculator().updateBalance(List.of(deposit));

            assertThat(deposit.getId()).isEqualTo(7);
            assertThat(deposit.getPlanType()).isEqualTo("premium");
            assertThat(deposit.getDays()).isEqualTo(46);
            assertThat(deposit.getBalance()).isEqualTo(1004.17);
        }

        @Test
        void compoundsWhenCalledAgainBecauseEachCallAppliesOneMonthOfInterest() {
            // Relevant for the "update all balances" endpoint: every call adds another month of interest.
            TimeDeposit deposit = new TimeDeposit(1, "premium", BALANCE, 46);
            TimeDepositCalculator calculator = new TimeDepositCalculator();

            calculator.updateBalance(List.of(deposit));
            calculator.updateBalance(List.of(deposit));

            assertThat(deposit.getBalance()).isEqualTo(1008.3499999999999);
        }

        @Test
        void acceptsAnUnmodifiableListBecauseItMutatesDepositsNotTheList() {
            List<TimeDeposit> deposits = List.of(new TimeDeposit(1, "basic", BALANCE, 31));

            assertThatCode(() -> new TimeDepositCalculator().updateBalance(deposits)).doesNotThrowAnyException();
            assertThat(deposits.get(0).getBalance()).isEqualTo(1000.83);
        }

        @Test
        void handlesAnEmptyList() {
            List<TimeDeposit> deposits = new ArrayList<>();

            assertThatCode(() -> new TimeDepositCalculator().updateBalance(deposits)).doesNotThrowAnyException();
            assertThat(deposits).isEmpty();
        }
    }

    private static double balanceAfterOneUpdate(String planType, double balance, int days) {
        TimeDeposit deposit = new TimeDeposit(1, planType, balance, days);
        new TimeDepositCalculator().updateBalance(List.of(deposit));
        return deposit.getBalance();
    }
}
