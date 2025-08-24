package ahmad.util

trait MoneyAware {
  def setOnMoneyChanged(cb: () => Unit): Unit
}
