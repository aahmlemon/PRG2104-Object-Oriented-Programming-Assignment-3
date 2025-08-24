package ahmad.model

final class Family(
                    val name: String,
                    val dailyNeed: Nutrition,
                    val assigned: Map[Produce, Int] = Map.empty,
                    val stockpile: Nutrition = Nutrition.Zero
                  ) {
  def receivedToday: Nutrition =
    assigned.foldLeft(Nutrition.Zero) { case (acc, (p, qty)) =>
      val n = p.nutritionPerUnit
      acc + new Nutrition(n.cal * qty, n.protein * qty, n.carbs * qty, n.vitamins * qty)
    }
  
  def isSatisfied: Boolean =
    (stockpile + receivedToday).covers(dailyNeed)

  def reset: Family =
    new Family(name, dailyNeed, Map.empty, stockpile)
  
  def withAssigned(a: Map[Produce,Int]): Family =
    new Family(name, dailyNeed, a, stockpile)

  def withStockpile(n: Nutrition): Family =
    new Family(name, dailyNeed, assigned, n)
}
object Family {
  def apply(name: String, dailyNeed: Nutrition, assigned: Map[Produce,Int] = Map.empty, stockpile: Nutrition = Nutrition.Zero): Family =
    new Family(name, dailyNeed, assigned, stockpile)
}

