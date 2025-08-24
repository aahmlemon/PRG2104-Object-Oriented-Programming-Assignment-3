package ahmad.controller

import ahmad.model.*
import ahmad.util.*
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.{Button, Label, Tooltip}
import javafx.scene.image.{Image, ImageView}
import javafx.scene.layout.{GridPane, TilePane, VBox}

/** JavaFX controller for the farm grid view.
  *
  * Purpose:
  * - Renders a grid of farm plots and a palette of seed cards.
  * - Allows planting a selected seed into a plot, or harvesting when mature.
  * - Reflects plot state (empty, growing, ready) via text and CSS classes.
  *
  * Lifecycle:
  * - initialize() is called by the FXML loader to build UI, wire handlers, and render initial state.
  *
  * Collaboration:
  * - Uses GameController as the source of truth for grid state and actions (plant/harvest).
  * - Implements Refreshable to re-render plots on demand.
  * - Implements InventoryAware to notify other UI parts when planting/harvesting changes inventory.
  *
  * Threading:
  * - UI updates occur on the JavaFX Application Thread. Inventory callbacks are dispatched via Platform.runLater.
  */
final class FarmGridController(private val game: GameController) extends Refreshable, InventoryAware {

  // FXML-injected nodes
  @FXML private var farmHouseImage: ImageView = _
  @FXML private var gridPane: GridPane = _
  @FXML private var seedTilePane: TilePane = _
  @FXML private var selectedSeedLabel: Label = _

  // UI state
  /** Currently selected seed from the seed palette, or None if none is selected. */
  private var selectedSeed: Option[Produce] = None
  /** Buttons representing each plot in the grid, kept for efficient refresh. */
  private val buttons = scala.collection.mutable.ArrayBuffer.empty[javafx.scene.control.Button]

  /** Inventory change callback provided by other UI parts (e.g., storage panel). */
  private var onInvChanged: () => Unit = () => ()
  /** Registers a callback to be invoked whenever planting/harvesting affects inventory. */
  override def setOnInventoryChanged(cb: () => Unit): Unit = onInvChanged = cb

  /** Dispatches the inventory-changed callback on the JavaFX Application Thread. */
  private def pingInventoryChanged(): Unit =
    Platform.runLater(() => onInvChanged())

  /** FXML initialization hook.
    *
    * - Binds the farmhouse image and loads it if available.
    * - Creates and inserts plot buttons into the grid.
    * - Builds seed cards and wires selection behavior.
    * - Renders initial selection label and plot states.
    */
  @FXML private def initialize(): Unit = {
    // --- House image (optional placeholder if missing) ---
    // Put your real image at /assets/images/farmhouse.png
    val houseUrl = Option(getClass.getResource("/assets/images/farmhouse.png"))
    houseUrl.foreach(u => farmHouseImage.setImage(new Image(u.toExternalForm)))
    farmHouseImage.fitWidthProperty().bind(gridPane.widthProperty)
    farmHouseImage.setPreserveRatio(true)
    
    val cols = 4; val rows = 2
    var idx  = 0
    for (r <- 0 until rows; c <- 0 until cols) {
      val btn = makePlotButton(idx)
      buttons += btn
      gridPane.add(btn, c, r)
      idx += 1
    }
    
    val seeds: Seq[(Produce, String, String)] = Seq(
      (Rice(),        "Rice",        "/assets/images/rice.png"),
      (Beans(),       "Beans",       "/assets/images/beans.png"),
      (Vegetables(),  "Vegetables",  "/assets/images/vegetables.png")
      // More can be added
    )

    seeds.foreach { case (prod, label, imgPath) =>
      val node = makeSeedCard(prod, label, imgPath)
      seedTilePane.getChildren.add(node)
    }

    updateSelectedSeedLabel()
    refresh()
  }

  /** Re-renders all plot buttons to reflect the latest game state. */
  override def refresh(): Unit = {
    buttons.indices.foreach(i => refreshPlotButton(i, buttons(i)))
  }
  
  /** Builds a plot button for a grid index and wires its click behavior.
    *
    * Behavior:
    * - If no seed is selected, shows an informational alert.
    * - If plot is empty, plants the selected seed.
    * - If plot is planted, harvests it (when mature or otherwise as allowed).
    * - Notifies inventory listeners and refreshes the button state after actions.
    *
    * @param i zero-based plot index into the game grid
    * @return a configured Button for insertion into the GridPane
    */
  private def makePlotButton(i: Int): Button = {
    val b = new Button()
    b.getStyleClass.addAll("card", "plot-button")
    b.setPrefSize(190, 140)
    b.setWrapText(true)
    b.setTooltip(new Tooltip("Click to plant selected seed (or harvest if mature)"))

    b.setOnAction { _ =>
      val tile = game.gs.grid(i)
      
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
            pingInventoryChanged()
          }
          refreshPlotButton(i, b)
    }

    refreshPlotButton(i, b)
    b
  }
  
  /** Updates the visual state of a plot button based on the underlying tile.
    *
    * Applies/removes CSS classes:
    * - "planted" and "card--active" when a seed is present.
    * - "ready" in addition when the crop is mature.
    *
    * Text shows crop name and either growth progress or harvest readiness.
    *
    * @param i plot index
    * @param b plot button to update
    */
  private def refreshPlotButton(i: Int, b: Button): Unit = {
    val t = game.gs.grid(i)
    
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
  
  /** Creates a selectable seed card (image + label) and wires selection highlighting.
    *
    * Selecting a seed updates the selectedSeed state and the label shown in the UI.
    *
    * @param p       produce instance for planting
    * @param label   display name shown under the image
    * @param imgPath resource path to the seed image
    * @return a VBox representing the seed card
    */
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
    
    box.setOnMouseClicked { _ =>
      selectedSeed = Some(p)
      updateSelectedSeedLabel()
      highlightSelected(box)
    }

    box
  }
  
  /** Updates the "Selected seed" label to reflect the current selection. */
  private def updateSelectedSeedLabel(): Unit =
    selectedSeedLabel.setText(selectedSeed.map(_.name).getOrElse("(none)"))
  
  /** Highlights the given seed card and removes highlight from others. */
  private def highlightSelected(selectedBox: VBox): Unit = {
    seedTilePane.getChildren.forEach { n =>
      n.getStyleClass.remove("selected")
    }
    selectedBox.getStyleClass.add("selected")
  }
}
