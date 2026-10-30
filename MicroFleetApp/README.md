# MicroFleet (standalone Android app)

Intelligent delivery sharing for small businesses, as described in the project description.
Orders from nearby shops (pharmacy, bakery, florist, boutique, ...) go into one common pool.
The app then runs the project's pipeline **on the phone**:

1. **Fuzzy logic** scores each order's urgency (deadline slack, waiting time, priority).
2. **Genetic algorithm** decides which orders should be pooled into one shared trip.
3. **Ant colony optimisation** finds the best pick-up / drop-off order inside each trip.
4. **Reinforcement learning** picks the rider (location, workload, capacity) or holds a trip back.

The goal is fewer trips and lower cost while still meeting deadlines and capacity, not just the shortest route.

## Screens
- **Merchant** – pick a shop, create delivery orders, follow status, cancel orders still in the pool.
- **Dispatcher** – riders on/off duty, urgency-sorted pool (tap an order to see the fuzzy reasoning),
  "Run intelligent dispatch", plan with map, per-trip route, ant-colony vs nearest-stop distance,
  rider probabilities from the RL policy, then "Send trips to riders".
- **Rider** – current trip on the map, next stop, "Mark picked up / delivered", queue.
- **Savings dashboard** – trips, km and estimated cost saved by pooling, on-time rate, dispatch history,
  RL policy vs simple rules (retrain button).

## Technical notes
- Pure Java, Android framework only (no AndroidX, no third-party libraries, no network, no permissions).
- Engines (`engine/`) are a port of the project's simulation (same road graph of 20 Mumbai areas,
  same fuzzy rules, GA / ACO / RL). Results on the classic 10-order example match the simulation.
- Data is stored on the device in SharedPreferences (`data/Store`).
- minSdk 24, targetSdk 37, AGP 9.4.1, Gradle 9.6.0 (same as the existing MicroFleet project).
- Cost savings use assumed rates (Rs 25 per trip + Rs 8 per km), shown as assumptions in the app.

## Run
Open the folder in Android Studio, let Gradle sync, press Run.
Home screen -> "Load classic demo" -> Dispatcher -> "Run intelligent dispatch".
