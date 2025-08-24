package ahmad.controller

import ahmad.service.*
import ahmad.util.{MoneyAware, Refreshable}
import javafx.fxml.FXML
import javafx.scene.control.{Alert, Button, ListView}

final class UpgradesPaneController(private val game: GameController) extends Refreshable, MoneyAware {

  @FXML private var upgradesList: ListView[String] = _
  @FXML private var btnPurchase: Button = _

  private val items = new java.util.ArrayList[String]()

  private var onMoneyChanged: () => Unit = () => ()
  override def setOnMoneyChanged(cb: () => Unit): Unit = onMoneyChanged = cb


  @FXML private def initialize(): Unit = {
    refresh()
    btnPurchase.setOnAction(_ => onPurchase())
  }

  override def refresh(): Unit = {
    items.clear()
    items.add(render(Irrigation))
    items.add(render(YieldBoost))
    upgradesList.getItems.setAll(items)
  }

  private def render(up: Upgrade): String = {
    val lvl = UpgradeService.currentLevel(game.gs.farm, up)
    UpgradeService.nextCost(game.gs.farm, up) match {
      case Some(UpgradeCost(nextLvl, price)) =>
        s"${up.name}  —  Level: $lvl → $nextLvl  |  Cost: $$${price}"
      case None =>
        s"${up.name}  —  Level: $lvl (MAX)"
    }
  }

  private def onPurchase(): Unit = {
    val idx = upgradesList.getSelectionModel.getSelectedIndex
    if (idx < 0) { info("Select an upgrade to purchase."); return }

    val up = if (idx == 0) Irrigation else YieldBoost
    game.buyUpgrade(up) match {
      case Left(msg)  => error(msg)
      case Right(())  =>
        info("Upgrade purchased!")
        refresh()
        onMoneyChanged()
    }
  }

  private def info(msg: String): Unit = {
    val a = new Alert(Alert.AlertType.INFORMATION); a.setHeaderText(null); a.setContentText(msg); a.showAndWait()
  }
  private def error(msg: String): Unit = {
    val a = new Alert(Alert.AlertType.ERROR); a.setHeaderText("Cannot purchase"); a.setContentText(msg); a.showAndWait()
  }
}
