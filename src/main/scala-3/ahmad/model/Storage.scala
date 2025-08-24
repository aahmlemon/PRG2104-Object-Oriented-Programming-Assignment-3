package ahmad.model

final class Storage(val stock: Map[Produce, Int]) {

  def getStock: Map[Produce, Int] = stock

  def add(p: Produce, qty: Int): Storage =
    new Storage(stock.updated(p, stock.getOrElse(p, 0) + qty))

  def take(p: Produce, qty: Int): (Boolean, Storage) = {
    stock.get(p)
      .filter(_ >= qty)
      .map(_ => true -> new Storage(stock.updated(p, stock(p) - qty)))
      .getOrElse(false -> this)
  }
    
  def amountOf(p: Produce): Int = stock.getOrElse(p, 0)
}

object Storage {
  def empty: Storage = new Storage(Map.empty)
  def apply(stock: Map[Produce, Int] = Map.empty): Storage =
    new Storage(stock)
}
