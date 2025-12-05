# Farm & Family Management Simulation (Scala 3 + JavaFX)

A strategic desktop simulation game where players manage a farm, cultivate crops, and ensure four families' nutritional needs are met. Built with Scala 3 and JavaFX, this project demonstrates object-oriented programming concepts including the Model-View-Controller (MVC) pattern, immutable data structures, and functional service layers.

## 🎮 Features

* **Farm Management:** Upgrade irrigation and yield levels to improve crop growth and harvest output.
* **Crop Grid System:** Manage a 12-tile grid where crops progress through growth stages.
* **Family Nutrition Tracking:** Monitor four families' daily nutritional requirements (calories, protein, carbohydrates, vitamins).
* **Dynamic Market System:** Buy and sell produce with prices that evolve over time.
* **Day-Based Progression:** Advance through in-game days with end-of-day validation to ensure family satisfaction.
* **Storage & Inventory:** Manage produce inventory across multiple crop types.
* **Save/Load System:** Persist game progress to disk using serialization.
* **Multi-View UI:** Navigate between Title Screen, Farm Grid, Families Pane, Market, and Upgrades panels.

## 📋 Prerequisites

To build and run this project, you need:

1.  **Java Development Kit (JDK)** 21 or higher
2.  **Scala Build Tool (SBT)** 1.9.0 or higher

## 📦 Dependencies

The project uses the following dependencies, managed through SBT:

* `javafx-base`, `javafx-controls`, `javafx-fxml`, `javafx-graphics`, `javafx-media`, `javafx-swing`, `javafx-web` (v21.0.4): Cross-platform GUI framework with FXML support for layout files.
* `scalafx` (v21.0.0-R32): Scala wrapper for JavaFX with type-safe bindings.
* `upickle` (v3.3.1): Serialization library for saving and loading game state.

All dependencies are automatically resolved by SBT during the build process.

## ⚙️ Setup & Installation

1.  **Clone the Repository:**
    ```powershell
    git clone https://github.com/aahmlemon/PRG2104-Object-Oriented-Programming-Assignment-3.git
    cd ScalaA3
    ```

2.  **Directory Structure:**
    The project follows a standard SBT layout:
    ```
    ScalaA3/
    ├── build.sbt                 # Build configuration
    ├── project/
    │   ├── build.properties      # SBT version
    │   └── plugins.sbt           # Build plugins
    ├── src/main/
    │   ├── resources/            # FXML files and CSS styling
    │   │   ├── ahmad/view/       # UI layout files (.fxml)
    │   │   ├── assets/           # Game assets and images
    │   │   └── css/              # Application stylesheet
    │   └── scala-3/
    │       └── ahmad/
    │           ├── MainApp.scala # Application entry point
    │           ├── GameFactory.scala # Game initialization
    │           ├── controller/   # MVC controllers for UI panels
    │           ├── model/        # Core game entities (Farm, Family, GameState, etc.)
    │           ├── service/      # Business logic (DayService, MarketService, etc.)
    │           └── util/         # Utility traits (Refreshable, MoneyAware, InventoryAware)
    ```

3.  **Important Notes:**
    - FXML files must be located in `src/main/resources/ahmad/view/` for the application to find them.
    - The CSS stylesheet (`app.css`) should be in `src/main/resources/css/`.
    - The project automatically detects your OS (Windows, Mac, Linux) and downloads the appropriate JavaFX binaries.

## 🚀 How to Run

You can run the game using SBT or compile it to an executable.

**Option 1: Run Directly with SBT**
```powershell
sbt run
```

**Option 2: Compile to JAR**
```powershell
sbt package
```

Then run the generated JAR file (note: JavaFX requires special module configuration when running JARs).

## 🕹️ Game Controls & Concepts

### Core Gameplay Loop
1. **Start Game:** Select "New Game" from the Title Screen.
2. **Farm Management:** Plant crops in empty tiles and wait for harvest.
3. **Market Trading:** Buy and sell produce to manage inventory.
4. **Family Feeding:** Distribute produce to families to meet their daily nutritional needs.
5. **End Day:** Advance to the next day; game ends if families are not satisfied.
6. **Upgrades:** Invest money to improve farm irrigation and yield levels.

### Key Panels

| Panel | Purpose |
| :---: | :--- |
| **Title Screen** | New Game / Load Game options |
| **Farm Grid** | Plant and manage crops on the 12-tile grid |
| **Families Pane** | Monitor each family's nutrition status and stockpile |
| **Market Pane** | Buy/sell produce with dynamic pricing |
| **Upgrades Pane** | Spend money to improve farm irrigation and yield |
| **Root Layout** | Main hub showing game state and day counter |

### Game Entities

| Entity | Description |
| :---: | :--- |
| **Family** | Represents a household with specific nutritional needs (4 families: Ali, Bala, Chen, Devi) |
| **Farm** | Tracks irrigation level and yield level upgrades |
| **CropTile** | Individual grid cell containing a crop at various growth stages |
| **Storage** | Inventory system managing quantities of different produce types |
| **Market** | Dynamic marketplace where produce prices fluctuate over time |
| **GameState** | Immutable state container holding families, farm, grid, storage, day counter, and money |

## 🧠 Technical Highlights

* **Immutable Data Structures:** `GameState` uses immutable records and update methods (`withFamilies()`, `withDay()`, etc.) to ensure consistent state transitions.
* **Model-View-Controller Pattern:** Separation of concerns with dedicated controller classes handling UI interactions for each view.
* **Service Layer:** Business logic encapsulated in service objects (`DayService`, `MarketService`, `UpgradeService`, `SaveLoadService`) that operate on `GameState`.
* **Functional Programming:** Heavy use of functional utilities like `map`, `filter`, and `fold` for data transformations.
* **FXML + ScalaFX:** Declarative UI layouts with type-safe Scala bindings for reactive updates.
* **Serialization:** `SaveLoadDTO` and `SaveLoadService` use `upickle` for JSON-based game persistence.

## 📁 Package Structure

* **`model/`:** Core game entities (immutable, represent game concepts).
* **`controller/`:** UI controllers binding FXML views to game logic.
* **`service/`:** Business logic services handling game mechanics and state transitions.
* **`util/`:** Utility traits (`Refreshable`, `MoneyAware`, `InventoryAware`) providing mixin functionality.

## 🎯 Learning Objectives

This project demonstrates:
- Object-oriented design with immutable objects and update patterns.
- MVC architecture for desktop GUI applications.
- Functional service composition and state management.
- JavaFX/FXML integration with Scala type safety.
- Game state validation and day-cycle progression logic.
