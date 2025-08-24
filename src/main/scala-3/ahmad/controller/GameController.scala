package ahmad.controller

import ahmad.model.*
import ahmad.service.*

final class GameController(private var _gs: GameState, private var _market: MarketService) {
  def gs: GameState = _gs
  def market: MarketService = _market

  // Domain-facing labels (keep pure; UI can format if needed)
  def day: Int = _gs.day
  def money: Int = _gs.money

  // ---- Actions -------------------------------------------------------------

  def replaceState(newGs: GameState, newMarket: MarketService): Unit = {
    _gs = newGs
    _market = newMarket
  }

  def buyUpgrade(up: Upgrade): Either[String, Unit] =
    UpgradeService.buy(_gs, up).map { gs2 => _gs = gs2 }

  /** Advances the simulation by one day. */
  def endDay(): Either[String, Unit] =
    DayService.endOfDay(_gs, _market).map { case (nextGs, nextMarket) =>
      _gs = nextGs
      _market = nextMarket
    }

  /** Plants a produce at a grid index if empty. Returns true if planted. */
  def plantAt(idx: Int, p: Produce): Boolean = {
    if (!idxIsValid(idx)) return false
    val tile = _gs.grid(idx)
    if (tile.seed.nonEmpty) return false

    val updatedTile = tile.plant(p)
    val updatedGrid = _gs.grid.updated(idx, updatedTile)
    _gs = _gs.withGrid(updatedGrid)
    true
  }

  /** Harvests a tile if mature and adds 1 unit to storage. Returns true if harvested. */
  def harvestAt(idx: Int): Boolean = {
    val tileOpt = _gs.grid.lift(idx)
    tileOpt.flatMap { tile =>
      val (maybeProd, cleared) = tile.harvest
      maybeProd.map { prod =>
        val qty = _gs.farm.yieldPerHarvest                 // 👈 +yield per harvest
        val st2 = _gs.storage.add(prod, qty)
        val updatedGrid = _gs.grid.updated(idx, cleared)
        _gs = _gs.withGrid(updatedGrid).withStorage(st2)
        true
      }
    }.getOrElse(false)
  }


  def families: Seq[Family] = _gs.families
  def storage: Storage = _gs.storage
  def prices: Map[Produce, Int] = _market.prices

  /** Assigns qty of product to a family if available in storage. */
  def assignToFamily(fIdx: Int, p: Produce, qty: Int): Boolean = {
    if (qty <= 0 || fIdx < 0 || fIdx >= _gs.families.length) return false
    val (ok, st2) = _gs.storage.take(p, qty)
    if (!ok) return false

    val fam = _gs.families(fIdx)
    val added = {
      val n = p.nutritionPerUnit
      new Nutrition(n.cal * qty, n.protein * qty, n.carbs * qty, n.vitamins * qty)
    }

    val updatedFam =
      fam.withAssigned(fam.assigned.updated(p, fam.assigned.getOrElse(p, 0) + qty))
        .withStockpile(fam.stockpile + added)

    val updatedFamilies = _gs.families.updated(fIdx, updatedFam)
    _gs = new GameState(updatedFamilies, _gs.farm, _gs.grid, st2, _gs.day, _gs.money)
    true
  }

  /** Sells qty at current market price; updates storage AND market if needed. */
  def sell(p: Produce, qty: Int): Boolean = {
    if (qty <= 0) return false
    val (ok, st2) = _gs.storage.take(p, qty)
    if (!ok) return false

    // If `sell` returns (revenue: Int, nextMarket: MarketService), don’t drop the update.
    val (revenue, nextMarket) = _market.sell(p, qty)
    _market = nextMarket
    _gs = _gs.withStorage(st2).withMoney(_gs.money + revenue)
    true
  }

  // ---- Internal helpers ----------------------------------------------------

  private def idxIsValid(i: Int): Boolean =
    i >= 0 && i < _gs.grid.length

  private def familyIdxIsValid(i: Int): Boolean =
    i >= 0 && i < _gs.families.length
}
