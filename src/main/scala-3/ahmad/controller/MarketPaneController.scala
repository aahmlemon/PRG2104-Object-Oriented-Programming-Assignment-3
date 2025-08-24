package ahmad.controller

import ahmad.model.*
import ahmad.util.{InventoryAware, MoneyAware, Refreshable}
import javafx.beans.property.{SimpleIntegerProperty, SimpleStringProperty}
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*

import scala.jdk.CollectionConverters.*

final class MarketPaneController(private val game: GameController)
  extends Refreshable, InventoryAware, MoneyAware {

  // table
  @FXML private var priceTable: TableView[Row] = _
  @FXML private var colProd:  TableColumn[Row, String] = _
  @FXML private var colPrice: TableColumn[Row, Number] = _
  @FXML private var colHave:  TableColumn[Row, Number] = _
  @FXML private var colDelta: TableColumn[Row, String] = _

  // sell panel
  @FXML private var sellProduceCombo: ComboBox[String] = _
  @FXML private var sellQtySpinner: Spinner[java.lang.Integer] = _
  @FXML private var btnSell: Button = _
  @FXML private var livePriceLbl: Label = _
  @FXML private var liveRevenueLbl: Label = _
  @FXML private var haveLbl: Label = _

  private val model = FXCollections.observableArrayList[Row]()
  private var onInvChanged: () => Unit = () => ()

  override def setOnInventoryChanged(cb: () => Unit): Unit = onInvChanged = cb
  override def refresh(): Unit = refreshTable()

  private var onMoneyChanged: () => Unit = () => ()
  override def setOnMoneyChanged(cb: () => Unit): Unit = onMoneyChanged = cb

  @FXML private def initialize(): Unit = {
    priceTable.setItems(model)

    colProd.setCellValueFactory (cdf => new SimpleStringProperty(cdf.getValue.name))
    colPrice.setCellValueFactory(cdf => new SimpleIntegerProperty(cdf.getValue.price))

    // Δ text
    colDelta.setCellValueFactory(cdf => new SimpleStringProperty(cdf.getValue.delta))

    // Optional: color Δ cells red/green
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

    // sell controls
    sellProduceCombo.getItems.addAll("Rice", "Beans", "Vegetables")
    sellQtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1, 1))
    sellQtySpinner.setEditable(true)

    // react to selection/qty to update price & revenue
    sellProduceCombo.valueProperty.addListener((_, _, _) => updateSellPanel())
    sellQtySpinner.valueProperty.addListener((_, _, _) => updateSellPanel())

    // click row to prefill
    priceTable.getSelectionModel.selectedItemProperty.addListener((_, _, row) => {
      if (row != null) {
        sellProduceCombo.setValue(row.name)
        updateSellPanel()
      }
    })

    // disable button if invalid
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

  private def updateSellPanel(): Unit = {
    val p = Option(sellProduceCombo.getValue).map(toProduce)
    val price = p.map(game.prices.getOrElse(_, 0)).getOrElse(0)
    val have  = p.map(game.storage.amountOf).getOrElse(0)
    val max   = if (have <= 0) 1 else have

    // cap spinner to available stock
    val vf = sellQtySpinner.getValueFactory.asInstanceOf[SpinnerValueFactory.IntegerSpinnerValueFactory]
    vf.setMax(math.max(1, have))
    if (sellQtySpinner.getValue == null || sellQtySpinner.getValue.intValue() > max)
      sellQtySpinner.getValueFactory.setValue(Integer.valueOf(math.min(1, max)))

    val qty = Option(sellQtySpinner.getValue).fold(0)(_.intValue())
    livePriceLbl.setText(f"$$$price%,d")
    liveRevenueLbl.setText(f"$$${price * qty}%,d")
    haveLbl.setText(s"/ $have")
  }

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

  private def toProduce(name: String): Produce = name match {
    case "Rice"       => Rice()
    case "Beans"      => Beans()
    case "Vegetables" => Vegetables()
    case _            => Rice()
  }

  final case class Row(name: String, price: Int, delta: String)

}
