package ahmad

import ahmad.controller.*
import ahmad.model.*
import ahmad.service.*
import javafx.fxml.FXMLLoader
import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.scene.Scene

/**
 * Entry point for the Farm & Family application.
 *
 * This object extends [[scalafx.application.JFXApp3]], which integrates
 * with the JavaFX application lifecycle. It is responsible for:
 *  - Creating an initial [[GameController]] with a shared game state.
 *  - Loading the Title Screen UI (FXML).
 *  - Wiring in controllers with proper factories (notably TitleScreenController).
 *  - Initializing and showing the primary application stage.
 *
 * The UI starts on `TitleScreen.fxml`, and transitions to `RootLayout.fxml`
 * when the user chooses New Game or Load Game.
 */
object MainApp extends JFXApp3 {

  /**
   * Shared initial game setup, used to bootstrap the application.
   *
   * Families are defined with their nutritional needs and stockpiles
   * (initialized to 3 days of food). The farm, crop grid, storage, day,
   * and money are all initialized to default values. A starting market
   * is also created with rice, beans, and vegetables.
   */
  private val sharedGame = {
    // Construct initial GameState with families, farm, crop grid, etc.
    val gs = new GameState(
      families = Vector(
        new Family("Ali",  Nutrition(1800,50,250,60), stockpile = Nutrition(1800,50,250,60) * 3),
        new Family("Bala", Nutrition(2000,60,300,70), stockpile = Nutrition(2000,60,300,70) * 3),
        new Family("Chen", Nutrition(1700,45,230,55), stockpile = Nutrition(1700,45,230,55) * 3),
        new Family("Devi", Nutrition(1900,55,270,65), stockpile = Nutrition(1900,55,270,65) * 3)
      ),
      farm = new Farm(),
      grid = Vector.fill(12)(new CropTile()), // 12 empty crop tiles
      storage = Storage(),
      day = 1,
      money = 0
    )
    // Initialize the produce market with three staple items
    val market = Market.initial(List(Rice(), Beans(), Vegetables()))

    // Wrap state + market in a GameController
    new GameController(gs, market)
  }

  /**
   * Start method is invoked by the JavaFX runtime when the application launches.
   * It loads the Title Screen FXML and sets up the primary stage.
   */
  override def start(): Unit = {
    // Prepare FXML loader for Title Screen
    val loader = new FXMLLoader(getClass.getResource("/ahmad/view/TitleScreen.fxml"))

    // Controller factory ensures TitleScreenController gets constructed with
    // a GameFactory reference, while all others use default constructors
    loader.setControllerFactory { clazz =>
      if (clazz == classOf[TitleScreenController])
        new TitleScreenController(() => GameFactory.newGame())
      else clazz.getDeclaredConstructor().newInstance()
    }

    // Load the JavaFX root node from FXML
    val jfxRoot: javafx.scene.Parent = loader.load()
    // Convert it into a ScalaFX wrapper for seamless use in ScalaFX APIs
    val sfxRoot: scalafx.scene.Parent = jfxRoot

    // Configure and show the main application window
    stage = new JFXApp3.PrimaryStage {
      title = "Farm & Family"
      scene = new Scene(sfxRoot, 1200, 720) // fixed scene size
      resizable = false
    }

    // Attach application-wide stylesheet
    stage.scene().stylesheets += getClass.getResource("/css/app.css").toExternalForm
  }
}
