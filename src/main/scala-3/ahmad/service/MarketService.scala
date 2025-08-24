package ahmad.service

import ahmad.model.Produce
import scala.util.Random

final class MarketService(
                           val prices: Map[Produce, Int],
                           val yesterday: Map[Produce, Int] = Map.empty,
                           val rnd: Random = new Random(42)
                         ) {

  /** Return the price of a given produce, defaulting to 10 if missing */
  private def quote(p: Produce): Int =
    prices.getOrElse(p, 10)

  /** Sell quantity of a produce and return (money earned, same market state) */
  def sell(p: Produce, qty: Int): (Int, MarketService) =
    (quote(p) * qty, this)

  /** Advance to the next day, evolving prices randomly */
  def nextDay(): MarketService = {
    val evolved = prices.map { case (k, v) =>
      val eps = rnd.between(-5, 6)
      val newP = math.max(1, v + (v * eps) / 100)
      k -> newP
    }
    copy(prices = evolved, yesterday = prices)
  }

  /** Compute day-to-day delta for a given produce */
  def delta(p: Produce): Option[Int] =
    for (t <- prices.get(p); y <- yesterday.get(p)) yield t - y

  /** Replacement for case class `.copy` */
  def copy(
            prices: Map[Produce, Int] = this.prices,
            yesterday: Map[Produce, Int] = this.yesterday,
            rnd: Random = this.rnd
          ): MarketService =
    new MarketService(prices, yesterday, rnd)

  /** Equality check (since we lost case class structural equality) */
  override def equals(obj: Any): Boolean = obj match {
    case other: MarketService =>
      this.prices == other.prices &&
        this.yesterday == other.yesterday
    case _ => false
  }

  /** Hash code consistent with equals */
  override def hashCode(): Int =
    (prices, yesterday).hashCode()

  /** Human-readable string for debugging */
  override def toString: String =
    s"MarketService(prices=$prices, yesterday=$yesterday)"
}

object MarketService {
  def fromPrices(pr: Map[Produce, Int], seed: Long = 42L): MarketService =
    new MarketService(pr, rnd = new Random(seed))

  def initial(ps: List[Produce], base: Int = 50, seed: Long = 42L): MarketService =
    new MarketService(ps.map(_ -> base).toMap, rnd = new Random(seed))
}

object Market {
  def initial(ps: List[Produce]): MarketService =
    MarketService.initial(ps)
}
