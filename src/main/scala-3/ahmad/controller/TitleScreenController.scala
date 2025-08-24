package ahmad.controller

import ahmad.GameFactory
import ahmad.model.*
import ahmad.service.SaveLoadService
import javafx.event.ActionEvent
import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.control.Alert
import javafx.scene.layout.BorderPane
import javafx.scene.{Node, Parent}
import javafx.stage.{FileChooser, Stage}

import scala.util.{Failure, Success}

/**
 * Controller for the Title Screen.
 *
 * Responsibilities:
 *  - Handle the three title actions: "New Game", "Load Game", and "Quit".
 *  - Create or restore a [[GameController]] instance and transition the UI from the
 *    title screen to the main application layout (RootLayout.fxml).
 *  - Wire sub-controllers (FarmGrid, Families, Market, Upgrades) with the shared
 *    [[GameController]] so they all operate on the same game state.
 *
 * Notes on construction:
 *  - This class accepts a `makeGame: () => GameController` factory for improved testability
 *    and DI flexibility. The current implementation invokes [[GameFactory.newGame()]] directly
 *    in [[onNewGame]], but you can switch to the injected `makeGame` with minimal changes
 *    if desired (see the comment inside [[onNewGame]]).
 */
final class TitleScreenController(private val makeGame: () => GameController) {

  /** Root node of the title screen (injected by FXML). May be null until FXML loads. */
  @FXML private var root: BorderPane = _ // May be null

  /**
   * Starts a brand-new game and opens the main application layout.
   *
   * @param evt The action event fired by the "New Game" button.
   */
  @FXML private def onNewGame(evt: ActionEvent): Unit =
    openRootWith(GameFactory.newGame(), evt)

  /**
   * Prompts the user to choose a JSON save file and attempts to load it.
   * On success, transitions into the main UI with the restored game state.
   * On failure, shows an error dialog and stays on the title screen.
   *
   * @param evt The action event fired by the "Load Game" button.
   */
  @FXML private def onLoadGame(evt: ActionEvent): Unit = {
    val stage = nodeStage(evt.getSource)

    // Configure a file chooser that filters to JSON saves only.
    val fc = new FileChooser()
    fc.setTitle("Load Game")
    fc.getExtensionFilters.add(new FileChooser.ExtensionFilter("JSON Save (*.json)", "*.json"))

    // Show the OS-native open dialog; user may cancel (null result).
    val f = fc.showOpenDialog(stage)
    if (f == null) return

    // Delegate deserialization to the SaveLoadService; it returns a Try[(GameState, Market)].
    SaveLoadService.load(f.toPath) match {
      case Success((gs, market)) =>
        // Rehydrate GameController with loaded state and enter the main UI.
        openRootWith(new GameController(gs, market), evt)

      case Failure(ex) =>
        // Surface a user-friendly error message while preserving the exception detail.
        val a = new Alert(Alert.AlertType.ERROR)
        a.setHeaderText("Load failed")
        a.setContentText(Option(ex.getMessage).getOrElse(ex.toString))
        a.showAndWait()
    }
  }

  /** Quits the application process immediately. */
  @FXML private def onQuit(): Unit = System.exit(0)

  /**
   * Loads the main application layout (RootLayout.fxml), supplying a controller factory
   * that injects the shared [[GameController]] into feature panes. It then replaces the
   * current Scene's root with the newly loaded layout, or creates a Scene if needed.
   *
   * @param game A fully-initialized [[GameController]] instance to share across sub-controllers.
   * @param evt  The UI event whose source is used to resolve the current Stage.
   */
  private def openRootWith(game: GameController, evt: ActionEvent): Unit = {
    // Prepare an FXMLLoader for the main application layout.
    val loader = new FXMLLoader(getClass.getResource("/ahmad/view/RootLayout.fxml"))

    // Controller factory that DI-injects the shared `game` into known controllers.
    // Unknown controllers fall back to a zero-arg constructor.
    loader.setControllerFactory { clazz =>
      if      (clazz == classOf[RootLayoutController])   new RootLayoutController(game)
      else if (clazz == classOf[FarmGridController])     new FarmGridController(game)
      else if (clazz == classOf[FamiliesPaneController]) new FamiliesPaneController(game)
      else if (clazz == classOf[MarketPaneController])   new MarketPaneController(game)
      else if (clazz == classOf[UpgradesPaneController]) new UpgradesPaneController(game)
      else clazz.getDeclaredConstructor().newInstance()
    }

    // Load the FXML, producing the new root node for the main UI.
    val newRoot: Parent = loader.load()

    // Swap the current Scene's root (if present) for a seamless transition; otherwise
    // create a fresh Scene with a sensible default size.
    val stage = nodeStage(evt.getSource)
    if (stage.getScene == null) stage.setScene(new javafx.scene.Scene(newRoot, 1200, 720))
    else stage.getScene.setRoot(newRoot)
  }

  /**
   * Utility: given any event source coming from a JavaFX Node, resolve the owning Stage.
   *
   * @param src An event source expected to be a JavaFX Node.
   * @return The Stage that contains the Node's Scene.
   */
  private def nodeStage(src: Any): Stage =
    src.asInstanceOf[Node].getScene.getWindow.asInstanceOf[Stage]
}
