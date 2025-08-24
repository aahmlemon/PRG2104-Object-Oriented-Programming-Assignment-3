package ahmad.model

final class Farm(
                  val irrigationLevel: Int = 0,
                  val yieldLevel: Int = 0
                ) {
  def growthBonusDays: Int = irrigationLevel
  
  def yieldPerHarvest: Int = 1 + yieldLevel

  def withIrrigationLevel(n: Int): Farm = new Farm(n, yieldLevel)
  def withYieldLevel(n: Int): Farm      = new Farm(irrigationLevel, n)
}

object Farm { def apply(): Farm = new Farm() }

