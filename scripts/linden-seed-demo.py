#!/usr/bin/env python3
"""Wipe ~/.linden/linden.db and seed realistic-but-fake demo data for Play Store screenshots.

Persona: London-based salaried user paid in GBP. Keeps a USD savings pot funded
by regular GBP->USD conversions, a US credit card for travel spending (paid from
the USD pot), US trips, a December bonus and the occasional reverse conversion.

Idempotent: re-running replaces all rows. No backups are made (caller keeps copies).
Nothing is ever dated after "now", so the app never enters its hidden-future state.
"""
import datetime as dt
import os
import random
import sqlite3
import zoneinfo

ZONE = zoneinfo.ZoneInfo("Europe/London")
DB_PATH = os.path.expanduser("~/.linden/linden.db")
NOW = dt.datetime.now(ZONE)
TODAY = NOW.date()

FX = 1.36  # demo GBP -> USD conversion rate

# ---------------------------------------------------------------- accounts --
# (id, name, currency, initialBalanceMinor)
ACCOUNTS = [
    (1, "Main Account", "GBP", 3_600_00),
    (2, "USD Savings", "USD", 4_000_00),
    (3, "US Credit Card", "USD", 0),
]

# (id, name, type, icon)
CATEGORIES = [
    (1, "Groceries", "Expense", "ShoppingCart"),
    (2, "Dining", "Expense", "Restaurant"),
    (3, "Housing", "Expense", "Home"),
    (4, "Shopping", "Expense", "ShoppingBag"),
    (5, "Bills & Fees", "Expense", "AccountBalance"),
    (6, "Health", "Expense", "LocalHospital"),
    (7, "Entertainment", "Expense", "Movie"),
    (8, "Travel", "Expense", "Flight"),
    (9, "Personal Care", "Expense", "Spa"),
    (10, "Gifts", "Expense", "FavoriteBorder"),
    (11, "Salary", "Income", "Savings"),
]
CAT = {name: cid for cid, name, _t, _i in CATEGORIES}

MAIN, US_SAV, US_CARD = 1, 2, 3

rng = random.Random(20260908)

# ------------------------------------------------------------------ helpers --
def cents(money: float) -> int:
    return int(round(money * 100))


def to_ms(d: dt.date, hour: int, minute: int = 0) -> int:
    local = dt.datetime(d.year, d.month, d.day, hour, minute, tzinfo=ZONE)
    return int(local.timestamp() * 1000)


def amount(low: float, high: float) -> int:
    v = rng.uniform(low, high)
    if rng.random() < 0.35:
        v = round(v)  # whole-pound purchases
    elif rng.random() < 0.5:
        v = round(v / 5) * 5  # .00/.05 endings
    return cents(v)


def chance(p: float) -> bool:
    return rng.random() < p


entries = []


def add(kind, cat, account, amt, desc, d, hour, minute=0, to_account=None, to_amt=None):
    if d > TODAY:
        return
    assert amt >= 0
    entries.append({
        "type": kind,
        "category_id": CAT.get(cat) if cat else None,
        "description": desc,
        "account_id": account,
        "amount": amt,
        "to_account_id": to_account,
        "to_amount": to_amt,
        "created_at": to_ms(d, hour, minute),
    })


def add_expense(cat, account, amt, desc, d, hour=None):
    if hour is None:
        hour = rng.randint(8, 20)
    add("Expense", cat, account, amt, desc, d, hour, rng.randint(0, 59))


def add_income(cat, account, amt, desc, d, hour=9):
    add("Income", cat, account, amt, desc, d, hour, 15)


def add_transfer(src, dst, amt, desc, d, to_amt=None, hour=10):
    add("Transfer", None, src, amt, desc, d, hour, 0, to_account=dst, to_amt=to_amt)


# ------------------------------------------------------------- recurring GBP --
def month_rows(y: int, m: int, max_day: int):
    """Routine month for the GBP side; only samples days in 1..max_day."""
    if max_day < 1:
        return
    d = lambda day: dt.date(y, m, day)  # noqa: E731

    # Salary on the 25th, saved into USD on the 26th.
    if 25 <= max_day:
        add_income("Salary", MAIN, cents(4_200.00), "Salary", d(25), hour=7)
    if 26 <= max_day:
        add_transfer(MAIN, US_SAV, cents(750.00), "Save in USD", d(26),
                     to_amt=cents(round(750.00 * FX)), hour=11)

    # Rent on the 1st.
    if 1 <= max_day:
        add_expense("Housing", MAIN, cents(1_850.00), "Rent", d(1), hour=8)

    # Bills across the month.
    bills = [
        ("Council Tax", cents(172.00), 1),
        ("Energy", cents(round(rng.uniform(90, 130))), 6),
        ("Broadband", cents(37.00), 8),
        ("Mobile plan", cents(19.00), 12),
        ("Water", cents(35.00), 20),
    ]
    for desc, amt, day in bills:
        if day <= max_day:
            add_expense("Bills & Fees", MAIN, amt, desc, d(day), hour=8)

    # Streaming subscriptions.
    for desc, amt, day in [("Video streaming", cents(10.99), 4),
                           ("Music streaming", cents(11.99), 9),
                           ("Cloud storage", cents(2.99), 15)]:
        if day <= max_day and (day != 15 or chance(0.5)):
            add_expense("Entertainment", MAIN, amt, desc, d(day), hour=6)

    # Groceries, ~5 shops a month.
    grocers = ["Tesco", "Sainsbury's", "Waitrose", "Aldi", "Lidl", "M&S Food", "Co-op"]
    for _ in range(rng.randint(4, 6)):
        add_expense("Groceries", MAIN, amount(15, 95), rng.choice(grocers),
                    d(rng.randint(1, max_day)))

    # Dining: lunches, coffee, pub dinners.
    dining = [
        ("Lunch out", amount(9, 18)), ("Coffee & pastry", amount(3.5, 7)),
        ("Pub dinner", amount(18, 45)), ("Sunday roast", amount(15, 30)),
        ("Takeaway", amount(10, 28)), ("Brunch", amount(12, 26)),
        ("Sushi lunch", amount(10, 24)), ("After-work drinks", amount(10, 30)),
        ("Bakery", amount(3, 8)), ("Pizza night", amount(14, 32)),
    ]
    for _ in range(rng.randint(7, 9)):
        desc, amt = rng.choice(dining)
        add_expense("Dining", MAIN, amt, desc, d(rng.randint(1, max_day)))

    # Shopping, occasionally.
    if chance(0.85):
        for _ in range(rng.randint(1, 2)):
            add_expense("Shopping", MAIN, amount(30, 220),
                        rng.choice(["Online order", "Clothing", "Electronics", "Books",
                                    "Sports gear", "Homeware"]),
                        d(rng.randint(1, max_day)))

    # Entertainment outings.
    if chance(0.6):
        add_expense("Entertainment", MAIN, amount(15, 120),
                    rng.choice(["Cinema", "Concert tickets", "Theatre", "Gallery"]),
                    d(rng.randint(1, max_day)))

    # Health / pharmacy.
    if chance(0.35):
        add_expense("Health", MAIN, amount(8, 60),
                    rng.choice(["Pharmacy", "Dentist", "Optician"]),
                    d(rng.randint(1, max_day)))

    # Personal care.
    if chance(0.45):
        add_expense("Personal Care", MAIN, amount(15, 90),
                    rng.choice(["Haircut", "Barber", "Grooming"]),
                    d(rng.randint(1, max_day)))

    # Gifts.
    if chance(0.3):
        add_expense("Gifts", MAIN, amount(25, 150),
                    rng.choice(["Birthday gift", "Wedding gift", "Present"]),
                    d(rng.randint(1, max_day)))

    # Occasional domestic rail under Travel.
    if chance(0.35):
        add_expense("Travel", MAIN, amount(18, 75),
                    rng.choice(["Train to Manchester", "Train to Brighton",
                                "Train to Oxford", "National Rail"]),
                    d(rng.randint(1, max_day)))


# -------------------------------------------------------------- US trips ----
TRIPS = [
    # (start, end, flight_gbp, flight_desc)
    (dt.date(2025, 8, 18), dt.date(2025, 8, 28), 460, "Flights London to New York"),
    (dt.date(2025, 10, 9), dt.date(2025, 10, 19), 520, "Flights London to San Francisco"),
    (dt.date(2026, 3, 5), dt.date(2026, 3, 13), 430, "Flights London to Miami"),
    (dt.date(2026, 7, 30), dt.date(2026, 8, 10), 470, "Flights London to Boston"),
]

us_food = ["Street food", "Diner", "Steakhouse", "Café", "Brunch place", "Sushi bar",
           "Pizzeria", "Food truck"]
us_fun = ["Museum tickets", "Broadway show", "Concert", "Ballpark", "Aquarium"]
us_shop = ["Souvenirs", "Outlet mall", "Clothing", "Electronics store", "Bookstore"]
us_rides = ["Uber", "Metro card", "Taxi"]


def us_trip(start: dt.date, end: dt.date, flight_gbp: float, flight_desc: str):
    # Flight booked on the GBP account beforehand.
    add_expense("Travel", MAIN, cents(flight_gbp), flight_desc,
                start - dt.timedelta(days=45), hour=19)

    city = flight_desc.split(" to ")[-1]
    days = (end - start).days + 1
    hotel_nights = max(days - 2, 1)
    hotel_amt = cents(hotel_nights * round(rng.uniform(160, 230), 0))
    add_expense("Travel", US_CARD, hotel_amt, f"Hotel — {city}", start + dt.timedelta(days=1), hour=15)

    card_total = hotel_amt
    d = start
    while d <= end:
        if chance(0.75):
            amt = amount(6, 45)
            add_expense("Dining", US_CARD, amt, rng.choice(us_food), d)
            card_total += amt
        if chance(0.9):
            amt = amount(18, 95)
            add_expense("Dining", US_CARD, amt, rng.choice(us_food), d)
            card_total += amt
        if chance(0.35):
            amt = amount(20, 160)
            add_expense(rng.choice(["Entertainment", "Shopping", "Travel"]), US_CARD, amt,
                        rng.choice(us_fun + us_shop + us_rides), d)
            card_total += amt
        if chance(0.4):
            amt = amount(8, 45)
            add_expense("Groceries", US_CARD, amt, rng.choice(["Grocery store", "Corner deli"]), d)
            card_total += amt
        if chance(0.25):
            amt = amount(10, 45)
            add_expense("Travel", US_CARD, amt, rng.choice(us_rides), d)
            card_total += amt
        d += dt.timedelta(days=1)

    # The card is paid off from the USD pot shortly after the trip.
    add_transfer(US_SAV, US_CARD, card_total, "Card payment", end + dt.timedelta(days=3), hour=9)


# ---------------------------------------------------------- year of routine --
def build():
    # From July 2025 up to and including the current month (partial).
    year, month = 2025, 7
    while (year, month) <= (TODAY.year, TODAY.month):
        if (year, month) == (TODAY.year, TODAY.month):
            max_day = TODAY.day - 1  # routine rows stop the day before today
        else:
            max_day = 28
        month_rows(year, month, max_day)
        if month == 12:
            year += 1
            month = 1
        else:
            month += 1

    # One-offs the routine misses.
    add_income("Salary", MAIN, cents(5_000.00), "Annual bonus", dt.date(2025, 12, 15), hour=9)
    add_transfer(MAIN, US_SAV, cents(3_000.00), "Save bonus in USD", dt.date(2025, 12, 18),
                 to_amt=cents(round(3_000.00 * FX)), hour=11)
    add_income("Salary", MAIN, cents(1_120.00), "Tax refund", dt.date(2026, 2, 2), hour=10)

    add_expense("Travel", MAIN, cents(210.00), "Weekend — Edinburgh", dt.date(2025, 11, 7), hour=8)
    add_expense("Travel", MAIN, cents(340.00), "City break — Lisbon", dt.date(2026, 4, 10), hour=9)

    for (y, m, desc) in [(2025, 12, "Christmas presents"), (2026, 5, "Birthday gift"),
                         (2026, 6, "Wedding gift")]:
        day = rng.randint(1, 24 if m == 12 else 28)
        add_expense("Gifts", MAIN, amount(50, 250), desc, dt.date(y, m, day))

    for start, end, f_gbp, f_desc in TRIPS:
        us_trip(start, end, f_gbp, f_desc)

    # A couple of reverse conversions when the dollar strengthens.
    add_transfer(US_SAV, MAIN, cents(1_200.00), "Currency conversion", dt.date(2026, 3, 20),
                 to_amt=cents(round(1_200.00 / FX)), hour=10)
    add_transfer(US_SAV, MAIN, cents(900.00), "Currency conversion", dt.date(2026, 9, 3),
                 to_amt=cents(round(900.00 / FX)), hour=10)

    # A small pending charge on the US card so it never sits at exactly zero.
    add_expense("Entertainment", US_CARD, cents(11.99), "US streaming", dt.date(2026, 9, 6), hour=9)

    # Deliberate, tidy entries dated today so the quick-add prefill and the
    # current-month ledger look intentional. Timestamps are minutes ago, so these
    # are always the newest rows and the coffee one seeds the Expense form.
    def recent(minutes_ago: int) -> int:
        return int((NOW - dt.timedelta(minutes=minutes_ago)).timestamp() * 1000)

    entries.append({
        "type": "Expense", "category_id": CAT["Groceries"], "description": "Tesco",
        "account_id": MAIN, "amount": cents(48.72), "to_account_id": None,
        "to_amount": None, "created_at": recent(160),
    })
    entries.append({
        "type": "Expense", "category_id": CAT["Dining"], "description": "Coffee & croissant",
        "account_id": MAIN, "amount": cents(4.10), "to_account_id": None,
        "to_amount": None, "created_at": recent(45),
    })


# ------------------------------------------------------------------- insert --
def main():
    build()
    # Safety net: never store anything after "now".
    now_ms = int(NOW.timestamp() * 1000)
    entries[:] = [e for e in entries if e["created_at"] <= now_ms]
    entries.sort(key=lambda e: e["created_at"])

    con = sqlite3.connect(DB_PATH)
    cur = con.cursor()
    cur.execute("DELETE FROM EntryEntity")
    cur.execute("DELETE FROM AccountEntity")
    cur.execute("DELETE FROM CategoryEntity")
    cur.execute("DELETE FROM BudgetEntity")
    cur.execute("DELETE FROM FxRateEntity")
    cur.execute("DELETE FROM AppSettingsEntity")
    cur.execute("DELETE FROM sqlite_sequence")

    for cid, name, ctype, icon in CATEGORIES:
        cur.execute("INSERT INTO CategoryEntity (id, name, type, icon) VALUES (?, ?, ?, ?)",
                    (cid, name, ctype, icon))
    for aid, name, currency, initial in ACCOUNTS:
        cur.execute("INSERT INTO AccountEntity (id, name, currency, initialBalance, hidden) "
                    "VALUES (?, ?, ?, ?, 0)", (aid, name, currency, initial))

    for e in entries:
        cur.execute(
            "INSERT INTO EntryEntity (type, category_id, description, account_id, amount, "
            "to_account_id, to_amount, created_at, created_zone) "
            "VALUES (:type, :category_id, :description, :account_id, :amount, "
            ":to_account_id, :to_amount, :created_at, 'Europe/London')", e)

    # Settings: GBP default, English, totals visible.
    for key, value in [("currency", "GBP"), ("language", "en"),
                       ("hideLedgerTotal", "false"), ("hideEntryTotal", "false")]:
        cur.execute("INSERT OR REPLACE INTO AppSettingsEntity (key, value) VALUES (?, ?)",
                    (key, value))

    # FX rates: GBP base (the default currency). Fresh fetchedAt so startup does
    # not try to refresh and risk a warning banner mid-screenshot.
    fx_date = TODAY.isoformat()
    gbp_rates = [("CHF", 1.0924), ("EUR", 1.1666), ("HKD", 10.6482),
                 ("JPY", 216.89), ("SGD", 1.7268), ("USD", 1.3583)]
    for quote, rate in gbp_rates:
        cur.execute(
            "INSERT OR REPLACE INTO FxRateEntity (baseCurrency, quoteCurrency, rate, date, fetchedAt) "
            "VALUES ('GBP', ?, ?, ?, ?)", (quote, rate, fx_date, now_ms))

    con.commit()
    con.close()
    print(f"Seeded {len(entries)} entries; max date {TODAY.isoformat()}.")


if __name__ == "__main__":
    main()
