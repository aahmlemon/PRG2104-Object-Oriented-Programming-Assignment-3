package ahmad.controller

import ahmad.model.*
import ahmad.util.*
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.{Button, Label, Tooltip}
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.{GridPane, TilePane, VBox}
// Rice, Beans, Vegetables, etc.

final class FarmGridController(private val game: GameController) extends Refreshable, InventoryAware {

  // FXML-injected nodes
  @FXML private var farmHouseImage: ImageView = _
  @FXML private var gridPane: GridPane = _
  @FXML private var seedTilePane: TilePane = _
  @FXML private var selectedSeedLabel: Label = _

  // UI state
  private var selectedSeed: Option[Produce] = None
  private val buttons = scala.collection.mutable.ArrayBuffer.empty[javafx.scene.control.Button]

  private var onInvChanged: () => Unit = () => ()
  override def setOnInventoryChanged(cb: () => Unit): Unit = onInvChanged = cb

  private def pingInventoryChanged(): Unit =
    Platform.runLater(() => onInvChanged())

  @FXML private def initialize(): Unit = {
    // --- House image (optional placeholder if missing) ---
    // Put your real image at /assets/images/farmhouse.png
    val houseUrl = Option(getClass.getResource("/assets/images/farmhouse.png"))
    houseUrl.foreach(u => farmHouseImage.setImage(new Image(u.toExternalForm)))
    farmHouseImage.fitWidthProperty().bind(gridPane.widthProperty)
    farmHouseImage.setPreserveRatio(true)

    // --- Build the 2x4 plot grid (12 tiles) ---
    val cols = 4; val rows = 2
    var idx  = 0
    for (r <- 0 until rows; c <- 0 until cols) {
      val btn = makePlotButton(idx)
      buttons += btn // <-- keep a handle to refresh later
      gridPane.add(btn, c, r)
      idx += 1
    }

    // --- Populate the scrollable seed list ---
    // Add whatever seeds your game supports; each shows image and label
    val seeds: Seq[(Produce, String, String)] = Seq(
      (Rice(),        "Rice",        "/assets/images/rice.png"),
      (Beans(),       "Beans",       "/assets/images/beans.png"),
      (Vegetables(),  "Vegetables",  "/assets/images/vegetables.png")
      // add more here...
    )

    seeds.foreach { case (prod, label, imgPath) =>
      val node = makeSeedCard(prod, label, imgPath)
      seedTilePane.getChildren.add(node)
    }

    updateSelectedSeedLabel()
    refresh()
  }

  override def refresh(): Unit = {
    buttons.indices.foreach(i => refreshPlotButton(i, buttons(i)))
  }

  // --- Create a farm plot button for index `i` ---
  private def makePlotButton(i: Int): Button = {
    val b = new Button()
    b.getStyleClass.addAll("card", "plot-button")
    b.setPrefSize(190, 140) // slightly larger, consistent
    b.setWrapText(true)
    b.setTooltip(new Tooltip("Click to plant selected seed (or harvest if mature)"))

    b.setOnAction { _ =>
      val tile = game.gs.grid(i)

      // if no seed chosen -> do nothing (or show an alert)
      selectedSeed match
        case None =>
          val a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION)
          a.setHeaderText("No seed selected")
          a.setContentText("Pick a seed from the Crops list on the right.")
          a.showAndWait()

        case Some(seed) =>
          if (tile.seed.isEmpty) then {
            game.plantAt(i, seed)
            refreshPlotButton(i, b)
            pingInventoryChanged()
          } else {
            game.harvestAt(i)
            refreshPlotButton(i, b)
            pingInventoryChanged()          // <--- notify header to refresh
          }
          refreshPlotButton(i, b)
    }

    refreshPlotButton(i, b) // set initial text/state
    b
  }

  // --- Refresh a single plot button label/state from game state ---
  private def refreshPlotButton(i: Int, b: Button): Unit = {
    val t = game.gs.grid(i)

    // Always reset first
    b.getStyleClass.removeAll("planted", "card--active", "ready")

    if (t.seed.isEmpty) {
      b.setText(s"Plot ${i + 1}\n(empty)")
    } else {
      val name = t.seed.get.name
      val status =
        if (t.isMature) {
          b.getStyleClass.addAll("planted", "card--active", "ready")
          "Ready to harvest"
        } else {
          b.getStyleClass.addAll("planted", "card--active")
          s"Growing (${t.daysGrown}/${t.seed.get.daysToMature})"
        }
      b.setText(s"$name\n$status")
    }
  }

  // --- Build one seed card for the sidebar (image + label, clickable) ---
  private def makeSeedCard(p: Produce, label: String, imgPath: String): javafx.scene.layout.VBox = {
    val box = new javafx.scene.layout.VBox(6)
    box.getStyleClass.add("seed-card")
    box.setPrefWidth(220)

    val imgView = new ImageView()
    imgView.setFitWidth(200)
    imgView.setFitHeight(120)
    imgView.setPreserveRatio(true)
    Option(getClass.getResource(imgPath)).foreach { u =>
      imgView.setImage(new Image(u.toExternalForm))
    }

    val name = new Label(label)
    name.setStyle("-fx-font-weight: bold;")

    box.getChildren.addAll(imgView, name)

    // select on click
    box.setOnMouseClicked { _ =>
      selectedSeed = Some(p)
      updateSelectedSeedLabel()
      highlightSelected(box)
    }

    box
  }

  // --- Update selected seed label ---
  private def updateSelectedSeedLabel(): Unit =
    selectedSeedLabel.setText(selectedSeed.map(_.name).getOrElse("(none)"))

  // --- Simple highlight: remove highlight from others, add to clicked ---
  private def highlightSelected(selectedBox: VBox): Unit = {
    seedTilePane.getChildren.forEach { n =>
      n.getStyleClass.remove("selected")
    }
    selectedBox.getStyleClass.add("selected")
  }
}
