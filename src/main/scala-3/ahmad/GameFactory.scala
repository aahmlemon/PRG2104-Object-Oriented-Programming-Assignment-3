package ahmad

import ahmad.controller.*
import ahmad.model.*
import ahmad.service.*

object GameFactory {
  def newGame(): GameController = {
    def need(n: Nutrition) = n
    def x3(n: Nutrition)   = Nutrition(n.cal*3, n.protein*3, n.carbs*3, n.vitamins*3)

    val families = Vector(
      new Family("Ali",  need(Nutrition(1800,50,250,60)), stockpile = x3(Nutrition(1800,50,250,60))),
      new Family("Bala", need(Nutrition(2000,60,300,70)), stockpile = x3(Nutrition(2000,60,300,70))),
      new Family("Chen", need(Nutrition(1700,45,230,55)), stockpile = x3(Nutrition(1700,45,230,55))),
      new Family("Devi", need(Nutrition(1900,55,270,65)), stockpile = x3(Nutrition(1900,55,270,65)))
    )

    val gs = new GameState(
      families = families,
      farm     = new Farm(),
      grid     = Vector.fill(12)(new CropTile()),
      storage  = Storage(),
      day      = 1,
      money    = 0
    )

    val market = Market.initial(List(Rice(), Beans(), Vegetables()))
    new GameController(gs, market)
  }
}
