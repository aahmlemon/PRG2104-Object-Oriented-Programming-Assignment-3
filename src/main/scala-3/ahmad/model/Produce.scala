package ahmad.model

sealed trait Produce {
  def name: String
  def nutritionPerUnit: Nutrition
  def daysToMature: Int
}

final case class Rice() extends Produce {
  val name = "Rice"
  val nutritionPerUnit: Nutrition = Nutrition(200, 5, 45, 2)
  val daysToMature = 3
}

final case class Beans() extends Produce {
  val name = "Beans"
  val nutritionPerUnit: Nutrition = Nutrition(150, 10, 20, 8)
  val daysToMature = 3
}

final case class Vegetables() extends Produce {
  val name = "Vegetables"
  val nutritionPerUnit: Nutrition = Nutrition(80, 3, 10, 20)
  val daysToMature = 2
}


