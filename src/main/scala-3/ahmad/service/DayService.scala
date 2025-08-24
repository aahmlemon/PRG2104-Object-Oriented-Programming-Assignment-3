package ahmad.service

import ahmad.model.*

/**
 * Service object that handles the transition logic at the end of each in-game day.
 *
 * Responsibilities:
 *  - Verify whether all families are satisfied with their daily needs.
 *    * If not, the game ends with a failure message.
 *    * If yes, progress the game to the next day.
 *  - Update families’ stockpiles and reset their "received today" amounts.
 *  - Advance the farm grid one day forward, applying growth bonuses if applicable.
 *  - Increment the day counter in the game state.
 *  - Trigger a new market day, evolving produce prices.
 *
 * The result is expressed as an [[Either]]:
 *  - `Left(String)` if the game is over due to unsatisfied families.
 *  - `Right((GameState, MarketService))` if the game continues, providing the updated state
 *    and the evolved market service.
 */
object DayService {
  /**
   * Process the end-of-day transition for the given game state and market.
   *
   * @param gs     The current [[GameState]], containing families, farm, grid, storage, etc.
   * @param market The current [[MarketService]], holding daily produce prices.
   * @return       Either:
   *               - `Left(message)` if the game ends (a family moved out).
   *               - `Right((nextState, nextMarket))` if the game continues.
   */
  def endOfDay(gs: GameState, market: MarketService)
  : Either[String, (GameState, MarketService)] = {

    // Check if every family met its daily needs
    val allOk = gs.families.forall(_.isSatisfied)
    if (!allOk) Left("A family moved out! Game over.")
    else {
      // Update each family: reset receivedToday, apply consumption, and carry over stockpile
      val nextFamilies = gs.families.map { f =>
        val consumed = f.stockpile + f.receivedToday - f.dailyNeed
        new Family(f.name, f.dailyNeed, Map.empty, consumed)
      }

      // Farm bonus days contribute to crop growth
      val bonus    = gs.farm.growthBonusDays
      val nextGrid = gs.grid.map(_.growOneDay(bonus))

      // Construct the new game state for the next day
      val nextGs = new GameState(
        families = nextFamilies,
        farm     = gs.farm,
        grid     = nextGrid,
        storage  = gs.storage,
        day      = gs.day + 1,
        money    = gs.money
      )

      // Return the updated game state paired with the evolved market
      Right(nextGs -> market.nextDay())
    }
  }
}
