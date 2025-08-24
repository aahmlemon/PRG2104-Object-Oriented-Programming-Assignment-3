package ahmad.model

final class Nutrition(
                       val cal: Int,
                       val protein: Int,
                       val carbs: Int,
                       val vitamins: Int
                     ) {

  def +(o: Nutrition): Nutrition =
    new Nutrition(
      cal + o.cal,
      protein + o.protein,
      carbs + o.carbs,
      vitamins + o.vitamins
    )

  def -(o: Nutrition): Nutrition =
    new Nutrition(math.max(0, cal - o.cal),
      math.max(0, protein - o.protein),
      math.max(0, carbs - o.carbs),
      math.max(0, vitamins - o.vitamins))

  def *(k: Int): Nutrition =
    new Nutrition(cal * k, protein * k, carbs * k, vitamins * k)

  def covers(need: Nutrition): Boolean =
    cal >= need.cal &&
      protein >= need.protein &&
      carbs >= need.carbs &&
      vitamins >= need.vitamins

  override def equals(other: Any): Boolean = other match {
    case that: Nutrition =>
      this.cal == that.cal &&
        this.protein == that.protein &&
        this.carbs == that.carbs &&
        this.vitamins == that.vitamins
    case _ => false
  }

  override def hashCode(): Int =
    (cal, protein, carbs, vitamins).##

  override def toString: String =
    s"Nutrition(cal=$cal, protein=$protein, carbs=$carbs, vitamins=$vitamins)"
}

object Nutrition {
  def apply(cal: Int, protein: Int, carbs: Int, vitamins: Int): Nutrition =
    new Nutrition(cal, protein, carbs, vitamins)

  def unapply(n: Nutrition): Option[(Int, Int, Int, Int)] =
    Some((n.cal, n.protein, n.carbs, n.vitamins))

  val Zero: Nutrition = Nutrition(0, 0, 0, 0)
}
