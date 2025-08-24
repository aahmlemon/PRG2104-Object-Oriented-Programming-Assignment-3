package ahmad.service

import ahmad.model._

/**
 * Base trait for all possible farm upgrades.
 *
 * Each upgrade has:
 *   - a `name` (shown in the UI)
 *   - a `maxLevel` (cap on how far it can be upgraded)
 *
 * Concrete upgrades (see below) extend this trait.
 */
sealed trait Upgrade {
  def name: String
  def maxLevel: Int
}

/**
 * Upgrade: Irrigation system. 
 * Effect: reduces crop maturity time by 1 day per level. 
 * Max: 3 levels.
 */
case object Irrigation extends Upgrade {
  val name     = "Increased Irrigation (-1 Day to Mature)"
  val maxLevel = 3
}

/**
 * Upgrade: Yield boost.
 * Effect: increases harvest yield by +1 per crop per level.
 * Max: 3 levels.
 */
case object YieldBoost extends Upgrade {
  val name     = "Increased Yield (+1 per Harvest)"
  val maxLevel = 3
}

/**
 * Represents the next cost of an upgrade.
 *
 * @param level the level being purchased (not the current one, but the next)
 * @param price how much it costs in money
 */
final case class UpgradeCost(level: Int, price: Int)

/**
 * Service object containing logic for upgrades.
 *
 * Responsibilities:
 *   - Compute the current level of a given upgrade.
 *   - Determine the next cost (if not at max).
 *   - Apply an upgrade purchase, deducting money and updating the farm.
 */
object UpgradeService {

  /**
   * Price curve for upgrades.
   *   Index 0 → cost of level 1
   *   Index 1 → cost of level 2
   *   Index 2 → cost of level 3
   *
   * If a level is missing here, fallback cost (1000) is used.
   */
  private val curve: Vector[Int] = Vector(500, 1000, 2000)

  /**
   * Get the current level of an upgrade for a farm.
   *
   * @param farm farm state
   * @param up   which upgrade to query
   */
  def currentLevel(farm: Farm, up: Upgrade): Int = up match {
    case Irrigation => farm.irrigationLevel
    case YieldBoost => farm.yieldLevel
  }

  /**
   * Compute the cost of the next upgrade level, if not maxed.
   *
   * @param farm farm state
   * @param up   which upgrade to query
   * @return Some(UpgradeCost) if another level is available, else None
   */
  def nextCost(farm: Farm, up: Upgrade): Option[UpgradeCost] = {
    val lvl = currentLevel(farm, up)
    if (lvl >= up.maxLevel) None
    else {
      // curve.lift ensures safe access, fallback = 1000
      Some(UpgradeCost(lvl + 1, curve.lift(lvl).getOrElse(1000)))
    }
  }

  /**
   * Attempt to buy an upgrade for the game state.
   *
   * Checks:
   *   - Is the upgrade already maxed?
   *   - Does the player have enough money?
   *
   * If valid:
   *   - Deducts cost
   *   - Updates farm with new level
   *   - Returns Right(newGameState)
   *
   * If not:
   *   - Returns Left(errorMessage)
   *
   * @param gs  current game state
   * @param up  upgrade to purchase
   */
  def buy(gs: GameState, up: Upgrade): Either[String, GameState] = {
    val farm = gs.farm
    val maybeCost = nextCost(farm, up)
    if (maybeCost.isEmpty) return Left("Already at max level.")

    val cost = maybeCost.get.price
    if (gs.money < cost) return Left("Not enough money.")

    // Apply upgrade by incrementing the correct level
    val farm2 = up match {
      case Irrigation => farm.withIrrigationLevel(farm.irrigationLevel + 1)
      case YieldBoost => farm.withYieldLevel(farm.yieldLevel + 1)
    }

    // Deduct money and return updated game state
    Right(gs.withFarm(farm2).withMoney(gs.money - cost))
  }
}
