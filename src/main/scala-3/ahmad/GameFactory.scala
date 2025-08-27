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
      new Family("Ali",  need(Nutrition(900, 25, 125, 30)), stockpile = x3(Nutrition(900, 25, 125, 30))),
      new Family("Bala", need(Nutrition(1000, 30, 150, 35)), stockpile = x3(Nutrition(1000, 30, 150, 35))),
      new Family("Chen", need(Nutrition(850, 25, 120, 20)), stockpile = x3(Nutrition(850, 25, 120, 20))),
      new Family("Devi", need(Nutrition(950, 55, 135, 30)), stockpile = x3(Nutrition(950, 55, 135, 30)))
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
