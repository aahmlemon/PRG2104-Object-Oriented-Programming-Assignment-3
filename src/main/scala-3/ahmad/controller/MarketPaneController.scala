package ahmad.controller

import ahmad.model.*
import ahmad.util.{InventoryAware, MoneyAware, Refreshable}
import javafx.beans.property.{SimpleIntegerProperty, SimpleStringProperty}
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*

import scala.jdk.CollectionConverters.*

/** JavaFX controller for the Market pane.
  *
  * Responsibilities:
  * - Displays current market prices and day-over-day deltas in a table.
  * - Lets the user select a produce and quantity to sell from storage.
  * - Shows live computed revenue and available stock for the selection.
  * - Notifies listeners when inventory or money changes after a sale.
  *
  * Lifecycle:
  * - initialize() is called by the FXML loader to wire the table, inputs,
  *   cell factories, listeners, and to render the initial state.
  *
  * Collaboration:
  * - Reads prices and deltas from GameController/MarketService.
  * - Reads inventory from GameController.storage.
  * - Implements Refreshable, InventoryAware, MoneyAware to integrate with the rest of the UI.
  */
final class MarketPaneController(private val game: GameController)
  extends Refreshable, InventoryAware, MoneyAware {
  
  @FXML private var priceTable: TableView[Row] = _
  @FXML private var colProd:  TableColumn[Row, String] = _
  @FXML private var colPrice: TableColumn[Row, Number] = _
  @FXML private var colHave:  TableColumn[Row, Number] = _
  @FXML private var colDelta: TableColumn[Row, String] = _
  
  @FXML private var sellProduceCombo: ComboBox[String] = _
  @FXML private var sellQtySpinner: Spinner[java.lang.Integer] = _
  @FXML private var btnSell: Button = _
  @FXML private var livePriceLbl: Label = _
  @FXML private var liveRevenueLbl: Label = _
  @FXML private var haveLbl: Label = _

  private val model = FXCollections.observableArrayList[Row]()
  private var onInvChanged: () => Unit = () => ()

  override def setOnInventoryChanged(cb: () => Unit): Unit = onInvChanged = cb
  /** Refreshes the prices table from the current market data. */
  override def refresh(): Unit = refreshTable()

  private var onMoneyChanged: () => Unit = () => ()
  override def setOnMoneyChanged(cb: () => Unit): Unit = onMoneyChanged = cb

  /** FXML initialization hook.
    *
    * Sets up:
    * - Table items and columns, including delta coloring.
    * - Sell panel controls (produce combo, quantity spinner, button enablement).
    * - Listeners that keep the sell panel in sync with the current selection.
    * Also performs the initial render of the table and sell panel.
    */
  @FXML private def initialize(): Unit = {
    priceTable.setItems(model)

    colProd.setCellValueFactory (cdf => new SimpleStringProperty(cdf.getValue.name))
    colPrice.setCellValueFactory(cdf => new SimpleIntegerProperty(cdf.getValue.price))


    colDelta.setCellValueFactory(cdf => new SimpleStringProperty(cdf.getValue.delta))
    colDelta.setCellFactory { _ =>
      new TableCell[Row, String]() {
        override def updateItem(value: String, empty: Boolean): Unit = {
          super.updateItem(value, empty)
          setText(if (empty) null else value)
          setStyle("")
          if (!empty && value != "—") {
            if (value.startsWith("+")) setStyle("-fx-text-fill: -fx-accent;")   // green
            else if (value.startsWith("-")) setStyle("-fx-text-fill: #c0392b;") // red
          }
        }
      }
    }
    
    sellProduceCombo.getItems.addAll("Rice", "Beans", "Vegetables")
    sellQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1))
    sellQtySpinner.setEditable(true)
    
    sellProduceCombo.valueProperty.addListener((_, _, _) => updateSellPanel())
    sellQtySpinner.valueProperty.addListener((_, _, _) => updateSellPanel())
    
    priceTable.getSelectionModel.selectedItemProperty.addListener((_, _, row) => {
      if (row != null) {
        sellProduceCombo.setValue(row.name)
        updateSellPanel()
      }
    })
    
    btnSell.disableProperty().bind(
      sellProduceCombo.valueProperty().isNull
        .or(sellQtySpinner.valueProperty().isNull)
        .or(sellQtySpinner.valueProperty().isEqualTo(0))
    )

    btnSell.setOnAction { _ =>
      val p = toProduce(sellProduceCombo.getValue)
      val qty = Option(sellQtySpinner.getValue).fold(0)(_.intValue())
      if (qty > 0 && game.sell(p, qty)) {
        refreshTable()
        updateSellPanel()
        onInvChanged()
        onMoneyChanged()
      } else {
        new Alert(Alert.AlertType.WARNING) {
          setHeaderText("Cannot sell")
          setContentText("Not enough stock.")
        }.showAndWait()
      }
    }

    refreshTable()
    updateSellPanel()
  }

  /** Recomputes the sell panel based on current UI selections.
    *
    * Derives:
    * - Unit price for the selected produce.
    * - Available stock in storage (caps spinner max to available).
    * - Live revenue = price * quantity.
    * Updates labels and spinner bounds defensively against null/empty values.
    */
  private def updateSellPanel(): Unit = {
    val p = Option(sellProduceCombo.getValue).map(toProduce)
    val price = p.map(game.prices.getOrElse(_, 0)).getOrElse(0)
    val have  = p.map(game.storage.amountOf).getOrElse(0)
    val max   = if (have <= 0) 1 else have
    
    val vf = sellQtySpinner.getValueFactory.asInstanceOf[SpinnerValueFactory.IntegerSpinnerValueFactory]
    vf.setMax(math.max(1, have))
    if (sellQtySpinner.getValue == null || sellQtySpinner.getValue.intValue() > max)
      sellQtySpinner.getValueFactory.setValue(Integer.valueOf(math.min(1, max)))

    val qty = Option(sellQtySpinner.getValue).fold(0)(_.intValue())
    livePriceLbl.setText(f"$$$price%,d")
    liveRevenueLbl.setText(f"$$${price * qty}%,d")
    haveLbl.setText(s"/ $have")
  }

  /** Rebuilds the rows for the price table from the current market.
    *
    * - Sorts by produce name for stable presentation.
    * - Formats delta as "+d", "d", "0", or "—" when no previous day exists.
    * - Replaces the observable model content in bulk for efficient UI updates.
    */
  private def refreshTable(): Unit = {
    val rows = game.prices.toVector
      .sortBy(_._1.name)
      .map { case (p, price) =>
        val dStr = game.market.delta(p) match {
          case Some(d) if d > 0  => s"+$d"
          case Some(d) if d < 0  => s"$d"
          case Some(_)           => "0"
          case None              => "—"     // no “yesterday” yet (day 1)
        }
        Row(p.name, price, dStr)
      }
    model.setAll(rows.asJava)
  }

  /** Maps a display name to its corresponding Produce instance.
    * Falls back to Rice when the name is unknown.
    */
  private def toProduce(name: String): Produce = name match {
    case "Rice"       => Rice()
    case "Beans"      => Beans()
    case "Vegetables" => Vegetables()
    case _            => Rice()
  }

  /** Table row model for a single produce entry. */
  final private case class Row(name: String, price: Int, delta: String)
}
