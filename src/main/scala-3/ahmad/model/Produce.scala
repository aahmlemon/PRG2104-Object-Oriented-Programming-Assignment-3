package ahmad.model

sealed trait Produce {
  def name: String
  def nutritionPerUnit: Nutrition
  def daysToMature: Int
}

final case class Rice() extends Produce {
  val name = "Rice"
  val nutritionPerUnit: Nutrition = Nutrition(2000, 50, 450, 20)
  val daysToMature = 3
}

final case class Beans() extends Produce {
  val name = "Beans"
  val nutritionPerUnit: Nutrition = Nutrition(1500, 100, 200, 80)
  val daysToMature = 3
}

final case class Vegetables() extends Produce {
  val name = "Vegetables"
  val nutritionPerUnit: Nutrition = Nutrition(800, 30, 100, 200)
  val daysToMature = 2
}


