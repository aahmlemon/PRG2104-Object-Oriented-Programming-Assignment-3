package ahmad.model

final class CropTile(
                      val seed: Option[Produce] = None,
                      val daysGrown: Int = 0
                    ) {

  def plant(p: Produce): CropTile =
    new CropTile(Some(p), 0)

  def growOneDay(bonus: Int): CropTile =
    seed match {
      case Some(_) => new CropTile(seed, daysGrown + 1 + bonus)
      case None    => this
    }

  def isMature: Boolean =
    seed.exists(p => daysGrown >= p.daysToMature)

  def harvest: (Option[Produce], CropTile) =
    if isMature then (seed, new CropTile(None, 0))
    else (None, this)

  override def equals(other: Any): Boolean = other match {
    case that: CropTile =>
      this.seed == that.seed &&
        this.daysGrown == that.daysGrown
    case _ => false
  }

  override def hashCode(): Int =
    (seed, daysGrown).##

  override def toString: String =
    s"CropTile(seed=$seed, daysGrown=$daysGrown)"
}

object CropTile {
  def apply(seed: Option[Produce] = None, daysGrown: Int = 0): CropTile =
    new CropTile(seed, daysGrown)

  def unapply(t: CropTile): Option[(Option[Produce], Int)] =
    Some((t.seed, t.daysGrown))
}
