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

final class TitleScreenController(private val makeGame: () => GameController) {

  @FXML private var root: BorderPane = _ // may be null; we won't rely on it

  @FXML private def onNewGame(evt: ActionEvent): Unit =
    openRootWith(GameFactory.newGame(), evt)

  @FXML private def onLoadGame(evt: ActionEvent): Unit = {
    val stage = nodeStage(evt.getSource)

    val fc = new FileChooser()
    fc.setTitle("Load Game")
    fc.getExtensionFilters.add(new FileChooser.ExtensionFilter("JSON Save (*.json)", "*.json"))
    val f = fc.showOpenDialog(stage)
    if (f == null) return

    SaveLoadService.load(f.toPath) match {
      case Success((gs, market)) =>
        openRootWith(new GameController(gs, market), evt)
      case Failure(ex) =>
        val a = new Alert(Alert.AlertType.ERROR)
        a.setHeaderText("Load failed")
        a.setContentText(Option(ex.getMessage).getOrElse(ex.toString))
        a.showAndWait()
    }
  }

  @FXML private def onQuit(): Unit = System.exit(0)

  private def openRootWith(game: GameController, evt: ActionEvent): Unit = {
    val loader = new FXMLLoader(getClass.getResource("/ahmad/view/RootLayout.fxml"))
    loader.setControllerFactory { clazz =>
      if      (clazz == classOf[RootLayoutController])   new RootLayoutController(game)
      else if (clazz == classOf[FarmGridController])     new FarmGridController(game)
      else if (clazz == classOf[FamiliesPaneController]) new FamiliesPaneController(game)
      else if (clazz == classOf[MarketPaneController])   new MarketPaneController(game)
      else if (clazz == classOf[UpgradesPaneController]) new UpgradesPaneController(game)
      else clazz.getDeclaredConstructor().newInstance()
    }

    val newRoot: Parent = loader.load()
    val stage = nodeStage(evt.getSource)
    if (stage.getScene == null) stage.setScene(new javafx.scene.Scene(newRoot, 1200, 720))
    else stage.getScene.setRoot(newRoot)
  }

  private def nodeStage(src: Any): Stage =
    src.asInstanceOf[Node].getScene.getWindow.asInstanceOf[Stage]
}
