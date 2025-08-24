package ahmad.service

import ahmad.model.*

object DayService {
  def endOfDay(gs: GameState, market: MarketService)
  : Either[String, (GameState, MarketService)] = {

    val allOk = gs.families.forall(_.isSatisfied)
    if (!allOk) Left("A family moved out! Game over.")
    else {
      val nextFamilies = gs.families.map { f =>
        val consumed = f.stockpile + f.receivedToday - f.dailyNeed
        new Family(f.name, f.dailyNeed, Map.empty, consumed)
      }

      val bonus    = gs.farm.growthBonusDays
      val nextGrid = gs.grid.map(_.growOneDay(bonus))

      val nextGs = new GameState(
        families = nextFamilies,
        farm     = gs.farm,
        grid     = nextGrid,          // <-- was gs.grid
        storage  = gs.storage,
        day      = gs.day + 1,
        money    = gs.money
      )

      Right(nextGs -> market.nextDay())
    }
  }
}
