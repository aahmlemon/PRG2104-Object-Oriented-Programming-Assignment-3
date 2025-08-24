package ahmad.service

import ahmad.model.Produce
import scala.util.Random

final case class MarketService(
                                prices: Map[Produce, Int],
                                yesterday: Map[Produce, Int] = Map.empty,
                                rnd: Random = Random(42)
                              ) {
  def quote(p: Produce): Int =
    prices.getOrElse(p, 10)

  // returns (revenue, same-day market)
  def sell(p: Produce, qty: Int): (Int, MarketService) =
    (quote(p) * qty, this)

  // evolve prices ±5% each day (clamped to >= 1)
  def nextDay(): MarketService = {
    val evolved = prices.map { case (k, v) =>
      val eps = rnd.between(-5, 6)              // -5%..+5%
      val newP = math.max(1, v + (v * eps) / 100)
      k -> newP
    }
    copy(prices = evolved, yesterday = prices)   // <- key line
  }

  def delta(p: Produce): Option[Int] =
    for (t <- prices.get(p); y <- yesterday.get(p)) yield t - y
}

object MarketService {
  /** Build a MarketService directly from a price map (used by JSON load). */
  def fromPrices(pr: Map[Produce, Int], seed: Long = 42L): MarketService =
    MarketService(pr, rnd=new Random(seed))

  /** Build with a constant base price for a list of products. */
  def initial(ps: List[Produce], base: Int = 50, seed: Long = 42L): MarketService =
    MarketService(ps.map(_ -> base).toMap, rnd=new Random(seed))
}

/** (Optional) Keep your original helper for backward compatibility. */
object Market {
  def initial(ps: List[Produce]): MarketService =
    MarketService.initial(ps) // delegates to the companion above
}
