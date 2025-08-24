package ahmad.service

import ahmad.model._

sealed trait Upgrade { def name: String; def maxLevel: Int }
case object Irrigation extends Upgrade {
  val name = "Increased Irrigation (-1 Day to Mature)"
  val maxLevel = 3
}
case object YieldBoost extends Upgrade {
  val name = "Increased Yield (+1 per Harvest)"
  val maxLevel = 3
}

final case class UpgradeCost(level: Int, price: Int)

object UpgradeService {
  private val curve: Vector[Int] = Vector(500, 1000, 2000)

  def currentLevel(farm: Farm, up: Upgrade): Int = up match {
    case Irrigation => farm.irrigationLevel
    case YieldBoost => farm.yieldLevel
  }

  def nextCost(farm: Farm, up: Upgrade): Option[UpgradeCost] = {
    val lvl = currentLevel(farm, up)
    if (lvl >= up.maxLevel) None
    else Some(UpgradeCost(lvl + 1, curve.lift(lvl).getOrElse(1000)))
  }

  def buy(gs: GameState, up: Upgrade): Either[String, GameState] = {
    val farm = gs.farm
    val maybeCost = nextCost(farm, up)
    if (maybeCost.isEmpty) return Left("Already at max level.")

    val cost = maybeCost.get.price
    if (gs.money < cost) return Left("Not enough money.")

    val farm2 = up match {
      case Irrigation => farm.withIrrigationLevel(farm.irrigationLevel + 1)
      case YieldBoost => farm.withYieldLevel(farm.yieldLevel + 1)
    }
    Right(gs.withFarm(farm2).withMoney(gs.money - cost))
  }
}