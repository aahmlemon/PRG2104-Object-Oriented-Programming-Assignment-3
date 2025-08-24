package ahmad.controller

import ahmad.service.*
import ahmad.util.{MoneyAware, Refreshable}
import javafx.fxml.FXML
import javafx.scene.control.{Alert, Button, ListView}

/**
 * Controller for the **Upgrades** tab/pane.
 *
 * Responsibilities:
 *   - Display available farm upgrades (Irrigation, YieldBoost).
 *   - Show the current upgrade level and the next cost (or "MAX" if capped).
 *   - Allow the player to purchase upgrades if they have enough money.
 *   - Notify the root layout when money changes (via [[MoneyAware]]).
 *
 * This controller is bound to an FXML file that defines:
 *   - `upgradesList`: a ListView listing the available upgrades and their current status.
 *   - `btnPurchase`: a button to purchase the currently selected upgrade.
 *
 * Integrates with:
 *   - [[UpgradeService]] to compute upgrade levels/costs.
 *   - [[GameController]] to actually apply upgrades and update game state.
 */
final class UpgradesPaneController(private val game: GameController)
  extends Refreshable, MoneyAware {

  // --- FXML-injected UI controls ---
  @FXML private var upgradesList: ListView[String] = _
  @FXML private var btnPurchase: Button            = _

  // Internal backing list for upgrades (rendered into the ListView)
  private val items = new java.util.ArrayList[String]()

  // Callback for when money changes, provided by the root layout
  private var onMoneyChanged: () => Unit = () => ()
  override def setOnMoneyChanged(cb: () => Unit): Unit = onMoneyChanged = cb

  /** Called automatically by JavaFX after FXML is loaded. */
  @FXML private def initialize(): Unit = {
    refresh()                       // Populate the list with upgrades
    btnPurchase.setOnAction(_ => onPurchase()) // Wire button to purchase action
  }

  /** Refresh the upgrades list, showing current levels and next costs. */
  override def refresh(): Unit = {
    items.clear()
    items.add(render(Irrigation))
    items.add(render(YieldBoost))
    upgradesList.getItems.setAll(items) // Push to the ListView
  }

  /**
   * Render a human-readable string for a given upgrade.
   *
   * Example:
   *   "Irrigation — Level: 1 → 2 | Cost: $100"
   *   or "Yield Boost — Level: 3 (MAX)"
   */
  private def render(up: Upgrade): String = {
    val lvl = UpgradeService.currentLevel(game.gs.farm, up)
    UpgradeService.nextCost(game.gs.farm, up) match {
      case Some(UpgradeCost(nextLvl, price)) =>
        s"${up.name}  —  Level: $lvl → $nextLvl  |  Cost: $$${price}"
      case None =>
        s"${up.name}  —  Level: $lvl (MAX)"
    }
  }

  /** Handle the purchase button click. */
  private def onPurchase(): Unit = {
    val idx = upgradesList.getSelectionModel.getSelectedIndex
    if (idx < 0) { info("Select an upgrade to purchase."); return }

    // Map list index → actual upgrade
    val up = if (idx == 0) Irrigation else YieldBoost

    // Try to purchase
    game.buyUpgrade(up) match {
      case Left(msg)  => error(msg)           // Insufficient funds or maxed out
      case Right(())  =>
        info("Upgrade purchased!")           // Show confirmation
        refresh()                            // Refresh list with new levels
        onMoneyChanged()                     // Update money label in header
    }
  }

  // --- Helpers for showing dialogs ---

  /** Show an informational popup (non-error). */
  private def info(msg: String): Unit = {
    val a = new Alert(Alert.AlertType.INFORMATION)
    a.setHeaderText(null)
    a.setContentText(msg)
    a.showAndWait()
  }

  /** Show an error popup for failed purchases. */
  private def error(msg: String): Unit = {
    val a = new Alert(Alert.AlertType.ERROR)
    a.setHeaderText("Cannot purchase")
    a.setContentText(msg)
    a.showAndWait()
  }
}
