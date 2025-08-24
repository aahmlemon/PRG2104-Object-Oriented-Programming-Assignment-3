package ahmad

import ahmad.GameFactory.*
import ahmad.controller.*
import ahmad.model.*
import ahmad.service.*
import javafx.fxml.FXMLLoader
import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.scene.Scene

object MainApp extends JFXApp3 {

  private val sharedGame = {
    val gs = new GameState(
      families = Vector(
        new Family("Ali",  Nutrition(1800,50,250,60), stockpile = Nutrition(1800,50,250,60) * 3),
        new Family("Bala", Nutrition(2000,60,300,70), stockpile = Nutrition(2000,60,300,70) * 3),
        new Family("Chen", Nutrition(1700,45,230,55), stockpile = Nutrition(1700,45,230,55) * 3),
        new Family("Devi", Nutrition(1900,55,270,65), stockpile = Nutrition(1900,55,270,65) * 3)
      ),
      farm = new Farm(),
      grid = Vector.fill(12)(new CropTile()),
      storage = Storage(),
      day = 1,
      money = 0
    )
    val market = Market.initial(List(Rice(), Beans(), Vegetables()))
    new GameController(gs, market)
  }

  override def start(): Unit = {
    val loader = new FXMLLoader(getClass.getResource("/ahmad/view/TitleScreen.fxml"))
    loader.setControllerFactory { clazz =>
      if (clazz == classOf[TitleScreenController]) new TitleScreenController(() => GameFactory.newGame())
      else clazz.getDeclaredConstructor().newInstance()
    }

    val jfxRoot: javafx.scene.Parent = loader.load()
    val sfxRoot: scalafx.scene.Parent = jfxRoot

    stage = new JFXApp3.PrimaryStage {
      title = "Farm & Family"
      scene = new Scene(sfxRoot, 1200, 720)
      resizable = false
    }

    stage.scene().stylesheets += getClass.getResource("/css/app.css").toExternalForm
  }
}
