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

final class RootLayoutController(private val game: GameController) {

  @FXML private var root: BorderPane = _
  @FXML private var dayLabel: Label = _
  @FXML private var moneyLabel: Label = _
  @FXML private var btnEndDay: Button = _
  @FXML private var riceQty: Label = _
  @FXML private var beansQty: Label = _
  @FXML private var vegsQty: Label = _

  private var currentCenterController: AnyRef = _

  @FXML private def initialize(): Unit = {
    refreshHeader()
    refreshInventoryLabels()
    openFarm()
  }

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

    ctrl match
      case inv: InventoryAware => inv.setOnInventoryChanged(() => refreshHeader())
      case cash: MoneyAware    => cash.setOnMoneyChanged(() => refreshMoneyLabel())
      case _ => ()
  }

  @FXML private def openFamilies(): Unit =
    loadCenter("/ahmad/view/FamiliesPane.fxml")

  @FXML private def openMarket(): Unit =
    loadCenter("/ahmad/view/MarketPane.fxml")

  @FXML private def openUpgrades(): Unit =
    loadCenter("/ahmad/view/UpgradesPane.fxml")

  @FXML private def onBackToTitle(): Unit = {
    val a = new scalafx.scene.control.Alert(scalafx.scene.control.Alert.AlertType.Confirmation)
    a.headerText = "Return to Title?"
    a.contentText = "Unsaved progress will be lost."
    val res = a.showAndWait()
    if (res.isDefined && res.get == scalafx.scene.control.ButtonType.OK)
      showTitleScreen()
  }

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

  @FXML private def onLoad(): Unit = {
    val fc = new FileChooser()
    fc.setTitle("Load Game")
    fc.getExtensionFilters.add(new FileChooser.ExtensionFilter("JSON Save (*.json)", "*.json"))
    val f: File = fc.showOpenDialog(root.getScene.getWindow)
    if (f != null) {
      SaveLoadService.load(f.toPath) match
        case Success((gs2, market2)) =>
          game.replaceState(gs2, market2) // add method below
          refreshHeader()
        // if a sub-pane is open, consider refreshing it here
        case Failure(ex) =>
          val a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR)
          a.setHeaderText("Load failed")
          a.setContentText(ex.getMessage)
          a.showAndWait()
    }
  }

  @FXML private def endDay(): Unit = {
    game.endDay() match
      case Left(msg) =>
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
        refreshHeader()
        currentCenterController match
          case r: Refreshable => r.refresh()
          case _ => ()
  }

  private def refreshHeader(): Unit = {
    dayLabel.setText(s"Day ${game.gs.day}")
    moneyLabel.setText(f"$$${game.gs.money}%,d")
    refreshInventoryLabels()
  }

  private def refreshInventoryLabels(): Unit = {
    def amt(p: Produce): Int = game.storage.amountOf(p) // add amountOf to Storage if you don't have it

    riceQty.setText(amt(Rice()).toString)
    beansQty.setText(amt(Beans()).toString)
    vegsQty.setText(amt(Vegetables()).toString)
  }

  private def refreshMoneyLabel(): Unit =
    moneyLabel.setText(f"$$${game.gs.money}%,d")

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

    // If this center is the farm grid, hook the inventory change callback
    ctrl match
      case inv: InventoryAware => inv.setOnInventoryChanged(() => refreshHeader())
      case cash: MoneyAware    => cash.setOnMoneyChanged(() => refreshMoneyLabel())
      case _ => ()
  }

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
