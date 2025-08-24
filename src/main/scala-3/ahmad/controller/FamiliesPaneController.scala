package ahmad.controller

import ahmad.model.*
import ahmad.util.Refreshable
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.{HBox, Priority, TilePane, VBox}

import scala.jdk.CollectionConverters.*

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


  private var selectedIdx: Int = -1

  override def refresh(): Unit = refreshAll()

  @FXML private def initialize(): Unit = {
    // build 2x2 cards
    familyGrid.getChildren.clear()
    game.families.zipWithIndex.foreach { case (f, i) =>
      val b = makeFamilyCard(i)
      familyGrid.getChildren.add(b)
    }

    HBox.setHgrow(familyGrid, Priority.ALWAYS)
    VBox.setVgrow(familyGrid, Priority.ALWAYS)

    // controls
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

  // ---- UI builders/refresh --------------------------------------------------

  private def selectFamily(i: Int): Unit = {
    selectedIdx = i
    refreshAll()
    updateGiveSummary()
  }

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

  private def scaled(n: Nutrition, q: Int): Nutrition =
    new Nutrition(n.cal * q, n.protein * q, n.carbs * q, n.vitamins * q)

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

  private def refreshCard(i: Int, b: Button): Unit = {
    val f  = game.families(i)
    val ok = f.isSatisfied
    b.setText(s"${f.name}\n${if ok then "Satisfied" else "Needs food!"}")

    val classes = b.getStyleClass
    classes.removeAll("family-card-ok","family-card-bad","family-card-selected")
    classes.add(if ok then "family-card-ok" else "family-card-bad")
    if (i == selectedIdx) classes.add("family-card-selected")
  }

  private def refreshAll(): Unit = {
    // cards
    familyGrid.getChildren.asScala.zipWithIndex.foreach { case (n, i) =>
      refreshCard(i, n.asInstanceOf[Button])
    }
    // details
    if (selectedIdx >= 0) updateDetails()
    else clearDetails()
  }

  private def clearDetails(): Unit = {
    familyNameLbl.setText("(select a family)")
    Seq(needCalLbl, needProLbl, needCarbLbl, needVitLbl,
      curCalLbl, curProLbl, curCarbLbl, curVitLbl).foreach(_.setText("—"))
    Seq(curCalLbl, curProLbl, curCarbLbl, curVitLbl).foreach(resetMetricStyle)
  }

  private def updateDetails(): Unit = {
    val f = game.families(selectedIdx)
    familyNameLbl.setText(f.name)

    val need = f.dailyNeed
    val cur = f.stockpile // <-- show stockpile here

    needCalLbl.setText(s"Calories: ${need.cal}")
    needProLbl.setText(s"Protein:  ${need.protein}")
    needCarbLbl.setText(s"Carbs:    ${need.carbs}")
    needVitLbl.setText(s"Vitamins: ${need.vitamins}")

    setMetric(curCalLbl, "Calories", cur.cal, need.cal)
    setMetric(curProLbl, "Protein", cur.protein, need.protein)
    setMetric(curCarbLbl, "Carbs", cur.carbs, need.carbs)
    setMetric(curVitLbl, "Vitamins", cur.vitamins, need.vitamins)
  }

  private def setMetric(label: Label, name: String, cur: Int, need: Int): Unit = {
    label.setText(f"$name: $cur%,d / $need%,d")
    val ok = cur >= need
    applyMetricStyle(label, ok)
  }

  private def applyMetricStyle(label: Label, ok: Boolean): Unit = {
    val cls = label.getStyleClass
    cls.removeAll("metric-ok","metric-bad")
    cls.add(if ok then "metric-ok" else "metric-bad")
  }
  private def resetMetricStyle(label: Label): Unit =
    label.getStyleClass.removeAll("metric-ok","metric-bad")

  // ---- Assign action --------------------------------------------------------

  @FXML private def onAssign(): Unit = {
    if (selectedIdx < 0) {
      val a = new Alert(Alert.AlertType.INFORMATION)
      a.setHeaderText("Select a family first");
      a.showAndWait();
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
    updateGiveSummary() // keep the preview in sync
  }
}
