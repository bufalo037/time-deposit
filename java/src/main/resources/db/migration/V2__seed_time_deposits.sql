-- Sample data so both endpoints return meaningful results right after startup (ADR 0004).
-- One deposit per plan before and after its interest starts, so "update all balances" visibly changes some balances
-- (ids 2, 3, 6) and leaves others unchanged (ids 1, 4, 5):
--   1 basic    20 days  -> grace period (no interest for the first 30 days)
--   2 basic   120 days  -> 1% a year, applied monthly
--   3 student 200 days  -> 3% a year, applied monthly (under 1 year)
--   4 student 400 days  -> no interest after 1 year
--   5 premium  40 days  -> no interest until after 45 days
--   6 premium  90 days  -> 5% a year, applied monthly
-- Assumption: balance is the current balance, already net of withdrawals. Withdrawals are historical records and
-- don't affect interest (README defines interest only by plan type and days).
-- Explicit ids keep the data deterministic. The identity sequences are reset afterwards so new rows don't collide.

INSERT INTO time_deposits (id, plan_type, days, balance) VALUES
    (1, 'basic',   20,  10000.00),
    (2, 'basic',   120, 10000.00),
    (3, 'student', 200, 5000.00),
    (4, 'student', 400, 5000.00),
    (5, 'premium', 40,  20000.00),
    (6, 'premium', 90,  20000.00);

-- Deposits 1, 4 and 5 have no withdrawals, so the API also shows empty lists.
INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES
    (1, 2, 500.00,  DATE '2026-09-15'),
    (2, 3, 250.00,  DATE '2026-07-01'),
    (3, 6, 1000.00, DATE '2026-09-10'),
    (4, 6, 500.00,  DATE '2026-10-20');

SELECT setval(pg_get_serial_sequence('time_deposits', 'id'), (SELECT MAX(id) FROM time_deposits));
SELECT setval(pg_get_serial_sequence('withdrawals', 'id'), (SELECT MAX(id) FROM withdrawals));
