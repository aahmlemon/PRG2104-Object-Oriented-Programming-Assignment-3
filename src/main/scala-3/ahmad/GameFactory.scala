package ahmad

import ahmad.controller.*
import ahmad.model.*
import ahmad.service.*

/**
 * Factory object for creating a brand-new game instance.
 *
 * Provides utilities to build the initial [[GameState]],
 * set up families with nutritional needs and stockpiles,
 * initialize the farm and crop grid, and create the market.
 */
object GameFactory {
  /**
   * Creates a fresh [[GameController]] with:
   *  - Four families (Ali, Bala, Chen, Devi) with predefined nutrition needs
   *    and starting stockpiles equal to 3 days of food.
   *  - An empty farm and a 12-tile crop grid.
   *  - An empty storage, day set to 1, and money set to 0.
   *  - An initial market with Rice, Beans, and Vegetables.
   *
   * @return A fully initialized [[GameController]] ready to start play.
   */
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
