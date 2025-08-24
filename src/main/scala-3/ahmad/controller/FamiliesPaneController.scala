package ahmad.controller

import ahmad.model.*
import ahmad.util.Refreshable
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.{HBox, Priority, TilePane, VBox}

import scala.jdk.CollectionConverters.*

/** JavaFX controller for the "Families" pane.
  *
  * Responsibilities:
  * - Renders a grid of family "cards" and highlights selection/needs.
  * - Shows per-family nutrition needs vs. current stockpile.
  * - Lets the user pick a produce type and quantity to assign to the selected family.
  * - Keeps the UI in sync with the underlying game state via refresh().
  *
  * Lifecyle:
  * - initialize() is invoked by the FXML loader. It wires listeners, seeds controls,
  *   and renders the initial state.
  *
  * Threading:
  * - Intended to be used on the JavaFX Application Thread.
  *
  * Collaboration:
  * - Relies on GameController for the list of families and assignment actions.
  */
final class FamiliesPaneController(private val game: GameController) extends Refreshable {

  // FXML nodes
  @FXML private var familyGrid: TilePane = _
  @FXML private var familyNameLbl: Label = _
  @FXML private var needCalLbl:  Label = _
  @FXML private var needProLbl:  Label = _
  @FXML private var needCarbLbl: Label = _
  @FXML private var needVitLbl:  Label = _
  @FXML private var curCalLbl:   Label = _
  @FXML private var curProLbl:   Label = _
  @FXML private var curCarbLbl:  Label = _
  @FXML private var curVitLbl:   Label = _
  @FXML private var produceCombo: ComboBox[String] = _
  @FXML private var qtySpinner:   Spinner[java.lang.Integer] = _
  @FXML private var btnAssign:    Button = _
  @FXML private var giveCalLbl: javafx.scene.control.Label = _
  @FXML private var giveProLbl: javafx.scene.control.Label = _
  @FXML private var giveCarbLbl: javafx.scene.control.Label = _
  @FXML private var giveVitLbl: javafx.scene.control.Label = _

  /** Index of the currently selected family in game.families, or -1 if none is selected. */
  private var selectedIdx: Int = -1

  /** Refreshes the entire pane from the current game state. */
  override def refresh(): Unit = refreshAll()

  /** FXML initialize hook.
    *
    * Performs one-time UI setup:
    * - Builds family cards and lays out the grid.
    * - Seeds produce choices and quantity spinner.
    * - Wires listeners to keep the "give" summary up to date.
    * - Selects sensible defaults and renders initial state.
    */
  @FXML private def initialize(): Unit = {
    familyGrid.getChildren.clear()
    game.families.zipWithIndex.foreach { case (f, i) =>
      val b = makeFamilyCard(i)
      familyGrid.getChildren.add(b)
    }

    HBox.setHgrow(familyGrid, Priority.ALWAYS)
    VBox.setVgrow(familyGrid, Priority.ALWAYS)
    
    produceCombo.getItems.addAll("Rice", "Beans", "Vegetables")
    qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999, 1))
    qtySpinner.setEditable(true)

    produceCombo.valueProperty.addListener((_, _, _) => updateGiveSummary())
    qtySpinner.valueProperty.addListener((_, _, _) => updateGiveSummary())

    if (!produceCombo.getItems.isEmpty && produceCombo.getValue == null)
      produceCombo.setValue("Rice")
    updateGiveSummary()

    refreshAll()
  }

  /** Selects a family card by index and refreshes the UI accordingly.
    *
    * @param i index into game.families
    */
  private def selectFamily(i: Int): Unit = {
    selectedIdx = i
    refreshAll()
    updateGiveSummary()
  }

  /** Recomputes and displays the nutrient totals for the currently chosen
    * produce and quantity in the "give" summary area.
    * Safe to call at any time; handles empty/invalid inputs gracefully.
    */
  private def updateGiveSummary(): Unit = {
    val qty: Int =
      Option(qtySpinner.getValue).map(_.intValue()).getOrElse(0)

    val maybeProd: Option[Produce] = Option(produceCombo.getValue) map {
      case "Rice" => Rice()
      case "Beans" => Beans()
      case "Vegetables" => Vegetables()
      case _ => Rice()
    }

    val totals: Nutrition = maybeProd match
      case Some(p) if qty > 0 => scaled(p.nutritionPerUnit, qty)
      case _ => new Nutrition(0, 0, 0, 0)

    giveCalLbl.setText(s"Calories: ${totals.cal}")
    giveProLbl.setText(s"Protein: ${totals.protein}")
    giveCarbLbl.setText(s"Carbs: ${totals.carbs}")
    giveVitLbl.setText(s"Vitamins: ${totals.vitamins}")
  }

  /** Utility to scale a Nutrition profile by an integer quantity.
    *
    * @param n base per-unit nutrition
    * @param q number of units
    * @return nutrition totals for q units
    */
  private def scaled(n: Nutrition, q: Int): Nutrition =
    new Nutrition(n.cal * q, n.protein * q, n.carbs * q, n.vitamins * q)

  /** Creates a styled, clickable card for the i-th family. */
  private def makeFamilyCard(i: Int): Button = {
    val b = new Button()
    b.setMinSize(180, 140)
    b.setPrefSize(180, 140)
    b.setWrapText(true)
    b.getStyleClass.add("family-card")
    b.setOnAction(_ => selectFamily(i))
    refreshCard(i, b)
    b
  }

  /** Updates the content and style of a family card button.
    *
    * @param i index of the family
    * @param b button to update
    */
  private def refreshCard(i: Int, b: Button): Unit = {
    val f  = game.families(i)
    val ok = f.isSatisfied
    b.setText(s"${f.name}\n${if ok then "Satisfied" else "Needs food!"}")

    val classes = b.getStyleClass
    classes.removeAll("family-card-ok","family-card-bad","family-card-selected")
    classes.add(if ok then "family-card-ok" else "family-card-bad")
    if (i == selectedIdx) classes.add("family-card-selected")
  }

  /** Refreshes all cards and the details panel based on the current selection. */
  private def refreshAll(): Unit = {
    familyGrid.getChildren.asScala.zipWithIndex.foreach { case (n, i) =>
      refreshCard(i, n.asInstanceOf[Button])
    }
    if (selectedIdx >= 0) updateDetails()
    else clearDetails()
  }

  /** Clears the details panel when no family is selected. */
  private def clearDetails(): Unit = {
    familyNameLbl.setText("(select a family)")
    Seq(needCalLbl, needProLbl, needCarbLbl, needVitLbl,
      curCalLbl, curProLbl, curCarbLbl, curVitLbl).foreach(_.setText("—"))
    Seq(curCalLbl, curProLbl, curCarbLbl, curVitLbl).foreach(resetMetricStyle)
  }

  /** Populates the details panel with the selected family's needs and current stockpile,
    * including per-metric styling to indicate sufficiency.
    */
  private def updateDetails(): Unit = {
    val f = game.families(selectedIdx)
    familyNameLbl.setText(f.name)

    val need = f.dailyNeed
    val cur = f.stockpile

    needCalLbl.setText(s"Calories: ${need.cal}")
    needProLbl.setText(s"Protein:  ${need.protein}")
    needCarbLbl.setText(s"Carbs:    ${need.carbs}")
    needVitLbl.setText(s"Vitamins: ${need.vitamins}")

    setMetric(curCalLbl, "Calories", cur.cal, need.cal)
    setMetric(curProLbl, "Protein", cur.protein, need.protein)
    setMetric(curCarbLbl, "Carbs", cur.carbs, need.carbs)
    setMetric(curVitLbl, "Vitamins", cur.vitamins, need.vitamins)
  }

  /** Sets a single metric label text and applies ok/bad style based on sufficiency.
    *
    * @param label target label
    * @param name  display name of the metric
    * @param cur   current available amount
    * @param need  required amount
    */
  private def setMetric(label: Label, name: String, cur: Int, need: Int): Unit = {
    label.setText(f"$name: $cur%,d / $need%,d")
    val ok = cur >= need
    applyMetricStyle(label, ok)
  }

  /** Applies the corresponding CSS class for metric status (ok/bad). */
  private def applyMetricStyle(label: Label, ok: Boolean): Unit = {
    val cls = label.getStyleClass
    cls.removeAll("metric-ok","metric-bad")
    cls.add(if ok then "metric-ok" else "metric-bad")
  }

  /** Removes any previously applied metric status CSS classes. */
  private def resetMetricStyle(label: Label): Unit =
    label.getStyleClass.removeAll("metric-ok","metric-bad")

  /** Assigns the selected produce and quantity to the selected family.
    *
    * Validates selection and stock availability, shows alerts on failure,
    * and refreshes the UI on completion.
    */
  @FXML private def onAssign(): Unit = {
    if (selectedIdx < 0) {
      val a = new Alert(Alert.AlertType.INFORMATION)
      a.setHeaderText("Select a family first")
      a.showAndWait()
      return
    }

    val p = produceCombo.getValue match
      case "Rice" => Rice()
      case "Beans" => Beans()
      case "Vegetables" => Vegetables()
      case _ => Rice()

    val qty: Int = Option(qtySpinner.getValue).map(_.intValue()).getOrElse(0)

    val ok = game.assignToFamily(selectedIdx, p, qty)
    if (!ok) {
      val a = new Alert(Alert.AlertType.ERROR)
      a.setHeaderText("Not enough stock in storage.")
      a.showAndWait()
    }

    refreshAll()
    updateGiveSummary()
  }
}
