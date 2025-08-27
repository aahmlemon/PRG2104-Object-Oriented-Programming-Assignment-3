package ahmad.controller

import ahmad.GameFactory
import ahmad.model.*
import ahmad.service.SaveLoadService
import ahmad.util.*
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control.{Button, Label}
import javafx.scene.layout.{BorderPane, Pane}
import javafx.stage.FileChooser
import scalafx.scene.control.Alert

import java.io.File
import scala.util.{Failure, Success}

/** Root JavaFX controller that owns the top-level layout and screen navigation.
  *
  * Responsibilities:
  * - Displays header info (day, money, inventory quantities).
  * - Hosts the main center content (Farm, Families, Market, Upgrades) and swaps it on demand.
  * - Wires cross-pane callbacks (inventory/money changes) to keep the header in sync.
  * - Handles game lifecycle actions: save, load, end-day, return to title.
  *
  * Usage notes:
  * - initialize() sets up the initial header values and opens the Farm screen.
  * - loadCenter() is the common loader used by navigation actions to replace the center pane.
  */
final class RootLayoutController(private val game: GameController) {

  @FXML private var root: BorderPane = _   // Root container whose center is swapped between feature panes
  @FXML private var dayLabel: Label = _    // Displays current day number
  @FXML private var moneyLabel: Label = _  // Displays current money
  @FXML private var btnEndDay: Button = _  // Button to advance to the next day
  @FXML private var riceQty: Label = _     // Inventory badge for Rice
  @FXML private var beansQty: Label = _    // Inventory badge for Beans
  @FXML private var vegsQty: Label = _     // Inventory badge for Vegetables

  /** Holds the controller instance of the currently displayed center pane, if any.
    * Used to call Refreshable.refresh() after end-day and to attach callbacks.
    */
  private var currentCenterController: AnyRef = _

  /** JavaFX initialization hook.
    * - Renders header info from the current game.
    * - Opens the default center content (Farm view).
    */
  @FXML private def initialize(): Unit = {
    refreshHeader()
    refreshInventoryLabels()
    openFarm()
  }

  /** Opens the Farm screen in the center pane.
    * Provides the GameController to the FarmGridController via controller factory.
    * Also attaches inventory/money change callbacks when the controller supports them.
    */
  @FXML private def openFarm(): Unit = {
    val loader = new FXMLLoader(getClass.getResource("/ahmad/view/FarmGrid.fxml"))
    loader.setControllerFactory { clazz =>
      if clazz == classOf[FarmGridController] then new FarmGridController(game)
      else clazz.getDeclaredConstructor().newInstance()
    }
    val pane: Pane = loader.load()
    val ctrl = loader.getController[AnyRef]
    currentCenterController = ctrl
    root.setCenter(pane)

    // Propagate notifications from the child controller back to the header
    ctrl match
      case inv: InventoryAware => inv.setOnInventoryChanged(() => refreshHeader())
      case cash: MoneyAware    => cash.setOnMoneyChanged(() => refreshMoneyLabel())
      case _ => ()
  }

  /** Opens the Families screen in the center pane. */
  @FXML private def openFamilies(): Unit =
    loadCenter("/ahmad/view/FamiliesPane.fxml")

  /** Opens the Market screen in the center pane. */
  @FXML private def openMarket(): Unit =
    loadCenter("/ahmad/view/MarketPane.fxml")

  /** Opens the Upgrades screen in the center pane. */
  @FXML private def openUpgrades(): Unit =
    loadCenter("/ahmad/view/UpgradesPane.fxml")

  /** Navigates back to the title screen after confirmation. */
  @FXML private def onBackToTitle(): Unit = {
    val a = new scalafx.scene.control.Alert(scalafx.scene.control.Alert.AlertType.Confirmation)
    a.headerText = "Return to Title?"
    a.contentText = "Unsaved progress will be lost."
    val res = a.showAndWait()
    if (res.isDefined && res.get == scalafx.scene.control.ButtonType.OK)
      showTitleScreen()
  }

  /** Prompts for a file and saves the current game state and market.
    * Shows a success or error dialog based on the SaveLoadService result.
    */
  @FXML private def onSave(): Unit = {
    val fc = new FileChooser()
    fc.setTitle("Save Game")
    fc.getExtensionFilters.add(new FileChooser.ExtensionFilter("JSON Save (*.json)", "*.json"))
    val f: File = fc.showSaveDialog(root.getScene.getWindow)
    if (f != null) {
      SaveLoadService.save(f.toPath, game.gs, game.market) match
        case Success(_) =>
          val a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
          a.setHeaderText("Saved")
          a.setContentText(s"Saved to ${f.getName}")
          a.showAndWait()
        case Failure(ex) =>
          val a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR)
          a.setHeaderText("Save failed")
          a.setContentText(ex.getMessage)
          a.showAndWait()
    }
  }

  /** Prompts for a file and loads a saved game state and market.
    * On success, replaces the in-memory state and refreshes the header.
    */
  @FXML private def onLoad(): Unit = {
    val fc = new FileChooser()
    fc.setTitle("Load Game")
    fc.getExtensionFilters.add(new FileChooser.ExtensionFilter("JSON Save (*.json)", "*.json"))
    val f: File = fc.showOpenDialog(root.getScene.getWindow)
    if (f != null) {
      SaveLoadService.load(f.toPath) match
        case Success((gs2, market2)) =>
          game.replaceState(gs2, market2)
          currentCenterController match
            case r: Refreshable => r.refresh()
            case _ => ()
          refreshHeader()
        case Failure(ex) =>
          val a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR)
          a.setHeaderText("Load failed")
          a.setContentText(ex.getMessage)
          a.showAndWait()
    }
  }

  /** Advances the game by one day or shows Game Over if the transition fails.
    * When the day advances successfully, refreshes the header and asks the currently
    * displayed center controller to re-render if it implements Refreshable.
    */
  @FXML private def endDay(): Unit = {
    game.endDay() match
      case Left(msg) =>
        // Compute a simple score from current day and money to display on Game Over
        val days = game.gs.day
        val money = game.gs.money
        val score = days.toLong * money.toLong

        def fmt(n: Long) = f"$n%,d"

        val a = new scalafx.scene.control.Alert(scalafx.scene.control.Alert.AlertType.Error)
        a.headerText = "Game Over"
        a.contentText =
          s"""$msg

             |Score: ${fmt(days)} days × $$${fmt(money)} = ${fmt(score)}
             |""".stripMargin
        a.showAndWait()

        showTitleScreen()

      case Right(_) =>
        // Successful day end: update header and let the center screen refresh itself
        refreshHeader()
        currentCenterController match
          case r: Refreshable => r.refresh()
          case _ => ()
  }

  /** Refreshes the whole header region (day, money, inventory quantities). */
  private def refreshHeader(): Unit = {
    dayLabel.setText(s"Day ${game.gs.day}")
    moneyLabel.setText(f"$$${game.gs.money}%,d")
    refreshInventoryLabels()
  }

  /** Renders the three inventory counters based on current storage. */
  private def refreshInventoryLabels(): Unit = {
    def amt(p: Produce): Int = game.storage.amountOf(p) // helper for brevity

    riceQty.setText(amt(Rice()).toString)
    beansQty.setText(amt(Beans()).toString)
    vegsQty.setText(amt(Vegetables()).toString)
  }

  /** Updates only the money label. Useful when a child pane reports money changes. */
  private def refreshMoneyLabel(): Unit =
    moneyLabel.setText(f"$$${game.gs.money}%,d")

  /** Loads an FXML as the center pane and wires controller callbacks.
    * Uses a controller factory to inject the shared GameController into child panes.
    *
    * @param resource classpath to the FXML file for the center content
    */
  private def loadCenter(resource: String): Unit = {
    val loader = new FXMLLoader(getClass.getResource(resource))
    loader.setControllerFactory { clazz =>
      if      clazz == classOf[FamiliesPaneController] then new FamiliesPaneController(game)
      else if clazz == classOf[MarketPaneController]   then new MarketPaneController(game)
      else if clazz == classOf[UpgradesPaneController] then new UpgradesPaneController(game)
      else if clazz == classOf[FarmGridController]     then new FarmGridController(game)
      else clazz.getDeclaredConstructor().newInstance()
    }
    val pane: Pane = loader.load()
    val ctrl = loader.getController[AnyRef]
    currentCenterController = ctrl
    root.setCenter(pane)
    
    // Attach header-refresh hooks when the child exposes inventory/money signals
    ctrl match
      case inv: InventoryAware => inv.setOnInventoryChanged(() => refreshHeader())
      case cash: MoneyAware    => cash.setOnMoneyChanged(() => refreshMoneyLabel())
      case _ => ()
  }

  /** Replaces the current Scene root with the Title screen.
    * Creates the title controller and supplies a fresh game factory callback.
    */
  private def showTitleScreen(): Unit = {
    val loader = new FXMLLoader(getClass.getResource("/ahmad/view/TitleScreen.fxml"))
    loader.setControllerFactory { clazz =>
      if (clazz == classOf[TitleScreenController]) new TitleScreenController(() => GameFactory.newGame())
      else clazz.getDeclaredConstructor().newInstance()
    }
    val root = loader.load[javafx.scene.Parent]()
    val stage = dayLabel.getScene.getWindow.asInstanceOf[javafx.stage.Stage]
    stage.getScene.setRoot(root)
  }
}
